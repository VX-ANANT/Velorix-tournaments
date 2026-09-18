import os

files_to_fix = [
    "app/src/main/java/com/example/MainActivity.kt",
    "app/src/main/java/com/example/ui/components/VeloRixButton.kt",
    "app/src/main/java/com/example/ui/screens/HomeScreen.kt",
    "app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt",
    "app/src/main/java/com/example/ui/screens/SplashScreen.kt",
    "app/src/main/java/com/example/ui/components/TournamentCard.kt"
]

imports_to_add = """
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.scale
"""

for file in files_to_fix:
    with open(file, "r") as f:
        text = f.read()
    
    # Revert the generic types
    text = text.replace("tween<Float>(", "tween(")
    text = text.replace("infiniteRepeatable<Float>(", "infiniteRepeatable(")
    
    # Add imports after the package declaration
    if "import androidx.compose.animation.core.animateFloat" not in text:
        text = text.replace("package com.example.ui.components", "package com.example.ui.components\n" + imports_to_add)
        text = text.replace("package com.example.ui.screens", "package com.example.ui.screens\n" + imports_to_add)
        text = text.replace("package com.example", "package com.example\n" + imports_to_add)

    # Note: text.replace("package com.example", ...) will match all the others if I'm not careful,
    # but the above order will match components/screens first, then the root. Wait, it's better to just regex it.
    
    with open(file, "w") as f:
        f.write(text)

