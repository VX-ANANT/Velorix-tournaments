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

    # Clean out all previous injects
    text = text.replace("    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current\n", "")
    text = text.replace("    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current", "")

    # For each @Composable fun, find the opening brace { and inject
    lines = text.split("\n")
    new_lines = []
    
    i = 0
    while i < len(lines):
        line = lines[i]
        new_lines.append(line)
        if line.strip().startswith("@Composable"):
            # Next few lines should be `fun ... {`
            while i + 1 < len(lines):
                i += 1
                next_line = lines[i]
                new_lines.append(next_line)
                
                # Check if it has an opening brace that belongs to the function body
                # If we see `) {` or just `{` at the end of a line
                if "{" in next_line and not next_line.strip().startswith("onClick = {"):
                    # We might have `) {` or something
                    if re.search(r'\)\s*\{', next_line) or (next_line.strip() == "{"):
                        new_lines.append("    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current")
                        break
        i += 1
                
    with open(file_path, "w") as f:
        f.write("\n".join(new_lines))

print("Cleaned up and reinjected haptic v3")
