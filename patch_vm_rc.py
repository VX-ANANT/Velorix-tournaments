import re

with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    content = f.read()

replacement = """    init {
        val isEmulator = android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("Emulator") || android.os.Build.HARDWARE.contains("goldfish") || android.os.Build.HARDWARE.contains("ranchu")
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(com.example.R.xml.remote_config_defaults)

        if (!isEmulator) {
            remoteConfig.fetchAndActivate()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        android.util.Log.d("RemoteConfig", "Config values fetched and activated: ${task.result}")
                    } else {
                        android.util.Log.e("RemoteConfig", "Error fetching Remote Config", task.exception)
                    }
                }

            remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
                override fun onUpdate(configUpdate : ConfigUpdate) {
                    android.util.Log.d("RemoteConfig", "Updated keys: " + configUpdate.updatedKeys)
                    remoteConfig.activate().addOnCompleteListener { }
                }

                override fun onError(error : FirebaseRemoteConfigException) {
                    android.util.Log.w("RemoteConfig", "Config update error with code: " + error.code, error)
                }
            })
        }
    }"""

content = re.sub(
    r"    init\s*\{.*?remoteConfig\.addOnConfigUpdateListener.*?\}\)\s*\}",
    replacement,
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
    f.write(content)
