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

    # Clean out all instances
    text = text.replace("    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current", "")
    text = text.replace("    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current}", "}")

    # Now carefully inject `val haptic` right after the main function signature `{\n`
    # We can regex match `fun [A-Za-z0-9_]+\([^)]*\)\s*\{`
    # But Kotlin functions can have parameters across lines, so `[^)]*` will match newlines.
    
    def repl(m):
        return m.group(0) + "\n    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current\n"
        
    # Match `@Composable\nfun <Name>(<args>) {`
    # We will search for `@Composable` and then the next `{` that ends the function signature.
    # Actually, simpler: search for `) {\n` and replace it, but what if there are lambdas?
    # Better: Find all `@Composable\nfun ... {` manually.
    
    parts = re.split(r'(@Composable\s*(?:@OptIn[^\n]*\n)?\s*fun\s+[A-Za-z0-9_]+\s*\([^)]*\)\s*(?::\s*[A-Za-z0-9_]+\s*)?\{)', text)
    
    # parts[0] is before first match
    # parts[1] is first match
    # parts[2] is after first match...
    
    new_text = parts[0]
    for i in range(1, len(parts), 2):
        sig = parts[i]
        body = parts[i+1]
        
        # Inject haptic
        new_text += sig + "\n    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current\n" + body
        
    with open(file_path, "w") as f:
        f.write(new_text)

print("Cleaned up and reinjected haptic v2")
