import re
with open("app/src/main/java/com/example/data/repository/PlatformRepository.kt", "r") as f:
    text = f.read()

target = """    suspend fun submitAccountDeletionRequest(reason: String, details: String) {
        val userItem = user.firstOrNull() ?: throw Exception("User not found locally")
        val deletionData = hashMapOf(
            "userId" to userItem.id,
            "username" to userItem.username,
            "phoneOrEmail" to userItem.phoneOrEmail,
            "reason" to reason,
            "details" to details,
            "requestedAt" to System.currentTimeMillis()
        )
        try {
            kotlinx.coroutines.withTimeout(15000L) {
                firestore.collection("accountDeletionRequests").add(deletionData).await()
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            throw Exception("Network timeout. Please check your connection.")
        }
        db.userDao().clearAll()
        db.transactionDao().clearAll()
    }"""

replacement = """    suspend fun submitAccountDeletionRequest(reason: String, details: String) {
        val userItem = user.firstOrNull() ?: throw Exception("User not found locally")
        val deletionData = hashMapOf(
            "userId" to userItem.id,
            "username" to userItem.username,
            "phoneOrEmail" to userItem.phoneOrEmail,
            "reason" to reason,
            "details" to details,
            "requestedAt" to System.currentTimeMillis()
        )
        db.userDao().clearAll()
        db.transactionDao().clearAll()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                firestore.collection("accountDeletionRequests").add(deletionData).await()
            } catch (e: Exception) {
                Log.e("PlatformRepository", "Failed to submit deletion request", e)
            }
        }
    }"""

text = text.replace(target, replacement)
with open("app/src/main/java/com/example/data/repository/PlatformRepository.kt", "w") as f:
    f.write(text)
