package com.example

import android.app.Application
import com.entrig.sdk.Entrig
import com.entrig.sdk.models.EntrigConfig
import com.google.firebase.FirebaseApp

class MyApplication : Application() {
    companion object {
        lateinit var instance: MyApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        val isEmulator = EnvUtils.isEmu()
        
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                android.util.Log.d("Firebase", "Firebase initialized.")
            } else {
                android.util.Log.d("Firebase", "Firebase already initialized.")
            }

            // Initialize Notification Channels
            com.example.service.NotificationHelper.createNotificationChannels(this)

            // Initialize Periodic Engagement Scheduler (every 1.5 - 2 hours)
            com.example.service.EngagementNotificationScheduler.schedulePeriodicEngagement(this)

            // Initialize Firebase Crashlytics & Sentinel Defense
            com.example.util.CrashReporter.init(this)

            // Initialize Cross-Panel Realtime SyncManager (Single Source of Truth)
            com.example.data.sync.SyncManager.getInstance(this)

            // Initialize Lifecycle-Aware AutoRefreshManager (App Open, Close & Periodic Foreground Sync)
            com.example.data.sync.AutoRefreshManager.getInstance(this).install(this)

            try {
                val isEmu = EnvUtils.isEmu()
                val playServicesAvailable = try {
                    com.google.android.gms.common.GoogleApiAvailability.getInstance()
                        .isGooglePlayServicesAvailable(this) == com.google.android.gms.common.ConnectionResult.SUCCESS
                } catch (_: Throwable) {
                    false
                }

                if (!isEmu && playServicesAvailable) {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().isAutoInitEnabled = true
                } else {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().isAutoInitEnabled = false
                    android.util.Log.d("FCM", "FCM auto-init disabled (Emulator or Play Services unavailable)")
                }
            } catch (_: Throwable) {}
        } catch (e: Exception) {
            android.util.Log.e("Firebase", "Failed to init FIREBASE APP", e)
        }

        try {
            val apiKey = BuildConfig.ENTRIG_API_KEY
            if (apiKey != "DEFAULT_ENTRIG_API_KEY" && apiKey.isNotBlank()) {
                if (!isEmulator) {
                    Entrig.initialize(this, EntrigConfig(apiKey = apiKey))
                    android.util.Log.d("Entrig", "Entrig SDK initialized with key.")
                } else {
                    android.util.Log.d("Entrig", "Skipping Entrig init on emulator.")
                }
            } else {
                android.util.Log.e("Entrig", "Entrig SDK key is missing or default. Add ENTRIG_API_KEY to AI Studio Secrets!")
            }
        } catch (e: Throwable) {
            android.util.Log.e("Entrig", "Entrig init failed", e)
        }
    }
}
