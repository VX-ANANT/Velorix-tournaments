with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

target1 = """                                        style = HazeStyle(
                                            tint = Color.Black.copy(alpha = 0.2f),"""
replacement1 = """                                        style = HazeStyle(
                                            tint = Color.Black.copy(alpha = 0.05f),"""

target2 = """                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0x8009090B), Color(0x4018181B)))) // Lighter gradient for blur"""
replacement2 = """                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0x3009090B), Color(0x1018181B)))) // Lighter gradient for blur"""

target3 = """                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
                    ),"""
replacement3 = """                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                    ),"""

text = text.replace(target1, replacement1)
text = text.replace(target2, replacement2)
text = text.replace(target3, replacement3)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)
