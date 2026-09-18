with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "init {" in line:
        start_idx = i
        break

for i in range(start_idx, len(lines)):
    if "override fun onError(error : FirebaseRemoteConfigException)" in lines[i]:
        end_idx = i + 3
        break

replacement = """    init {
        val isEmulator = try {
            android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("Emulator") || android.os.Build.HARDWARE.contains("goldfish") || android.os.Build.HARDWARE.contains("ranchu")
        } catch(e: Exception) { true }

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
    }
"""

new_lines = lines[:start_idx] + [replacement] + lines[end_idx:]

with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
    f.writelines(new_lines)
