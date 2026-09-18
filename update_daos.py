import re

with open("app/src/main/java/com/example/data/db/Daos.kt", "r") as f:
    text = f.read()

new_dao = """
@Dao
interface MissionDao {
    @Query("SELECT * FROM missions")
    fun getAllMissions(): Flow<List<com.example.data.model.Mission>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(missions: List<com.example.data.model.Mission>)

    @Update
    suspend fun update(mission: com.example.data.model.Mission)
}
"""

if "interface MissionDao" not in text:
    text += "\n" + new_dao
    with open("app/src/main/java/com/example/data/db/Daos.kt", "w") as f:
        f.write(text)

