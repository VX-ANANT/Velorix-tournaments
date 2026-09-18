import os
import re

files_to_fix = [
    "app/src/main/java/com/example/MainActivity.kt",
    "app/src/main/java/com/example/ui/components/VeloRixButton.kt",
    "app/src/main/java/com/example/ui/screens/HomeScreen.kt",
    "app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt",
    "app/src/main/java/com/example/ui/screens/SplashScreen.kt",
    "app/src/main/java/com/example/ui/components/TournamentCard.kt"
]

for file in files_to_fix:
    with open(file, "r") as f:
        lines = f.readlines()
    
    # Filter out bad lines
    good_lines = []
    has_package = False
    pkg_line = ""
    for line in lines:
        if line.startswith("package com.example"):
            if not has_package:
                if ".ui.components" in line:
                    pkg_line = "package com.example.ui.components\n"
                elif ".ui.screens" in line:
                    pkg_line = "package com.example.ui.screens\n"
                else:
                    pkg_line = "package com.example\n"
                has_package = True
        elif line.startswith("import androidx.compose.animation.core.animateFloat") or line.startswith("import androidx.compose.ui.draw.scale") or line.startswith("import androidx.compose.ui.draw.scale.ui.screens"):
            pass
        else:
            good_lines.append(line)
            
    # Reassemble
    final_content = pkg_line + "\nimport androidx.compose.animation.core.animateFloat\nimport androidx.compose.ui.draw.scale\n" + "".join(good_lines)
    
    with open(file, "w") as f:
        f.write(final_content)

