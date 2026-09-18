import re
with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

target = """                        enterTransition = { androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing), initialOffsetX = { it }) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(400)) },
                        exitTransition = { androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing), targetOffsetX = { -it / 3 }) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(400)) },
                        popEnterTransition = { androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing), initialOffsetX = { -it / 3 }) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(400)) },
                        popExitTransition = { androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing), targetOffsetX = { it }) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(400)) }"""

replacement = """                        enterTransition = { androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 400f), initialOffsetX = { it }) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) },
                        exitTransition = { androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 400f), targetOffsetX = { -it / 3 }) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)) },
                        popEnterTransition = { androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 400f), initialOffsetX = { -it / 3 }) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) },
                        popExitTransition = { androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 400f), targetOffsetX = { it }) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)) }"""

text = text.replace(target, replacement)
with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)
print("Transitions updated")
