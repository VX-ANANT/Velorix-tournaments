import re

with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    text = f.read()

update_fcm = """
    fun updateFcmToken(token: String) {
        viewModelScope.launch {
            repository.updateFcmToken(token)
        }
    }
"""

text = text.replace("    fun login(", update_fcm + "\n    fun login(")

with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
    f.write(text)

