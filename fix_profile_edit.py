import re
with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    text = f.read()

target = """    fun updateProfile(updatedUser: User) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.updateProfile(updatedUser)
                }
                _toastMessage.emit("Profile updated successfully!")
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "Failed to update profile")
            }
        }
    }"""

replacement = """    fun updateProfile(updatedUser: User, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.updateProfile(updatedUser)
                }
                _toastMessage.emit("Profile updated successfully!")
                onComplete(true)
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "Failed to update profile")
                onComplete(false)
            }
        }
    }"""

text = text.replace(target, replacement)
with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
    f.write(text)
