with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

target = """                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0x3009090B), Color(0x1018181B)))) // Lighter gradient for blur"""
replacement = """                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0x1A09090B), Color(0x0518181B)))) // Very light gradient for more visible blur"""

text = text.replace(target, replacement)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)
