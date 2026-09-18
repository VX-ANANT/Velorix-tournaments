import re
with open("app/src/main/java/com/example/data/db/Daos.kt", "r") as f:
    text = f.read()
text = text.replace('fun getUser(): Flow<User?>', 'fun getUser(): Flow<User?>\n    @Query("SELECT * FROM users LIMIT 1")\n    suspend fun getUserSync(): User?')
with open("app/src/main/java/com/example/data/db/Daos.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/data/repository/PlatformRepository.kt", "r") as f:
    text = f.read()
text = text.replace('val user: Flow<User?> = db.userDao().getUser()', 'val user: Flow<User?> = db.userDao().getUser()\n    suspend fun getUserSync(): User? = db.userDao().getUserSync()')
with open("app/src/main/java/com/example/data/repository/PlatformRepository.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    text = f.read()

# For google login token (the fallback code paths, normal logic, etc)
text = text.replace('var userItem = repository.user.firstOrNull()', 'var userItem = repository.getUserSync()')
text = text.replace('userItem = repository.user.firstOrNull()', 'userItem = repository.getUserSync()')

# Also for loginWithGoogle there's `var u = repository.user.firstOrNull()`
text = text.replace('var u = repository.user.firstOrNull()', 'var u = repository.getUserSync()')
text = text.replace('u = repository.user.firstOrNull()', 'u = repository.getUserSync()')
text = text.replace('val userItem = repository.user.firstOrNull()', 'val userItem = repository.getUserSync()')

with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
    f.write(text)
print("Sync fixes applied")
