import re
with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    text = f.read()

new_method = """
    fun loginWithGoogleToken(idToken: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(credential).await()
                    repository.fetchDataFromServer(force = true)
                }
                if (repository.user.firstOrNull() == null) {
                    repository.saveUserProfile("Player_" + (1000..9999).random(), "")
                }
                
                val username = repository.user.firstOrNull()?.username ?: "Warrior"
                prefs.edit().putBoolean("is_logged_in", true).apply()
                _isLoggedIn.value = true
                checkLoginStreak()
                _toastMessage.emit("Welcome back, ${username}!")
                onComplete()
            } catch (e: Exception) {
                _dbErrorDialog.value = "Google Login Failed: ${e.message}"
            } finally {
                _isAuthLoading.value = false
            }
        }
    }
"""

class_end = "}\n"
# insert before the last '}'
text = text[:text.rfind('}')] + new_method + "}\n"

with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
    f.write(text)
print("Added loginWithGoogleToken")
