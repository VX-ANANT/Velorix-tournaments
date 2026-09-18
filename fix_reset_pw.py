import re
with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    text = f.read()

target = """    fun resetPassword(email: String) {
        viewModelScope.launch {
            if (email.isBlank()) {
                _toastMessage.emit("Please enter your email to reset password.")
                return@launch
            }
            try {
                auth.sendPasswordResetEmail(email).await()
                _toastMessage.emit("Password reset email sent. Please check your inbox.")
            } catch (e: Exception) {
                _toastMessage.emit("Error: ${e.message}")
            }
        }
    }"""
replacement = """    fun resetPassword(email: String, onComplete: (Boolean, String) -> Unit = {_,_ ->}) {
        viewModelScope.launch {
            if (email.isBlank()) {
                onComplete(false, "Please enter your email to reset password.")
                return@launch
            }
            try {
                auth.sendPasswordResetEmail(email).await()
                onComplete(true, "Password reset email sent. Please check your inbox.")
            } catch (e: Exception) {
                onComplete(false, "Error: ${e.message}")
            }
        }
    }"""
text = text.replace(target, replacement)
with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
    f.write(text)
