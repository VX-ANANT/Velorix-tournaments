with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

# I want to add haptic in GlassBottomBar for tab switching
if "haptic.performHapticFeedback" not in text:
    text = text.replace("    onTabSelected: (String) -> Unit,", "    onTabSelected: (String) -> Unit,\n    hazeState: HazeState\n) {\n    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current")
    text = text.replace("    hazeState: HazeState\n) {", "")
    text = text.replace("clickable {", "clickable { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove);")
    text = text.replace("onClick = {", "onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove);")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)
