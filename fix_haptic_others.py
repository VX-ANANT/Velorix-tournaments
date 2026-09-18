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

    # In each Composable function, inject val haptic if there's clickable or onClick
    lines = text.split("\n")
    new_lines = []
    in_composable = False
    
    for i, line in enumerate(lines):
        if "@Composable" in line:
            in_composable = True
        elif line.startswith("fun ") and in_composable:
            new_lines.append(line)
            # check if it already has haptic
            if "val haptic" not in text:
                new_lines.append("    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current")
            in_composable = False
            continue
            
        # simple replacement
        if "clickable {" in line and "haptic.performHapticFeedback" not in line:
            line = line.replace("clickable {", "clickable { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove);")
        
        # We must be careful with onClick = {
        # Sometimes it's `onClick = onBack`, `onClick = { ... }`
        if "onClick = {" in line and "haptic.performHapticFeedback" not in line:
            line = line.replace("onClick = {", "onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove);")
            
        new_lines.append(line)
        
    with open(file_path, "w") as f:
        f.write("\n".join(new_lines))

print("Applied haptics to others")
