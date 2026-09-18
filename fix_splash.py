import re
with open("app/src/main/java/com/example/ui/screens/SplashScreen.kt", "r") as f:
    text = f.read()

target = """            // Loading bar
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color(0xFFF91E4E).copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progressAnim)
                        .fillMaxHeight()
                        .background(Color(0xFFF91E4E))
                )
            }"""

replacement = """            // Animated HTML-like Geometric Loader
            Spacer(modifier = Modifier.height(32.dp))
            com.example.ui.components.AnimatedLoaders(
                modifier = Modifier.wrapContentSize(),
            )"""

text = text.replace(target, replacement)
with open("app/src/main/java/com/example/ui/screens/SplashScreen.kt", "w") as f:
    f.write(text)
