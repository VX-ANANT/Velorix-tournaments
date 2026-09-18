cat << 'INNER_EOF' > app/src/main/java/com/example/MyApplication.kt
package com.example

import android.app.Application
import com.entrig.sdk.Entrig
import com.entrig.sdk.models.EntrigConfig
import com.google.firebase.FirebaseApp

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                android.util.Log.d("Firebase", "Firebase initialized.")
            } else {
                android.util.Log.d("Firebase", "Firebase already initialized. Apps: ${FirebaseApp.getApps(this)}")
            }
        } catch (e: Exception) {
            android.util.Log.e("Firebase", "Failed to init FIREBASE APP", e)
        }

        try {
            val apiKey = BuildConfig.ENTRIG_API_KEY
            if (apiKey != "DEFAULT_ENTRIG_API_KEY" && apiKey.isNotBlank()) {
                Entrig.initialize(this, EntrigConfig(apiKey = apiKey))
                android.util.Log.d("Entrig", "Entrig SDK initialized with key.")
            } else {
                android.util.Log.e("Entrig", "Entrig SDK key is missing or default. Add ENTRIG_API_KEY to AI Studio Secrets!")
            }
        } catch (e: Throwable) {
            android.util.Log.e("Entrig", "Entrig init failed", e)
        }
    }
}
INNER_EOF
