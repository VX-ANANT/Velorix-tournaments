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
    
    # Strip any garbage
    text = text.replace("package com.example.ui.screens\n", "")
    text = text.replace("package com.example.ui.components\n", "")
    text = text.replace("package com.example\n", "")
    
    lines = text.split("\n")
    clean_lines = []
    
    has_animateFloat = False
    has_animateFloatAsState = False
    has_scale = False
    
    for l in lines:
        if l.strip() == "": continue
        if l.startswith("import androidx.compose.animation.core.animateFloatAsState"):
            has_animateFloatAsState = True
        elif l.startswith("import androidx.compose.animation.core.animateFloat"):
            has_animateFloat = True
        elif l.startswith("import androidx.compose.ui.draw.scale"):
            has_scale = True
        elif l.startswith("package "):
            continue
        else:
            clean_lines.append(l)
            
    header = pkg + "\n"
    header += "import androidx.compose.animation.core.animateFloat\n"
    header += "import androidx.compose.animation.core.animateFloatAsState\n"
    header += "import androidx.compose.ui.draw.scale\n"
    
    final_text = header + "\n".join(clean_lines)
    
    # ensure any stray ".ui.screensimport" are gone
    final_text = final_text.replace(".ui.screensimport", "\nimport")
    
    with open(file, "w") as f:
        f.write(final_text)

