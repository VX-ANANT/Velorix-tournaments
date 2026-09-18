import re
with open("app/src/main/java/com/example/ui/screens/OnboardingScreen.kt", "r") as f:
    text = f.read()

target = "import androidx.compose.ui.graphics.Color"
replacement = "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.graphicsLayer"
text = text.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/OnboardingScreen.kt", "w") as f:
    f.write(text)
