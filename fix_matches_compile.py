import re

with open("app/src/main/java/com/example/ui/screens/MatchesScreen.kt", "r") as f:
    text = f.read()

text = text.replace("import androidx.compose.animation.animateContentSize\npackage com.example.ui.screens", "package com.example.ui.screens\nimport androidx.compose.animation.animateContentSize")

with open("app/src/main/java/com/example/ui/screens/MatchesScreen.kt", "w") as f:
    f.write(text)

