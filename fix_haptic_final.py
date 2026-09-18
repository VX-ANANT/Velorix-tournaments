import os
import re

screens = [
    "app/src/main/java/com/example/ui/screens/TournamentDetailsScreen.kt",
    "app/src/main/java/com/example/ui/screens/WalletScreen.kt",
    "app/src/main/java/com/example/ui/screens/ProfileScreen.kt",
    "app/src/main/java/com/example/ui/screens/MatchesScreen.kt",
    "app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt",
    "app/src/main/java/com/example/ui/screens/HomeScreen.kt",
    "app/src/main/java/com/example/MainActivity.kt"
]

for file_path in screens:
    if not os.path.exists(file_path): continue
    with open(file_path, "r") as f:
        text = f.read()

    # Remove all haptic.performHapticFeedback... calls entirely if we can't easily resolve them
    # But wait, we want haptics.
    # Let's just remove the explicit haptic param added to ProfileScreen parameter list
    text = text.replace("    onNavigateToSupport: () -> Unit = {\n}", "    onNavigateToSupport: () -> Unit = {}")
    text = text.replace("    onNavigateToSupport: () -> Unit = {\n    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current\n}", "    onNavigateToSupport: () -> Unit = {}")
    
    # We can just remove the `haptic.performHapticFeedback(...)` from all `onClick = { ... }` and `clickable { ... }` to fix the build quickly. 
    # Actually, we were asked to add haptic feedback.
    # What if we just fix the `val haptic` declaration to be securely inside the @Composable function body.
    
    # First, let's remove any stray `val haptic = ...`
    text = text.replace("    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current\n", "")
    text = text.replace("    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current", "")
    
    # Second, let's define a Composable wrapper or just inject it at the top of the function body.
    # For each @Composable function that has `haptic.perform...`, we must add `val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current`.
    # Let's just do a regex replace to insert it right after `{` if the block contains `haptic.`
    
    # Since doing this with python regex might be tricky, let's just do it manually for the files.
    # Actually, the simplest way is to replace `clickable { haptic.performHapticFeedback...` with `clickable {`
    # and `onClick = { haptic.performHapticFeedback...` with `onClick = {`
    # Wait, the user specifically asked for haptics. I should keep them!
    
with open("fix_haptic_final.py", "w") as f:
    f.write("")
