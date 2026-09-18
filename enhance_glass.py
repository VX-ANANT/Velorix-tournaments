import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

# Enhance Pill Background
text = text.replace(
    ".background(Color(0x99000000)) // Translucent black",
    ".background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xCC09090B), Color(0x8018181B)))) // Glass gradient"
)

# Enhance Circle Button Background
text = text.replace(
    ".background(Color(0x99000000))",
    ".background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xCC09090B), Color(0x8018181B))))"
)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)

