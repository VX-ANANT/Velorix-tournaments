import re
with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "r") as f:
    text = f.read()

target = """                                viewModel.updateProfile(updatedUser)
                                isEditing = false"""

replacement = """                                viewModel.updateProfile(updatedUser) { success ->
                                    if (success) {
                                        isEditing = false
                                    }
                                }"""

text = text.replace(target, replacement)
with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "w") as f:
    f.write(text)
