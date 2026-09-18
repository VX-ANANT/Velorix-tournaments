with open("app/src/main/java/com/example/ui/screens/SplashScreen.kt", "r") as f:
    text = f.read()

target = """            // Glowing concentric circles around the core logo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(280.dp)
            ) {"""

replacement = """            // Glowing concentric circles around the core logo
            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "splash")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "pulseScale"
            )
            
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(280.dp).androidx.compose.ui.draw.scale(pulseScale)
            ) {"""

text = text.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/SplashScreen.kt", "w") as f:
    f.write(text)
