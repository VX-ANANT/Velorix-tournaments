import re
with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "r") as f:
    text = f.read()

target_logo = """            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {"""
replacement_logo = """            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
            val floatOffset by androidx.compose.animation.core.animateFloat(
                initialValue = -5f,
                targetValue = 5f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(2000, easing = androidx.compose.animation.core.InOutSine),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                )
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .offset(y = floatOffset.dp)
            ) {"""
# Ah, infiniteTransition.animateFloat is correct

replacement_logo = """            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
            val floatOffset by infiniteTransition.animateFloat(
                initialValue = -5f,
                targetValue = 5f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(2000, easing = androidx.compose.animation.core.InOutSine),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "auth_bounce"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .offset(y = floatOffset.dp)
            ) {"""

if "infiniteTransition.animateFloat" not in text:
    text = text.replace(target_logo, replacement_logo)

with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "w") as f:
    f.write(text)
