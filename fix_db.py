with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    text = f.read()
text = text.replace("repository.db.userDao().clearAll()", "repository.clearUserDatabase()")
with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/data/repository/PlatformRepository.kt", "r") as f:
    text = f.read()
target = """    suspend fun saveSearchQuery(query: String) {"""
replacement = """    suspend fun clearUserDatabase() {
        db.userDao().clearAll()
    }
    suspend fun saveSearchQuery(query: String) {"""
text = text.replace(target, replacement)
with open("app/src/main/java/com/example/data/repository/PlatformRepository.kt", "w") as f:
    f.write(text)
