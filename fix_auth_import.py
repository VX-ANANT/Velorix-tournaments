import re
with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "r") as f:
    text = f.read()

text = text.replace("package com.example.ui.screens\n", "package com.example.ui.screens\n\nimport androidx.compose.animation.core.animateFloat\nimport androidx.compose.animation.core.FastOutSlowInEasing\n")

text = text.replace("easing = androidx.compose.animation.core.InOutSine", "easing = FastOutSlowInEasing")

with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "w") as f:
    f.write(text)
