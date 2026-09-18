import re
with open("app/src/main/java/com/example/data/repository/PlatformRepository.kt", "r") as f:
    text = f.read()

# Fix joinTournament
target_join = """        try {
            kotlinx.coroutines.withTimeout(15000L) {
                db.userDao().update(updatedUser)
                db.tournamentDao().update(updatedMatch)
                db.transactionDao().insert(newTx)
                firestore.collection("users").document(userItem.id).set(updatedUser).await()
                firestore.collection("transactions").document(newTx.id).set(newTx).await()
                firestore.collection("tournaments").document(updatedMatch.id).set(updatedMatch).await()
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.e("Room", "Sync timeout.", e)
            return JoinResult.Failure("Network timeout. Please try again.")
        } catch (e: Throwable) {
            Log.e("Room", "Sync failed.", e)
            return JoinResult.Failure("Database error. Try again.")
        }"""
replacement_join = """        try {
            db.userDao().update(updatedUser)
            db.tournamentDao().update(updatedMatch)
            db.transactionDao().insert(newTx)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    firestore.collection("users").document(userItem.id).set(updatedUser).await()
                    firestore.collection("transactions").document(newTx.id).set(newTx).await()
                    firestore.collection("tournaments").document(updatedMatch.id).set(updatedMatch).await()
                } catch (e: Exception) {
                    Log.e("Room", "Sync to Firestore failed", e)
                }
            }
        } catch (e: Throwable) {
            Log.e("Room", "Local sync failed.", e)
            return JoinResult.Failure("Database error. Try again.")
        }"""
text = text.replace(target_join, replacement_join)

# Fix addFunds
target_funds = """        try {
            kotlinx.coroutines.withTimeout(15000L) {
                db.userDao().update(updatedUser)
                db.transactionDao().insert(newTx)
                firestore.collection("users").document(userItem.id).set(updatedUser).await()
                firestore.collection("transactions").document(newTx.id).set(newTx).await()
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.e("Room", "Add funds timeout.", e)
            throw Exception("Network timeout. Please check your connection.")
        } catch (e: Throwable) {
            Log.e("Room", "Failed to add funds locally", e)
            throw Exception("Failed to add funds.")
        }"""
replacement_funds = """        try {
            db.userDao().update(updatedUser)
            db.transactionDao().insert(newTx)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    firestore.collection("users").document(userItem.id).set(updatedUser).await()
                    firestore.collection("transactions").document(newTx.id).set(newTx).await()
                } catch (e: Exception) {
                    Log.e("Room", "Failed to sync funds to Firestore", e)
                }
            }
        } catch (e: Throwable) {
            Log.e("Room", "Failed to add funds locally", e)
            throw Exception("Failed to add funds.")
        }"""
text = text.replace(target_funds, replacement_funds)

# Fix requestWithdrawal
target_withdraw = """        try {
            kotlinx.coroutines.withTimeout(15000L) {
                db.userDao().update(updatedUser)
                db.transactionDao().insert(newTx)
                firestore.collection("users").document(userItem.id).set(updatedUser).await()
                firestore.collection("transactions").document(newTx.id).set(newTx).await()
                
                // Push withdrawal request to Firestore for admin approval
                val withdrawData = hashMapOf(
                    "userId" to userItem.id,
                    "username" to userItem.username,
                    "phoneOrEmail" to userItem.phoneOrEmail,
                    "amount" to amount,
                    "status" to "PENDING",
                    "timestamp" to System.currentTimeMillis()
                )
                firestore.collection("withdrawal_requests").add(withdrawData).await()
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.e("PlatformRepository", "Withdraw timeout", e)
            return WithdrawResult.Failure("Network timeout. Please check connection.")
        } catch (e: Throwable) {
            Log.e("PlatformRepository", "Withdraw failed", e)
            return WithdrawResult.Failure("Network error. Try again.")
        }"""
replacement_withdraw = """        try {
            db.userDao().update(updatedUser)
            db.transactionDao().insert(newTx)
            
            // Push withdrawal request to Firestore for admin approval
            val withdrawData = hashMapOf(
                "userId" to userItem.id,
                "username" to userItem.username,
                "phoneOrEmail" to userItem.phoneOrEmail,
                "amount" to amount,
                "status" to "PENDING",
                "timestamp" to System.currentTimeMillis()
            )
            
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    firestore.collection("users").document(userItem.id).set(updatedUser).await()
                    firestore.collection("transactions").document(newTx.id).set(newTx).await()
                    firestore.collection("withdrawal_requests").add(withdrawData).await()
                } catch (e: Exception) {
                    Log.e("PlatformRepository", "Failed to sync withdraw to Firestore", e)
                }
            }
        } catch (e: Throwable) {
            Log.e("PlatformRepository", "Withdraw failed locally", e)
            return WithdrawResult.Failure("Database error. Try again.")
        }"""
text = text.replace(target_withdraw, replacement_withdraw)

with open("app/src/main/java/com/example/data/repository/PlatformRepository.kt", "w") as f:
    f.write(text)
