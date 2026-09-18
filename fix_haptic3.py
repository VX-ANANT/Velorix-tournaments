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
        
    text = text.replace("    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current\n", "")
    text = text.replace("    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current", "")

    lines = text.split("\n")
    new_lines = []
    
    in_composable = False
    brace_count = 0
    in_params = False
    
    for line in lines:
        new_lines.append(line)
        
        if line.strip().startswith("@Composable"):
            in_composable = True
            
        if in_composable and line.strip().startswith("fun "):
            in_params = True
            
        if in_params:
            if ") {" in line:
                # We found the end of the parameters!
                new_lines.append("    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current")
                in_composable = False
                in_params = False
            elif ") : " in line or ") :" in line: # if it has a return type
                pass
            elif line.strip() == "{":
                # Maybe it ended on the previous line and this is the brace
                new_lines.append("    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current")
                in_composable = False
                in_params = False
                
    with open(file_path, "w") as f:
        f.write("\n".join(new_lines))

print("Cleaned up and reinjected haptic robustly 3")
