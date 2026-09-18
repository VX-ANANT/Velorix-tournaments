import re

with open("app/src/main/java/com/example/data/repository/PlatformRepository.kt", "r") as f:
    text = f.read()

text = text.replace("import com.example.data.model.User", "import com.example.data.model.User\nimport com.example.data.model.Mission")

missions_val = """    val missions: Flow<List<Mission>> = db.missionDao().getAllMissions()
    
    suspend fun initializeMissions() {
        val currentMissions = db.missionDao().getAllMissions().firstOrNull() ?: emptyList()
        if (currentMissions.isEmpty()) {
            val initialMissions = listOf(
                Mission(title = "First Blood", description = "Get 5 kills in any match", target = 5, progress = 0, rewardCurrency = 10.0),
                Mission(title = "Veteran", description = "Play 3 matches", target = 3, progress = 0, rewardCurrency = 15.0),
                Mission(title = "Champion", description = "Win a match", target = 1, progress = 0, rewardCurrency = 50.0)
            )
            db.missionDao().insertAll(initialMissions)
        }
    }
    
    suspend fun claimMissionReward(mission: Mission): Boolean {
        val userItem = user.firstOrNull() ?: return false
        if (mission.isCompleted && !mission.isClaimed) {
            val updatedUser = userItem.copy(balance = userItem.balance + mission.rewardCurrency)
            val updatedMission = mission.copy(isClaimed = true)
            
            val newTx = Transaction(userId = userItem.id, type = "MISSION_REWARD", amount = mission.rewardCurrency, detail = "Claimed mission: ${mission.title}", isPositive = true, timestamp = System.currentTimeMillis())
            
            try {
                db.userDao().update(updatedUser)
                db.missionDao().update(updatedMission)
                db.transactionDao().insert(newTx)
                
                firestore.collection("users").document(userItem.id).set(updatedUser).await()
                firestore.collection("transactions").document(newTx.id).set(newTx).await()
                return true
            } catch (e: Exception) {
                Log.e("PlatformRepository", "Failed to claim mission", e)
                return false
            }
        }
        return false
    }
"""

if "val missions: Flow" not in text:
    text = text.replace("val searchHistory: Flow<List<String>> = db.searchHistoryDao().getRecentSearches()", "val searchHistory: Flow<List<String>> = db.searchHistoryDao().getRecentSearches()\n" + missions_val)
    with open("app/src/main/java/com/example/data/repository/PlatformRepository.kt", "w") as f:
        f.write(text)

