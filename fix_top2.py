import os
import re

files_to_fix = [
    ("app/src/main/java/com/example/MainActivity.kt", "package com.example\n"),
    ("app/src/main/java/com/example/ui/components/VeloRixButton.kt", "package com.example.ui.components\n"),
    ("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "package com.example.ui.screens\n"),
    ("app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt", "package com.example.ui.screens\n"),
    ("app/src/main/java/com/example/ui/screens/SplashScreen.kt", "package com.example.ui.screens\n"),
    ("app/src/main/java/com/example/ui/components/TournamentCard.kt", "package com.example.ui.components\n")
]

for file, pkg in files_to_fix:
    with open(file, "r") as f:
        text = f.read()
    
    text = text.replace("package com.example\n", "")
    text = text.replace(".ui.screensimport", "\nimport")
    text = text.replace(".ui.componentsimport", "\nimport")
    
    # ensure clean top
    lines = text.split("\n")
    clean_lines = []
    for l in lines:
        if l.startswith("package"):
            continue
        if l.startswith("import androidx.compose.animation.core.animateFloat") or l.startswith("import androidx.compose.ui.draw.scale"):
            continue
        clean_lines.append(l)
        
    final_text = pkg + "import androidx.compose.animation.core.animateFloat\nimport androidx.compose.ui.draw.scale\n" + "\n".join(clean_lines)
    
    with open(file, "w") as f:
        f.write(final_text)

