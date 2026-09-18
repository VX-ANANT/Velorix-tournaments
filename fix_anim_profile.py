import re
with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "r") as f:
    text = f.read()

target = "        Image("
replacement = """        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.98f,
            targetValue = 1.02f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(2000, easing = androidx.compose.animation.core.InOutSine),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            )
        )
        Image(
            modifier = Modifier.scale(scale),"""

if "val infiniteTransition" not in text:
    text = text.replace(target, replacement, 1) # Only first Image (avatar)

with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "w") as f:
    f.write(text)
print("Added anim to profile")
