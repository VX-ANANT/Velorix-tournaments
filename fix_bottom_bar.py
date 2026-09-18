with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

target = """    onTabSelected: (String) -> Unit,

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val pillTabs = listOf("""

replacement = """    onTabSelected: (String) -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val pillTabs = listOf("""

text = text.replace(target, replacement)

# Need to fix the extra brace at the end if we have one or if we are missing one.
# Looking at the previous output, we see a syntax error at line 595.
with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)
