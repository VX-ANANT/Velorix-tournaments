with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

target = """                                        style = HazeStyle(
                                            tint = Color.Black.copy(alpha = 0.05f),"""
replacement = """                                        style = HazeStyle(
                                            tint = Color.Transparent,"""

text = text.replace(target, replacement)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)
