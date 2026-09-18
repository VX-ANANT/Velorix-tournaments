import re
import os

screens = [
    "app/src/main/java/com/example/ui/screens/TournamentDetailsScreen.kt",
    "app/src/main/java/com/example/ui/screens/WalletScreen.kt",
    "app/src/main/java/com/example/ui/screens/ProfileScreen.kt",
    "app/src/main/java/com/example/ui/screens/MatchesScreen.kt",
    "app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt",
    "app/src/main/java/com/example/ui/screens/HomeScreen.kt",
]

for file_path in screens:
    if not os.path.exists(file_path): continue
    with open(file_path, "r") as f:
        text = f.read()

    # Clean out all previous injects
    text = text.replace("    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current\n", "")
    text = text.replace("    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current", "")
    
    # Fix the ProfileScreen typo
    text = text.replace("    onNavigateToSupport: () -> Unit = {\n}", "    onNavigateToSupport: () -> Unit = {}")
    text = text.replace("    onNavigateToSupport: () -> Unit = {\n    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current\n}", "    onNavigateToSupport: () -> Unit = {}")

    # For each @Composable fun, inject `val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current`
    
    lines = text.split("\n")
    new_lines = []
    
    i = 0
    in_composable = False
    while i < len(lines):
        line = lines[i]
        new_lines.append(line)
        if line.strip().startswith("@Composable"):
            in_composable = True
        
        if in_composable and line.strip().startswith("fun "):
            # Read until {
            while "{" not in new_lines[-1] and i + 1 < len(lines):
                i += 1
                new_lines.append(lines[i])
            
            # Now we are at the { line.
            new_lines.append("    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current")
            in_composable = False
            
        i += 1
                
    with open(file_path, "w") as f:
        f.write("\n".join(new_lines))

print("Cleaned up and reinjected haptic robustly")
