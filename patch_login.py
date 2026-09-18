import re
with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    text = f.read()

def replace_all(text):
    # For loginWithGoogle
    target_google = """                    if (repository.user.firstOrNull() == null) {
                        repository.saveUserProfile("Player_" + (1000..9999).random(), "")
                    }
                }
                if (_isAuthLoading.value) { // means fallback was not triggered
                    val username = repository.user.firstOrNull()?.username ?: "Warrior"
                    prefs.edit().putBoolean("is_logged_in", true).apply()
                    _isLoggedIn.value = true
                    checkLoginStreak()
                    _toastMessage.emit("Welcome back, ${username}!")
                    onComplete()
                }"""
                
    replacement_google = """                    var u = repository.user.firstOrNull()
                    if (u == null) {
                        repository.saveUserProfile("Player_" + (1000..9999).random(), "")
                        u = repository.user.firstOrNull()
                    }
                }
                if (_isAuthLoading.value) { // means fallback was not triggered
                    val userItem = repository.user.firstOrNull()
                    val username = userItem?.username ?: "Warrior"
                    if (userItem != null && userItem.inGameName.isNotBlank()) {
                        _hasCompletedOnboarding.value = true
                        prefs.edit().putBoolean("has_completed_onboarding", true).apply()
                    }
                    prefs.edit().putBoolean("is_logged_in", true).apply()
                    _isLoggedIn.value = true
                    checkLoginStreak()
                    _toastMessage.emit("Welcome back, ${username}!")
                    onComplete()
                }"""
                
    text = text.replace(target_google, replacement_google)
    
    # For normal login
    target_login = """                if (repository.user.firstOrNull() == null) {
                    repository.saveUserProfile("Player_" + (1000..9999).random(), phoneOrEmail)
                }
                val username = repository.user.firstOrNull()?.username ?: "Warrior"
                prefs.edit().putBoolean("is_logged_in", true).apply()
                _isLoggedIn.value = true
                checkLoginStreak()
                _toastMessage.emit("Welcome back, ${username}!")
                onComplete()"""
                
    replacement_login = """                var userItem = repository.user.firstOrNull()
                if (userItem == null) {
                    repository.saveUserProfile("Player_" + (1000..9999).random(), phoneOrEmail)
                    userItem = repository.user.firstOrNull()
                }
                val username = userItem?.username ?: "Warrior"
                if (userItem != null && userItem.inGameName.isNotBlank()) {
                    _hasCompletedOnboarding.value = true
                    prefs.edit().putBoolean("has_completed_onboarding", true).apply()
                }
                prefs.edit().putBoolean("is_logged_in", true).apply()
                _isLoggedIn.value = true
                checkLoginStreak()
                _toastMessage.emit("Welcome back, ${username}!")
                onComplete()"""
    
    text = text.replace(target_login, replacement_login)
    
    # For google token
    target_token = """                if (repository.user.firstOrNull() == null) {
                    repository.saveUserProfile("Player_" + (1000..9999).random(), "")
                }
                val username = repository.user.firstOrNull()?.username ?: "Warrior"
                prefs.edit().putBoolean("is_logged_in", true).apply()
                _isLoggedIn.value = true
                checkLoginStreak()
                _toastMessage.emit("Welcome back, ${username}!")
                onComplete()"""
                
    replacement_token = """                var userItem = repository.user.firstOrNull()
                if (userItem == null) {
                    repository.saveUserProfile("Player_" + (1000..9999).random(), "")
                    userItem = repository.user.firstOrNull()
                }
                val username = userItem?.username ?: "Warrior"
                if (userItem != null && userItem.inGameName.isNotBlank()) {
                    _hasCompletedOnboarding.value = true
                    prefs.edit().putBoolean("has_completed_onboarding", true).apply()
                }
                prefs.edit().putBoolean("is_logged_in", true).apply()
                _isLoggedIn.value = true
                checkLoginStreak()
                _toastMessage.emit("Welcome back, ${username}!")
                onComplete()"""
    
    text = text.replace(target_token, replacement_token)
    
    return text
    
with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
    f.write(replace_all(text))
