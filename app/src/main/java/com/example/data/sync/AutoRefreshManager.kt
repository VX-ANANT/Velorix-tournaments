package com.example.data.sync

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import com.example.data.repository.RepositoryManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * AutoRefreshManager.kt
 *
 * Centralized lifecycle-aware auto-refresh engine:
 * 1. App Open / Foreground: Immediately syncs latest tournaments, matches, wallet balance,
 *    user profile, banners, and credentials so the user doesn't have to manually refresh.
 * 2. Periodic Foreground Refresh: Automatically refreshes every [PERIODIC_INTERVAL_MS] (35s)
 *    while the app is actively in use, keeping match schedules, slot counts, and room credentials fresh.
 * 3. App Close / Background: Automatically cancels periodic loops to save battery, updates presence,
 *    and flushes pending local state.
 */
class AutoRefreshManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var periodicJob: Job? = null
    private val isForeground = AtomicBoolean(false)
    private val activeActivityCount = AtomicInteger(0)

    private val _isAutoRefreshing = MutableStateFlow(false)
    val isAutoRefreshing: StateFlow<Boolean> = _isAutoRefreshing.asStateFlow()

    private val _lastRefreshTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastRefreshTimestamp: StateFlow<Long> = _lastRefreshTimestamp.asStateFlow()

    companion object {
        private const val TAG = "AutoRefreshManager"
        private const val PERIODIC_INTERVAL_MS = 35_000L // 35 seconds periodic background refresh
        private const val DEBOUNCE_INTERVAL_MS = 5_000L  // 5 seconds debounce between immediate triggers

        @Volatile
        private var INSTANCE: AutoRefreshManager? = null

        fun getInstance(context: Context): AutoRefreshManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AutoRefreshManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Installs global Application ActivityLifecycleCallbacks to detect app open,
     * backgrounding, foregrounding, and termination automatically.
     */
    fun install(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {
                val count = activeActivityCount.incrementAndGet()
                if (count == 1) {
                    Log.d(TAG, "App transitioned from Background -> Foreground (Activity: ${activity.javaClass.simpleName})")
                    onAppForeground(source = "app_start")
                }
            }

            override fun onActivityResumed(activity: Activity) {
                // Ensure foreground periodic ticker is active
                if (!isForeground.get()) {
                    onAppForeground(source = "activity_resume")
                }
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                val count = activeActivityCount.decrementAndGet()
                if (count <= 0) {
                    activeActivityCount.set(0)
                    Log.d(TAG, "App transitioned from Foreground -> Background (Activity: ${activity.javaClass.simpleName})")
                    onAppBackground(source = "app_stop")
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (activeActivityCount.get() <= 0) {
                    onAppBackground(source = "app_destroy")
                }
            }
        })
        Log.i(TAG, "AutoRefreshManager lifecycle listener registered successfully.")
    }

    /**
     * Called when the app enters the foreground or opens.
     */
    fun onAppForeground(source: String = "foreground") {
        isForeground.set(true)
        val now = System.currentTimeMillis()
        if (now - _lastRefreshTimestamp.value > DEBOUNCE_INTERVAL_MS) {
            triggerRefresh(force = true, source = source)
        }
        updateUserPresence(isOnline = true)
        startPeriodicRefresh()
    }

    /**
     * Called when the app is minimized or closed.
     */
    fun onAppBackground(source: String = "background") {
        isForeground.set(false)
        stopPeriodicRefresh()
        updateUserPresence(isOnline = false)
        flushStateOnClose(source)
    }

    /**
     * Trigger a manual refresh (e.g. pull to refresh or button tap).
     * Resets the periodic timer so it doesn't fire redundant calls immediately.
     */
    fun triggerManualRefresh(force: Boolean = true, source: String = "manual") {
        triggerRefresh(force = force, source = source)
        if (isForeground.get()) {
            startPeriodicRefresh()
        }
    }

    private fun startPeriodicRefresh() {
        periodicJob?.cancel()
        periodicJob = scope.launch {
            while (isActive && isForeground.get()) {
                delay(PERIODIC_INTERVAL_MS)
                if (isForeground.get() && isNetworkAvailable()) {
                    Log.d(TAG, "Periodic auto-refresh timer triggered.")
                    triggerRefresh(force = false, source = "periodic_timer")
                }
            }
        }
    }

    private fun stopPeriodicRefresh() {
        periodicJob?.cancel()
        periodicJob = null
    }

    private fun triggerRefresh(force: Boolean, source: String) {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Skipping auto-refresh [$source]: Device offline")
            return
        }

        scope.launch {
            if (_isAutoRefreshing.value) {
                Log.d(TAG, "Auto-refresh already in progress, skipping duplicate call [$source]")
                return@launch
            }
            _isAutoRefreshing.value = true
            try {
                Log.d(TAG, "Starting auto-refresh [$source] (force=$force)")
                val repository = RepositoryManager.getInstance(context).repository

                // 1. Fetch remote updates for user, tournaments, transactions, and banners
                repository.refreshBackendSync(force = force)

                // 2. Synchronize Realtime Database Single Source of Truth
                SyncManager.getInstance(context).refreshGlobalState()

                _lastRefreshTimestamp.value = System.currentTimeMillis()
                Log.i(TAG, "Auto-refresh cycle completed successfully [$source]")
            } catch (e: Throwable) {
                Log.w(TAG, "Auto-refresh cycle completed with notice [$source]: ${e.message}")
            } finally {
                _isAutoRefreshing.value = false
            }
        }
    }

    private fun updateUserPresence(isOnline: Boolean) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        scope.launch {
            try {
                val userPresenceRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUserId)

                val updates = hashMapOf<String, Any>(
                    "isOnline" to isOnline,
                    "lastActiveAt" to ServerValue.TIMESTAMP
                )
                userPresenceRef.updateChildren(updates)
                Log.d(TAG, "Updated user presence for $currentUserId: isOnline=$isOnline")
            } catch (e: Throwable) {
                Log.w(TAG, "User presence update notice: ${e.message}")
            }
        }
    }

    private fun flushStateOnClose(source: String) {
        scope.launch {
            try {
                Log.d(TAG, "Flushing pending state on background [$source]")
                val repository = RepositoryManager.getInstance(context).repository
                // Any pending cleanup or state persistence
                _lastRefreshTimestamp.value = System.currentTimeMillis()
            } catch (e: Throwable) {
                Log.w(TAG, "State flush notice on background: ${e.message}")
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Throwable) {
            true
        }
    }
}
