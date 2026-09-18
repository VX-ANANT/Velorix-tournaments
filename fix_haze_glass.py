import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

# 1. Update GlassBottomBar usage to pass hazeState
text = text.replace(
    """                            bottomBar = {
                                GlassBottomBar(
                                    currentTab = currentTabState,
                                    onTabSelected = { currentTabState = it }
                                )""",
    """                            val hazeState = remember { dev.chrisbanes.haze.HazeState() }
                            bottomBar = {
                                GlassBottomBar(
                                    currentTab = currentTabState,
                                    onTabSelected = { currentTabState = it },
                                    hazeState = hazeState
                                )"""
)

# 2. Add haze(hazeState) to the Box modifier
text = text.replace(
    """                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(""",
    """                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .dev.chrisbanes.haze.haze(
                                        state = hazeState,
                                        style = dev.chrisbanes.haze.HazeStyle(
                                            tint = Color.Black.copy(alpha = 0.2f),
                                            blurRadius = 30.dp,
                                            noiseFactor = 0f
                                        )
                                    )
                                    .padding("""
)

# 3. Update GlassBottomBar definition
text = text.replace(
    """fun GlassBottomBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {""",
    """fun GlassBottomBar(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState
) {"""
)

# 4. Add hazeChild(hazeState) to Pill
text = text.replace(
    """        // The Glass Pill
        Row(
            modifier = Modifier
                .weight(1f)
                .height(72.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xCC09090B), Color(0x8018181B)))) // Glass gradient
                .border(""",
    """        // The Glass Pill
        Row(
            modifier = Modifier
                .weight(1f)
                .height(72.dp)
                .clip(RoundedCornerShape(36.dp))
                .dev.chrisbanes.haze.hazeChild(
                    state = hazeState,
                    shape = RoundedCornerShape(36.dp)
                )
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0x8009090B), Color(0x4018181B)))) // Lighter gradient for blur
                .border("""
)

# 5. Add hazeChild(hazeState) to Profile Circle
text = text.replace(
    """        // The Separate Circle Button (Profile)
        val isProfileSelected = currentTab == "profile"
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xCC09090B), Color(0x8018181B))))
                .border(""",
    """        // The Separate Circle Button (Profile)
        val isProfileSelected = currentTab == "profile"
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .dev.chrisbanes.haze.hazeChild(
                    state = hazeState,
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0x8009090B), Color(0x4018181B))))
                .border("""
)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)

