import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

fcm_block = """                val isEmulator = android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("Emulator") || android.os.Build.HARDWARE.contains("goldfish") || android.os.Build.HARDWARE.contains("ranchu")
                if (!isEmulator) {
                    try {
                        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("tournaments")
                            .addOnCompleteListener { task ->
                                if (!task.isSuccessful) {
                                    android.util.Log.d("FCM", "Topic subscription failed")
                                } else {
                                    android.util.Log.d("FCM", "Subscribed to tournaments topic")
                                }
                            }
                        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                android.util.Log.w("FCM_TOKEN", "Fetching FCM registration token failed", task.exception)
                                return@addOnCompleteListener
                            }
                            val token = task.result
                            android.util.Log.d("FCM_TOKEN", "FCM Registration Token: $token")
                            viewModel.updateFcmToken(token)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FCM", "FCM initialization failed", e)
                    }
                } else {
                    android.util.Log.d("FCM", "Skipping FCM setup on emulator to prevent broker errors.")
                }"""

# We need to replace the two FirebaseMessaging calls with the block above.
# Let's find the exact block to replace.

content = re.sub(
    r"com\.google\.firebase\.messaging\.FirebaseMessaging\.getInstance\(\)\.subscribeToTopic.*?viewModel\.updateFcmToken\(token\)\s*\}",
    fcm_block,
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
