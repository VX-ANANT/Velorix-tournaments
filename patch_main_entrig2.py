import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

replacement = """        val isEmulator = android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("Emulator") || android.os.Build.HARDWARE.contains("goldfish") || android.os.Build.HARDWARE.contains("ranchu")
        try {
            if (!isEmulator) {
                Entrig.setOnForegroundNotificationListener { notification ->
                    android.util.Log.d("Entrig", "Foreground notification: ${notification.title}")
                    // Manually show the notification in foreground to ensure it appears in the emulator
                    val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    val channelId = "entrig_test_channel"
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        val channel = android.app.NotificationChannel(channelId, "Test Channel", android.app.NotificationManager.IMPORTANCE_HIGH)
                        notificationManager.createNotificationChannel(channel)
                    }
                    val builder = androidx.core.app.NotificationCompat.Builder(this@MainActivity, channelId)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(notification.title)
                        .setContentText(notification.body ?: "Test body")
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                    notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
                }
                Entrig.setOnNotificationOpenedListener { notification ->
                    android.util.Log.d("Entrig", "Notification opened: ${notification.title}")
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("Entrig", "Entrig setup listeners failed", e)
        }"""

content = re.sub(r"        try \{\s*Entrig\.setOnForegroundNotificationListener.*?Entrig setup listeners failed\", e\)\s*\}", replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
