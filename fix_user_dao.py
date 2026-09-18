import re
with open("app/src/main/java/com/example/data/db/Daos.kt", "r") as f:
    text = f.read()
target = """    suspend fun getUserSync(): User?
    @Insert(onConflict = OnConflictStrategy.REPLACE)"""
replacement = """    suspend fun getUserSync(): User?
    @Query("DELETE FROM users")
    suspend fun clearAll()
    @Insert(onConflict = OnConflictStrategy.REPLACE)"""
text = text.replace(target, replacement)
with open("app/src/main/java/com/example/data/db/Daos.kt", "w") as f:
    f.write(text)
print("Added clearAll")
