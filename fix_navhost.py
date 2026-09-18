import re
with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

target = """                    androidx.navigation.compose.NavHost(navController = navController, startDestination = "splash") {"""

replacement = """                    androidx.navigation.compose.NavHost(
                        navController = navController, 
                        startDestination = "splash",
                        enterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.scaleIn(initialScale = 0.98f) },
                        exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.scaleOut(targetScale = 1.02f) },
                        popEnterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.scaleIn(initialScale = 1.02f) },
                        popExitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.scaleOut(targetScale = 0.98f) }
                    ) {"""

text = text.replace(target, replacement)
with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)
