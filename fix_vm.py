import re
with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    text = f.read()

# Remove the incorrectly appended part
to_remove = """}

    fun applyReferralCode(code: String) {
        viewModelScope.launch {
            val currentUser = repository.user.firstOrNull() ?: return@launch
            if (currentUser.referredBy.isNullOrEmpty()) {
                val updated = currentUser.copy(referredBy = code, tokens = currentUser.tokens + 50)
                repository.updateUserProfile(updated)
                _toastMessage.emit("Referral code applied! 50 tokens added.")
            }
        }
    }"""

if to_remove in text:
    text = text.replace(to_remove, "}")
    
    # insert inside the class
    class_end = "}\n"
    method_to_add = """    fun applyReferralCode(code: String) {
        viewModelScope.launch {
            val currentUser = repository.user.firstOrNull() ?: return@launch
            if (currentUser.referredBy.isNullOrEmpty()) {
                val updated = currentUser.copy(referredBy = code, tokens = currentUser.tokens + 50)
                repository.updateUserProfile(updated)
                _toastMessage.emit("Referral code applied! 50 tokens added.")
            }
        }
    }
}
"""
    # Replace the very last '}'
    text = text[:text.rfind('}')] + method_to_add
    
    with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
        f.write(text)
else:
    print("Not found")
