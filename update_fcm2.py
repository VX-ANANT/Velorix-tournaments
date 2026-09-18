import re

with open("app/src/main/java/com/example/data/repository/PlatformRepository.kt", "r") as f:
    text = f.read()

update_fcm = """
    suspend fun updateFcmToken(token: String) {
        try {
            val userItem = user.firstOrNull() ?: return
            db.userDao().update(userItem.copy(fcmToken = token))
            firestore.collection("users").document(userItem.id).update("fcmToken", token).await()
        } catch (e: Exception) {
            android.util.Log.e("PlatformRepository", "Failed to update FCM token", e)
        }
    }
"""

text = text.replace("    suspend fun updateSessionToken(token: String) {", update_fcm + "\n    suspend fun updateSessionToken(token: String) {")

with open("app/src/main/java/com/example/data/repository/PlatformRepository.kt", "w") as f:
    f.write(text)

