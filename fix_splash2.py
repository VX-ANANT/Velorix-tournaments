import re
with open("app/src/main/java/com/example/ui/screens/SplashScreen.kt", "r") as f:
    text = f.read()

target = """            com.example.ui.components.AnimatedLoaders(
                modifier = Modifier.wrapContentSize(),
            )"""

replacement = """            com.example.ui.components.AnimatedLoaders(
                modifier = Modifier.wrapContentSize(),
                pathColor = Color(0xFFF91E4E).copy(alpha = 0.5f),
                dotColor = Color(0xFFF91E4E)
            )"""

text = text.replace(target, replacement)
with open("app/src/main/java/com/example/ui/screens/SplashScreen.kt", "w") as f:
    f.write(text)
