import re
with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "r") as f:
    text = f.read()

text = text.replace("    onNavigateToSupport: () -> Unit = {}\n)\n{\n    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current", "    onNavigateToSupport: () -> Unit = {}\n) {\n    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current")

with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "w") as f:
    f.write(text)
