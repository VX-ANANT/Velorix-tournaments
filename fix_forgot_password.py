import re

with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    text = f.read()

forgot_password_fun = """
    /**
     * Sends a password reset email to the specified address.
     */
    fun resetPassword(email: String, onComplete: (Boolean, String) -> Unit) {
        if (email.isBlank()) {
            onComplete(false, "Please enter your email.")
            return
        }
        auth.sendPasswordResetEmail(email.trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, "Password reset email sent. Check your inbox.")
                } else {
                    onComplete(false, task.exception?.message ?: "Failed to send reset email.")
                }
            }
    }
"""

text = text.replace("    fun login(", forgot_password_fun + "\n    fun login(")

with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
    f.write(text)

