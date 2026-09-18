with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    text = f.read()

target = """                        // Dynamic themed avatar circle based on seed index
                        Box(
                            modifier = Modifier
                                .size(48.dp)"""

replacement = """                        // Dynamic themed avatar circle based on seed index
                        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "avatar")
                        val avatarScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.05f,
                            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                animation = androidx.compose.animation.core.tween(2000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                            ),
                            label = "avatarScale"
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .androidx.compose.ui.draw.scale(avatarScale)"""

text = text.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(text)
