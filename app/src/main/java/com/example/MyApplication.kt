package com.example

import android.app.Application
import com.entrig.sdk.Entrig
import com.entrig.sdk.models.EntrigConfig
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch

class MyApplication : Application(), coil.ImageLoaderFactory {
    companion object {
        lateinit var instance: MyApplication
            private set
    }

    override fun newImageLoader(): coil.ImageLoader {
        return coil.ImageLoader.Builder(this)
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .respectCacheHeaders(false)
            .allowHardware(true)
            .crossfade(true)
            .build()
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

            // Initialize Cross-Panel Realtime SyncManager (Single Source of Truth)
            com.example.data.sync.SyncManager.getInstance(this)

            // Initialize Lifecycle-Aware AutoRefreshManager (App Open, Close & Periodic Foreground Sync)
            com.example.data.sync.AutoRefreshManager.getInstance(this).install(this)

            // Offload non-critical initializations to background thread to ensure instantaneous cold start
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    // Periodic Engagement Scheduler
                    com.example.service.EngagementNotificationScheduler.schedulePeriodicEngagement(this@MyApplication)

                    // Firebase Crashlytics & Sentinel Defense
                    com.example.util.CrashReporter.init(this@MyApplication)

                    val isEmu = EnvUtils.isEmu()
                    val playServicesAvailable = try {
                        com.google.android.gms.common.GoogleApiAvailability.getInstance()
                            .isGooglePlayServicesAvailable(this@MyApplication) == com.google.android.gms.common.ConnectionResult.SUCCESS
                    } catch (_: Throwable) {
                        false
                    }

                    if (!isEmu && playServicesAvailable) {
                        com.google.firebase.messaging.FirebaseMessaging.getInstance().isAutoInitEnabled = true
                    } else {
                        com.google.firebase.messaging.FirebaseMessaging.getInstance().isAutoInitEnabled = false
                    }
                } catch (e: Throwable) {
                    android.util.Log.w("MyApplication", "Background init warning: ${e.message}")
                }

                try {
                    val apiKey = BuildConfig.ENTRIG_API_KEY
                    if (apiKey != "DEFAULT_ENTRIG_API_KEY" && apiKey.isNotBlank()) {
                        if (!isEmulator) {
                            Entrig.initialize(this@MyApplication, EntrigConfig(apiKey = apiKey))
                            android.util.Log.d("Entrig", "Entrig SDK initialized with key.")
                        }
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("Entrig", "Entrig init failed", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Firebase", "Failed to init FIREBASE APP", e)
        }
    }
}
