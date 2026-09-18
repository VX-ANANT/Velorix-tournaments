with open("app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt", "r") as f:
    text = f.read()

target = """    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {"""

replacement = """    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "podium")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(2000 + (player.rank * 200), easing = androidx.compose.animation.core.EaseInOutSine),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "float"
    )

    Column(
        modifier = modifier.offset(y = floatOffset.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {"""

text = text.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt", "w") as f:
    f.write(text)
