import re
import os

screens = [
    "app/src/main/java/com/example/ui/screens/TournamentDetailsScreen.kt",
    "app/src/main/java/com/example/ui/screens/WalletScreen.kt",
    "app/src/main/java/com/example/ui/screens/ProfileScreen.kt",
    "app/src/main/java/com/example/ui/screens/MatchesScreen.kt",
    "app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt",
]

for file_path in screens:
    if not os.path.exists(file_path): continue
    with open(file_path, "r") as f:
        text = f.read()

    # 1. Remove all standalone `val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current` lines that were added
    # We will just remove the exact string with leading/trailing whitespaces.
    lines = text.split("\n")
    cleaned_lines = []
    for line in lines:
        if line.strip() == "val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current":
            continue
        cleaned_lines.append(line)
        
    text = "\n".join(cleaned_lines)
    
    # 2. Inject `val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current` at the start of the body `{` of @Composable functions
    # A robust way is to find `@Composable`, then find the next `{` and insert after it.
    
    parts = text.split("@Composable")
    new_text = parts[0]
    
    for i in range(1, len(parts)):
        part = parts[i]
        # Find the first `{`
        idx = part.find("{")
        if idx != -1:
            part = part[:idx+1] + "\n    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current" + part[idx+1:]
        new_text += "@Composable" + part
        
    with open(file_path, "w") as f:
        f.write(new_text)

print("Cleaned up and reinjected haptic")
