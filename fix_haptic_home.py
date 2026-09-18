import re
with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    text = f.read()

# Insert val haptic
if "val haptic =" not in text:
    text = text.replace("    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current", "    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current\n    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current")
    
# Replace clickable {
text = text.replace("clickable {", "clickable { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove);")
text = text.replace("onClick = {", "onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove);")

# Add subtle idle animation to GameBannerCard
text = text.replace("fun GameBannerCard() {", "fun GameBannerCard() {\n    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()\n    val floatOffset by infiniteTransition.animateFloat(\n        initialValue = -5f,\n        targetValue = 5f,\n        animationSpec = androidx.compose.animation.core.infiniteRepeatable(\n            animation = androidx.compose.animation.core.tween(2000, easing = androidx.compose.animation.core.InOutSine),\n            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse\n        )\n    )")
text = text.replace("modifier = Modifier\n            .fillMaxWidth()", "modifier = Modifier\n            .fillMaxWidth()\n            .offset(y = floatOffset.dp)")

# We need to add haptic to TournamentRow as well, but wait, haptic might not be defined inside TournamentRow.
text = text.replace("fun TournamentRow(tournament: Tournament, onClick: () -> Unit) {", "fun TournamentRow(tournament: Tournament, onClick: () -> Unit) {\n    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current")

# Do the same for GameBannerCard
text = text.replace("fun GameBannerCard() {", "fun GameBannerCard() {\n    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current")

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(text)
print("Added haptics to HomeScreen")
