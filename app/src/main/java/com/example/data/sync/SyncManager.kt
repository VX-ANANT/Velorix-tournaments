package com.example.data.sync

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.example.data.repository.RepositoryManager
import com.example.service.NotificationHelper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Data class representing the centralized state synchronized across Admin and User panels.
 * Root node in Firebase Realtime Database: 'global_app_state'
 */
data class GlobalAppState(
    val eventId: String = "",
    val eventType: String = "INIT",
    val source: String = "SYSTEM",
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val tournamentId: String? = null,
    val affectedUserId: String? = null,
    val version: Long = 1L,
    val payload: Map<String, Any?> = emptyMap()
)

/**
 * Observer interface for components that need to respond to cross-panel updates.
 */
interface SyncObserver {
    fun onGlobalStateChanged(state: GlobalAppState) {}
    fun onTournamentUpdated(tournamentId: String, eventType: String, payload: Map<String, Any?>) {}
    fun onScheduleChanged(tournamentId: String, newSchedule: String) {}
    fun onRoomCredentialsReleased(tournamentId: String, roomId: String, roomPass: String) {}
    fun onUserStatusChanged(userId: String, newStatus: String, reason: String?) {}
}

/**
 * SyncManager
 *
 * Centralized Single Source of Truth coordinator between Admin and User panels.
 * 1. Synchronizes Realtime Database updates at 'global_app_state'.
 * 2. Implements the Observer pattern with thread-safe subscriber registry.
 * 3. Provides anti-echo deduplication to prevent recursive self-updates.
 * 4. Dispatches immediate UI and notification responses when Admin changes schedules, credentials, or statuses.
 */
class SyncManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SyncManager"
        private const val RTDB_URL = "https://velorix-tournaments-default-rtdb.asia-southeast1.firebasedatabase.app"
        private const val NODE_GLOBAL_APP_STATE = "global_app_state"
        private const val NODE_HISTORY = "global_app_state_history"

        const val SOURCE_USER_PANEL = "USER_PANEL"
        const val SOURCE_ADMIN_PANEL = "ADMIN_PANEL"

        // Periodic Polling Interval: 5 minutes
        const val PERIODIC_POLL_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes periodic polling
        private const val DEBOUNCE_SYNC_INTERVAL_MS = 3_000L  // 3s debounce between rapid foreground switches

        // Standard Event Types
        const val EVENT_TOURNAMENT_UPDATED = "TOURNAMENT_UPDATED"
        const val EVENT_SCHEDULE_CHANGED = "SCHEDULE_CHANGED"
        const val EVENT_ROOM_CREDENTIALS_RELEASED = "ROOM_CREDENTIALS_RELEASED"
        const val EVENT_USER_STATUS_CHANGED = "USER_STATUS_CHANGED"
        const val EVENT_TOURNAMENT_JOINED = "TOURNAMENT_JOINED"
        const val EVENT_TOURNAMENT_CANCELLED = "TOURNAMENT_CANCELLED"
        const val EVENT_HEARTBEAT = "HEARTBEAT"

        @Volatile
        private var INSTANCE: SyncManager? = null

        fun getInstance(context: Context): SyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val rtdb: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance(RTDB_URL)
    }

    private val globalStateRef: DatabaseReference by lazy {
        rtdb.getReference(NODE_GLOBAL_APP_STATE)
    }

    private val observers = CopyOnWriteArraySet<SyncObserver>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val schedulerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var periodicPollingJob: Job? = null
    private val isForeground = AtomicBoolean(false)
    private val activeActivityCount = AtomicInteger(0)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _globalAppState = MutableStateFlow(GlobalAppState())
    val globalAppState: StateFlow<GlobalAppState> = _globalAppState.asStateFlow()

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // Local set of recently dispatched event IDs to prevent echo loops
    private val recentLocalEventIds = CopyOnWriteArraySet<String>()
    private var isListening = false

    init {
        monitorConnection()
        startGlobalStateListener()
        setupLifecycleScheduler()
    }

    /**
     * Installs application lifecycle callbacks to automatically detect
     * when the app enters the foreground or resumes from the background,
     * triggering instant data synchronization and driving the 5-minute periodic polling scheduler.
     */
    private fun setupLifecycleScheduler() {
        val app = context.applicationContext as? Application
        if (app != null) {
            app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

                override fun onActivityStarted(activity: Activity) {
                    val count = activeActivityCount.incrementAndGet()
                    if (count == 1) {
                        Log.d(TAG, "SyncManager: App entered foreground (${activity.javaClass.simpleName}) -> triggering sync")
                        onAppForeground(source = "app_start")
                    }
                }

                override fun onActivityResumed(activity: Activity) {
                    if (!isForeground.get()) {
                        Log.d(TAG, "SyncManager: App resumed from background (${activity.javaClass.simpleName}) -> triggering sync")
                        onAppForeground(source = "activity_resume")
                    }
                }

                override fun onActivityPaused(activity: Activity) {}

                override fun onActivityStopped(activity: Activity) {
                    val count = activeActivityCount.decrementAndGet()
                    if (count <= 0) {
                        activeActivityCount.set(0)
                        Log.d(TAG, "SyncManager: App entered background")
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
            Log.i(TAG, "SyncManager lifecycle scheduler attached to Application successfully.")
        }
        // Launch the initial 5-minute periodic polling loop
        startPeriodicPolling()
    }

    /**
     * Called when the application enters the foreground or resumes from the background.
     * Triggers data synchronization immediately and restarts the 5-minute periodic polling loop.
     */
    fun onAppForeground(source: String = "foreground") {
        isForeground.set(true)
        val now = System.currentTimeMillis()
        if (now - _lastSyncTimestamp.value >= DEBOUNCE_SYNC_INTERVAL_MS) {
            triggerDataSynchronization(force = true, source = source)
        }
        startPeriodicPolling()
    }

    /**
     * Called when the application enters the background.
     */
    fun onAppBackground(source: String = "background") {
        isForeground.set(false)
        Log.d(TAG, "SyncManager: App transitioned to background [$source]")
    }

    /**
     * Starts the Coroutine-based 5-minute periodic polling scheduler.
     * Periodically queries the backend every 5 minutes while active.
     */
    fun startPeriodicPolling() {
        periodicPollingJob?.cancel()
        periodicPollingJob = schedulerScope.launch {
            while (isActive) {
                delay(PERIODIC_POLL_INTERVAL_MS)
                Log.d(TAG, "SyncManager: 5-minute periodic polling timer triggered")
                triggerDataSynchronization(force = false, source = "periodic_5min_poll")
            }
        }
        Log.d(TAG, "SyncManager: 5-minute periodic polling scheduler started.")
    }

    /**
     * Stops the periodic polling scheduler.
     */
    fun stopPeriodicPolling() {
        periodicPollingJob?.cancel()
        periodicPollingJob = null
        Log.d(TAG, "SyncManager: Periodic polling scheduler stopped.")
    }

    /**
     * Triggers complete data synchronization:
     * 1. Refreshes Realtime Database 'global_app_state' node (Single Source of Truth)
     * 2. Refreshes Room SQLite DB & remote repositories (tournaments, room credentials, wallet balance, user status)
     */
    fun triggerDataSynchronization(force: Boolean = true, source: String = "manual") {
        schedulerScope.launch {
            if (_isSyncing.value) {
                Log.d(TAG, "SyncManager: Data synchronization already in progress, skipping [$source]")
                return@launch
            }
            _isSyncing.value = true
            try {
                Log.i(TAG, "SyncManager: Starting data synchronization [source=$source, force=$force]")

                // 1. Fetch latest 'global_app_state' from Firebase RTDB and dispatch to registered observers
                refreshGlobalState()

                // 2. Fetch remote updates for user, tournaments, transactions, and banners into Room database
                try {
                    val repository = RepositoryManager.getInstance(context).repository
                    repository.refreshBackendSync(force = force)
                } catch (e: Throwable) {
                    Log.w(TAG, "SyncManager: Repository sync notice [$source]: ${e.message}")
                }

                _lastSyncTimestamp.value = System.currentTimeMillis()
                Log.i(TAG, "SyncManager: Data synchronization completed successfully [source=$source]")
            } catch (e: Throwable) {
                Log.e(TAG, "SyncManager: Data synchronization error [$source]: ${e.message}", e)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Registers a new observer to receive cross-panel sync events.
     */
    fun registerObserver(observer: SyncObserver) {
        observers.add(observer)
        // Immediately notify with current state if initialized
        if (_globalAppState.value.eventId.isNotBlank()) {
            try {
                observer.onGlobalStateChanged(_globalAppState.value)
            } catch (e: Throwable) {
                Log.w(TAG, "Observer initial callback error: ${e.message}")
            }
        }
    }

    /**
     * Unregisters an observer.
     */
    fun unregisterObserver(observer: SyncObserver) {
        observers.remove(observer)
    }

    /**
     * Monitors Firebase connection state.
     */
    private fun monitorConnection() {
        try {
            val connectedRef = rtdb.getReference(".info/connected")
            connectedRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    _isConnected.value = connected
                    Log.d(TAG, "SyncManager RTDB Connected: $connected")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Connection listener cancelled: ${error.message}")
                }
            })
        } catch (e: Throwable) {
            Log.w(TAG, "Error attaching connection monitor: ${e.message}")
        }
    }

    /**
     * Listens to 'global_app_state' node in Firebase RTDB.
     */
    private fun startGlobalStateListener() {
        if (isListening) return
        isListening = true

        globalStateRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                scope.launch {
                    try {
                        val eventId = snapshot.child("eventId").getValue(String::class.java).orEmpty()
                        val eventType = snapshot.child("eventType").getValue(String::class.java) ?: "UNKNOWN"
                        val source = snapshot.child("source").getValue(String::class.java) ?: "SYSTEM"
                        val lastUpdatedAt = snapshot.child("lastUpdatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                        val tournamentId = snapshot.child("tournamentId").getValue(String::class.java)
                        val affectedUserId = snapshot.child("affectedUserId").getValue(String::class.java)
                        val version = snapshot.child("version").getValue(Long::class.java) ?: 1L

                        val rawPayload = snapshot.child("payload").value
                        @Suppress("UNCHECKED_CAST")
                        val payload: Map<String, Any?> = when (rawPayload) {
                            is Map<*, *> -> rawPayload as Map<String, Any?>
                            else -> emptyMap()
                        }

                        val newState = GlobalAppState(
                            eventId = eventId,
                            eventType = eventType,
                            source = source,
                            lastUpdatedAt = lastUpdatedAt,
                            tournamentId = tournamentId,
                            affectedUserId = affectedUserId,
                            version = version,
                            payload = payload
                        )

                        _globalAppState.value = newState

                        // Prevent processing our own locally originated events twice
                        if (eventId.isNotBlank() && recentLocalEventIds.contains(eventId)) {
                            Log.d(TAG, "Ignoring echoed self-event: $eventId ($eventType)")
                            return@launch
                        }

                        Log.i(TAG, "⚡ Received cross-panel sync event: [$eventType] from [$source] (v$version)")
                        dispatchSyncEvent(newState)

                    } catch (e: Throwable) {
                        Log.e(TAG, "Error parsing global_app_state update", e)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "global_app_state listener cancelled: ${error.message}")
            }
        })
    }

    /**
     * Actively fetches the latest 'global_app_state' node from Realtime Database
     * and notifies observers.
     */
    fun refreshGlobalState() {
        globalStateRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                try {
                    val eventId = snapshot.child("eventId").getValue(String::class.java).orEmpty()
                    if (eventId.isNotBlank() && !recentLocalEventIds.contains(eventId)) {
                        val eventType = snapshot.child("eventType").getValue(String::class.java).orEmpty()
                        val source = snapshot.child("source").getValue(String::class.java) ?: "UNKNOWN"
                        val tournamentId = snapshot.child("tournamentId").getValue(String::class.java)
                        val affectedUserId = snapshot.child("affectedUserId").getValue(String::class.java)
                        val version = snapshot.child("version").getValue(Long::class.java) ?: 0L
                        val lastUpdatedAt = snapshot.child("lastUpdatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                        val payload = mutableMapOf<String, Any?>()
                        val payloadSnap = snapshot.child("payload")
                        if (payloadSnap.exists()) {
                            for (child in payloadSnap.children) {
                                val key = child.key ?: continue
                                payload[key] = child.value
                            }
                        }

                        val parsedState = GlobalAppState(
                            eventId = eventId,
                            eventType = eventType,
                            source = source,
                            tournamentId = tournamentId,
                            affectedUserId = affectedUserId,
                            version = version,
                            lastUpdatedAt = lastUpdatedAt,
                            payload = payload
                        )
                        _globalAppState.value = parsedState
                        dispatchSyncEvent(parsedState)
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Error in manual refreshGlobalState: ${e.message}")
                }
            }
        }.addOnFailureListener { e ->
            Log.w(TAG, "Failed to refresh global_app_state: ${e.message}")
        }
    }

    /**
     * Dispatches parsed cross-panel event to registered observers and system helpers.
     */
    private fun dispatchSyncEvent(state: GlobalAppState) {
        // 1. Notify all registered observers
        for (observer in observers) {
            try {
                observer.onGlobalStateChanged(state)

                when (state.eventType) {
                    EVENT_TOURNAMENT_UPDATED -> {
                        state.tournamentId?.let { tid ->
                            observer.onTournamentUpdated(tid, state.eventType, state.payload)
                        }
                    }
                    EVENT_SCHEDULE_CHANGED -> {
                        val newSchedule = state.payload["newSchedule"]?.toString().orEmpty()
                        state.tournamentId?.let { tid ->
                            observer.onScheduleChanged(tid, newSchedule)
                        }
                    }
                    EVENT_ROOM_CREDENTIALS_RELEASED -> {
                        val roomId = state.payload["roomId"]?.toString().orEmpty()
                        val roomPass = state.payload["roomPass"]?.toString().orEmpty()
                        state.tournamentId?.let { tid ->
                            observer.onRoomCredentialsReleased(tid, roomId, roomPass)
                        }
                    }
                    EVENT_USER_STATUS_CHANGED -> {
                        val newStatus = state.payload["newStatus"]?.toString().orEmpty()
                        val reason = state.payload["reason"]?.toString()
                        state.affectedUserId?.let { uid ->
                            observer.onUserStatusChanged(uid, newStatus, reason)
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Observer dispatch error", e)
            }
        }

        // 2. Perform automated action if coming from Admin Panel
        if (state.source == SOURCE_ADMIN_PANEL) {
            when (state.eventType) {
                EVENT_ROOM_CREDENTIALS_RELEASED -> {
                    val tournamentTitle = state.payload["tournamentTitle"]?.toString() ?: "Tournament Lobby"
                    val roomId = state.payload["roomId"]?.toString().orEmpty()
                    val roomPass = state.payload["roomPass"]?.toString().orEmpty()
                    if (roomId.isNotBlank()) {
                        NotificationHelper.showRoomCredentialsNotification(
                            context = context,
                            tournamentTitle = tournamentTitle,
                            roomId = roomId,
                            roomPass = roomPass,
                            tournamentId = state.tournamentId.orEmpty()
                        )
                    }
                }
                EVENT_SCHEDULE_CHANGED -> {
                    val tournamentTitle = state.payload["tournamentTitle"]?.toString() ?: "Tournament"
                    val newSchedule = state.payload["newSchedule"]?.toString().orEmpty()
                    if (newSchedule.isNotBlank()) {
                        NotificationHelper.showTournamentUpdatedNotification(
                            context = context,
                            tournamentTitle = tournamentTitle,
                            updateDetails = "Schedule updated to: $newSchedule",
                            tournamentId = state.tournamentId.orEmpty()
                        )
                    }
                }
                EVENT_TOURNAMENT_CANCELLED -> {
                    val tournamentTitle = state.payload["tournamentTitle"]?.toString() ?: "Tournament"
                    val refundFee = (state.payload["entryFee"] as? Number)?.toDouble() ?: 0.0
                    val reason = state.payload["reason"]?.toString() ?: "Cancelled by Admin"
                    NotificationHelper.showTournamentCancelledNotification(
                        context = context,
                        tournamentTitle = tournamentTitle,
                        refundAmount = refundFee,
                        reason = reason,
                        tournamentId = state.tournamentId.orEmpty()
                    )
                }
            }
        }
    }

    /**
     * Publishes a tournament lifecycle update to 'global_app_state'.
     */
    fun pushTournamentUpdate(
        tournamentId: String,
        eventType: String,
        tournamentTitle: String? = null,
        metadata: Map<String, Any?> = emptyMap()
    ) {
        val payload = metadata.toMutableMap()
        if (tournamentTitle != null) {
            payload["tournamentTitle"] = tournamentTitle
        }
        pushStateUpdate(
            eventType = eventType,
            tournamentId = tournamentId,
            payload = payload
        )
    }

    /**
     * Publishes schedule modification to 'global_app_state'.
     */
    fun pushScheduleChange(
        tournamentId: String,
        newScheduleTime: String,
        tournamentTitle: String? = null
    ) {
        pushStateUpdate(
            eventType = EVENT_SCHEDULE_CHANGED,
            tournamentId = tournamentId,
            payload = mapOf(
                "newSchedule" to newScheduleTime,
                "tournamentTitle" to (tournamentTitle ?: "Tournament")
            )
        )
    }

    /**
     * Publishes room credentials release to 'global_app_state'.
     */
    fun pushRoomCredentials(
        tournamentId: String,
        roomId: String,
        roomPass: String,
        tournamentTitle: String? = null
    ) {
        pushStateUpdate(
            eventType = EVENT_ROOM_CREDENTIALS_RELEASED,
            tournamentId = tournamentId,
            payload = mapOf(
                "roomId" to roomId,
                "roomPass" to roomPass,
                "tournamentTitle" to (tournamentTitle ?: "Tournament Lobby")
            )
        )
    }

    /**
     * Publishes user status change (e.g. BAN, SUSPEND, ACTIVE) to 'global_app_state'.
     */
    fun pushUserStatusChange(
        userId: String,
        newStatus: String,
        reason: String? = null
    ) {
        pushStateUpdate(
            eventType = EVENT_USER_STATUS_CHANGED,
            affectedUserId = userId,
            payload = mapOf(
                "newStatus" to newStatus,
                "reason" to (reason ?: "Status updated")
            )
        )
    }

    /**
     * Internal unified publisher writing to Firebase RTDB 'global_app_state'.
     */
    private fun pushStateUpdate(
        eventType: String,
        tournamentId: String? = null,
        affectedUserId: String? = null,
        payload: Map<String, Any?> = emptyMap()
    ) {
        scope.launch {
            try {
                val eventId = "evt_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
                val currentVersion = _globalAppState.value.version + 1L

                // Cache to prevent self-echo handling
                recentLocalEventIds.add(eventId)
                if (recentLocalEventIds.size > 50) {
                    recentLocalEventIds.clear()
                    recentLocalEventIds.add(eventId)
                }

                val stateMap = hashMapOf<String, Any?>(
                    "eventId" to eventId,
                    "eventType" to eventType,
                    "source" to SOURCE_USER_PANEL,
                    "lastUpdatedAt" to ServerValue.TIMESTAMP,
                    "tournamentId" to tournamentId,
                    "affectedUserId" to affectedUserId,
                    "version" to currentVersion,
                    "payload" to payload
                )

                // Write atomic update to Firebase Realtime Database
                globalStateRef.setValue(stateMap).addOnSuccessListener {
                    Log.d(TAG, "Successfully published cross-panel event: $eventType (v$currentVersion)")
                }.addOnFailureListener { error ->
                    Log.w(TAG, "Failed to publish cross-panel event: ${error.message}")
                }

                // Also append to audit history node for admin debugging
                rtdb.getReference(NODE_HISTORY).child(eventId).setValue(stateMap)

            } catch (e: Throwable) {
                Log.e(TAG, "Failed to push state update", e)
            }
        }
    }
}
