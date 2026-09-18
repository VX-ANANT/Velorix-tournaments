cat << 'INNER_EOF' > app/src/main/java/com/example/MyApplication.kt
package com.example

import android.app.Application
import com.entrig.sdk.Entrig
import com.entrig.sdk.models.EntrigConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        try {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:27931798964:android:8e63979e3ef30c1ebe56e1")
                .setApiKey("AIzaSyCb5K_V5FT5jeyydEO92RnoLHr75pdnTEs")
                .setProjectId("velorix-tournaments")
                .setDatabaseUrl("https://velorix-tournaments-default-rtdb.asia-southeast1.firebasedatabase.app")
                .setStorageBucket("velorix-tournaments.firebasestorage.app")
                .build()
            
            FirebaseApp.initializeApp(this, options)
            android.util.Log.d("Firebase", "Firebase manually initialized.")
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
