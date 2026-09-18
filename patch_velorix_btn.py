with open("app/src/main/java/com/example/ui/components/VeloRixButton.kt", "r") as f:
    text = f.read()

target = """    // Compute glow depth
    val shadowElevation = if (isPressed) 4.dp else 10.dp

    Button("""

replacement = """    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "idlePulse"
    )

    // Compute glow depth
    val shadowElevation = if (isPressed) 4.dp else 10.dp
    val currentScale = if (isPressed) scale else (if (enabled) idlePulse else 1f)

    Button("""

target2 = """            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }"""

replacement2 = """            .graphicsLayer {
                scaleX = currentScale
                scaleY = currentScale
            }"""

text = text.replace(target, replacement)
text = text.replace(target2, replacement2)

with open("app/src/main/java/com/example/ui/components/VeloRixButton.kt", "w") as f:
    f.write(text)
