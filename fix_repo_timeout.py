import re
with open("app/src/main/java/com/example/data/repository/PlatformRepository.kt", "r") as f:
    text = f.read()

target = """    suspend fun updateProfile(updatedUser: User) {
        try {
            kotlinx.coroutines.withTimeout(15000L) {
                db.userDao().update(updatedUser)
                firestore.collection("users").document(updatedUser.id).set(updatedUser).await()
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.e("Room", "Profile update timeout", e)
            throw Exception("Network timeout.")
        } catch (e: Throwable) {
            Log.e("Room", "Failed to update profile", e)
            throw Exception("Failed to update profile.")
        }
    }"""

replacement = """    suspend fun updateProfile(updatedUser: User) {
        try {
            db.userDao().update(updatedUser)
            // Fire and forget for firestore
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    firestore.collection("users").document(updatedUser.id).set(updatedUser).await()
                } catch (e: Exception) {
                    Log.e("Room", "Failed to update profile on Firestore", e)
                }
            }
        } catch (e: Throwable) {
            Log.e("Room", "Failed to update profile locally", e)
            throw Exception("Failed to update profile.")
        }
    }"""

text = text.replace(target, replacement)
with open("app/src/main/java/com/example/data/repository/PlatformRepository.kt", "w") as f:
    f.write(text)
