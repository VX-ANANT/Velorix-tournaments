import re

with open("app/src/main/java/com/example/service/MyFirebaseMessagingService.kt", "r") as f:
    text = f.read()

update_logic = """
        // Update the token in the repository
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val db = com.example.data.local.AppDatabase.getDatabase(applicationContext)
                val repository = com.example.data.repository.PlatformRepository(db)
                repository.updateFcmToken(token)
            } catch (e: Exception) {
                Log.e("FCM_TOKEN", "Failed to update token in backend", e)
            }
        }
"""

text = text.replace('        // Ideally send this token to Supabase/Backend to link it to the user.', update_logic)
text = "import kotlinx.coroutines.launch\n" + text

with open("app/src/main/java/com/example/service/MyFirebaseMessagingService.kt", "w") as f:
    f.write(text)

