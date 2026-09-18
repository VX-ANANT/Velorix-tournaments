import re
with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    text = f.read()

target = """        viewModelScope.launch {
            val isFirebaseAuthed = try {
                withContext(Dispatchers.IO) {
                    auth.currentUser != null
                }
            } catch (e: Exception) { false }

            val prefsLoggedIn = prefs.getBoolean("is_logged_in", false)
            
            if (isFirebaseAuthed || prefsLoggedIn) {
                _isLoggedIn.value = true
                checkLoginStreak()
            } else {
                _isLoggedIn.value = false
            }
            _isCheckingAuth.value = false
        }"""

replacement = """        viewModelScope.launch {
            val isFirebaseAuthed = try {
                withContext(Dispatchers.IO) {
                    auth.currentUser != null
                }
            } catch (e: Exception) { false }
            
            if (isFirebaseAuthed) {
                _isLoggedIn.value = true
                prefs.edit().putBoolean("is_logged_in", true).apply()
                checkLoginStreak()
                withContext(Dispatchers.IO) {
                    repository.fetchDataFromServer(force = true)
                }
            } else {
                prefs.edit().putBoolean("is_logged_in", false).apply()
                _isLoggedIn.value = false
            }
            _isCheckingAuth.value = false
        }"""

text = text.replace(target, replacement)
with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
    f.write(text)
