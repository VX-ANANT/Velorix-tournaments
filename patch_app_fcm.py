import re

with open("app/src/main/java/com/example/MyApplication.kt", "r") as f:
    content = f.read()

replacement = """        val isEmulator = android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("Emulator") || android.os.Build.HARDWARE.contains("goldfish") || android.os.Build.HARDWARE.contains("ranchu")
        
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                android.util.Log.d("Firebase", "Firebase initialized.")
            } else {
                android.util.Log.d("Firebase", "Firebase already initialized.")
            }
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
        }"""

content = re.sub(r"        try \{\s*if \(FirebaseApp\.getApps.*?Entrig init failed\", e\)\s*\}", replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/MyApplication.kt", "w") as f:
    f.write(content)
