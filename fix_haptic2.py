import os
import re

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
    
    parts = re.split(r'(@Composable\s*(?:@OptIn[^\n]*\n)*\s*fun\s+[A-Za-z0-9_]+\s*\([^)]*\)\s*(?::\s*[A-Za-z0-9_]+\s*)?\{)', text)
    
    new_text = parts[0]
    for i in range(1, len(parts), 2):
        sig = parts[i]
        body = parts[i+1]
        new_text += sig + "\n    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current\n" + body
        
    with open(file_path, "w") as f:
        f.write(new_text)

print("Cleaned up and reinjected haptic robustly 2")
