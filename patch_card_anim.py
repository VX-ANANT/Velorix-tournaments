with open("app/src/main/java/com/example/ui/components/TournamentCard.kt", "r") as f:
    text = f.read()

target = """                            val buttonScale by animateFloatAsState(
                                targetValue = if (isPressed) 0.95f else 1f,
                                animationSpec = tween(durationMillis = 150)
                            )"""

replacement = """                            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "btn")
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.02f,
                                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                    animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                                ),
                                label = "btnPulse"
                            )
                            val buttonScale by animateFloatAsState(
                                targetValue = if (isPressed) 0.95f else (if (!isFull) pulseScale else 1f),
                                animationSpec = tween(durationMillis = 150)
                            )"""

text = text.replace(target, replacement)

with open("app/src/main/java/com/example/ui/components/TournamentCard.kt", "w") as f:
    f.write(text)
