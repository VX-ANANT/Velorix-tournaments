with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

import re

# 1. Global background
text = re.sub(
    r"androidx\.compose\.ui\.graphics\.Brush\.verticalGradient\(\s*colors = listOf\(\s*Color\(0xFF09090B\),\s*Color\(0xFF000000\)\s*\)\s*\)",
    r"androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background))",
    text
)

# 2. Navigation bar background
text = text.replace("color = Color(0xFF18181B).copy(alpha = 0.85f),", "color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),")
text = text.replace("border = BorderStroke(1.dp, Color(0xFF3F3F46).copy(alpha = 0.6f))", "border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)
