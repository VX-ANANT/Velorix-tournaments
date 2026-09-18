package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.db.AppDatabase
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.ConcurrentHashMap

/**
 * RepositoryManager.kt
 *
 * Thread-safe Singleton Manager that:
 * 1. Coordinates single shared instances of AppDatabase and PlatformRepository.
 * 2. Manages all Firebase Realtime Database ValueEventListener connections in a centralized registry.
 * 3. Guarantees safe cleanup, listener detachment, and prevents memory leaks, duplicate listeners, and ghost updates.
 */
class RepositoryManager private constructor(context: Context) {

    companion object {
        private const val TAG = "RepositoryManager"
        const val KEY_TOURNAMENTS = "listener_tournaments"
        const val KEY_MATCHES = "listener_matches"
        const val KEY_LEADERBOARD = "listener_leaderboard"
        const val KEY_USER_PROFILE = "listener_user_profile"
        const val KEY_TRANSACTIONS = "listener_transactions"
        const val KEY_BANNERS = "listener_banners"
        const val KEY_MISSIONS = "listener_missions"
        const val KEY_NOTIFICATIONS = "listener_notifications"
        const val KEY_USER_NOTIFICATIONS = "listener_user_notifications"
        const val KEY_DEPOSIT_REQUESTS = "listener_deposit_requests"
        const val KEY_WITHDRAW_REQUESTS = "listener_withdraw_requests"
        const val KEY_USER_REPORTS = "listener_user_reports"


        @Volatile
        private var INSTANCE: RepositoryManager? = null

        fun getInstance(context: Context): RepositoryManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RepositoryManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val applicationContext = context.applicationContext

    // Local SQLite Database Singleton
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(applicationContext)
    }

    // Platform Repository Singleton
    val repository: PlatformRepository by lazy {
        PlatformRepository(database, this)
    }

    /**
     * Internal Data Holder for an active listener registration.
     */
    private data class RegisteredListener(
        val key: String,
        val query: Query,
        val listener: ValueEventListener,
        val registeredAt: Long = System.currentTimeMillis()
    )

    // Thread-safe map tracking all active RTDB listeners
    private val activeListeners = ConcurrentHashMap<String, RegisteredListener>()

    /**
     * Registers and starts a ValueEventListener on the provided Firebase Query/Reference.
     * If a listener with the given [key] is already active, it is safely removed first
     * to eliminate duplicate subscriptions and ghost updates.
     */
    @Synchronized
    fun registerValueEventListener(key: String, query: Query, listener: ValueEventListener) {
        try {
            // If already registered, remove old listener first
            removeValueEventListener(key)

            // Attach new listener to query
            query.addValueEventListener(listener)

            // Save registration record
            val registration = RegisteredListener(
                key = key,
                query = query,
                listener = listener
            )
            activeListeners[key] = registration
            Log.d(TAG, "Registered RTDB listener: [$key] (Total active: ${activeListeners.size})")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering RTDB listener for key: $key", e)
        }
    }

    /**
     * Safely unregisters and removes the ValueEventListener identified by [key].
     */
    @Synchronized
    fun removeValueEventListener(key: String) {
        try {
            activeListeners.remove(key)?.let { reg ->
                reg.query.removeEventListener(reg.listener)
                Log.d(TAG, "Unregistered RTDB listener: [$key] (Remaining active: ${activeListeners.size})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing RTDB listener for key: $key", e)
        }
    }

    /**
     * Checks if a listener with the given [key] is currently active.
     */
    fun isListening(key: String): Boolean {
        return activeListeners.containsKey(key)
    }

    /**
     * Returns count of currently active listeners.
     */
    fun getActiveListenerCount(): Int {
        return activeListeners.size
    }

    /**
     * Detaches all active ValueEventListeners from Firebase RTDB and clears the registry.
     * Prevents memory leaks and ghost updates during lifecycle destruction or user sign out.
     */
    @Synchronized
    fun removeAllListeners() {
        try {
            Log.d(TAG, "Cleaning up all RTDB listeners (Count: ${activeListeners.size})...")
            for ((key, reg) in activeListeners) {
                try {
                    reg.query.removeEventListener(reg.listener)
                    Log.d(TAG, "Removed RTDB listener: [$key]")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed removing listener for [$key]", e)
                }
            }
            activeListeners.clear()
            Log.d(TAG, "All RTDB listeners successfully detached.")
        } catch (e: Exception) {
            Log.e(TAG, "Error during removeAllListeners", e)
        }
    }

    /**
     * Handles user sign-out lifecycle:
     * - Removes user-specific listeners (profile, transactions)
     * - Cleans up user database
     */
    suspend fun onUserLogout() {
        removeValueEventListener(KEY_USER_PROFILE)
        removeValueEventListener(KEY_TRANSACTIONS)
        repository.clearUserDatabase()
    }

    /**
     * Full lifecycle cleanup when repository manager is destroyed or reset.
     */
    fun cleanup() {
        removeAllListeners()
    }
}
