import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

replacement = """                LaunchedEffect(user?.id) {
                    val isEmulator = android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("Emulator") || android.os.Build.HARDWARE.contains("goldfish") || android.os.Build.HARDWARE.contains("ranchu")
                    val userId = user?.id
                    if (userId != null) {
                        try {
                            if (!isEmulator) {
                                Entrig.register(userId.toString(), this@MainActivity) { success, error ->
                                    if (success) {
                                        android.util.Log.d("Entrig", "Registered Entrig for user: $userId")
                                    } else {
                                        android.util.Log.e("Entrig", "Entrig register failed: $error")
                                    }
                                }
                            } else {
                                android.util.Log.d("Entrig", "Skipping Entrig register on emulator")
                            }
                        } catch (e: Throwable) {
                            android.util.Log.e("Entrig", "Entrig register crashed", e)
                        }
                    } else {
                        try {
                            if (!isEmulator) {
                                Entrig.unregister()
                            }
                        } catch (e: Throwable) {}
                    }
                }"""

content = re.sub(r"                LaunchedEffect\(user\?\.id\)\s*\{\s*val userId = user\?\.id.*?try\s*\{\s*Entrig\.unregister\(\)\s*\}\s*catch\s*\(e:\s*Throwable\)\s*\{\}\s*\}\s*\}", replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
