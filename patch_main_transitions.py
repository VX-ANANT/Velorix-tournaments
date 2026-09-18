import re
with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

target = """                        enterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.scaleIn(initialScale = 0.98f) },
                        exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.scaleOut(targetScale = 1.02f) },
                        popEnterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.scaleIn(initialScale = 1.02f) },
                        popExitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.scaleOut(targetScale = 0.98f) }"""

replacement = """                        enterTransition = { androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing), initialOffsetX = { it }) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(400)) },
                        exitTransition = { androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing), targetOffsetX = { -it / 3 }) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(400)) },
                        popEnterTransition = { androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing), initialOffsetX = { -it / 3 }) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(400)) },
                        popExitTransition = { androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing), targetOffsetX = { it }) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(400)) }"""

if target in text:
    text = text.replace(target, replacement)
    with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
        f.write(text)
    print("Main transitions patched")
else:
    print("Transitions not found")
