import os

files = [
    "app/src/main/java/com/example/MainActivity.kt",
    "app/src/main/java/com/example/ui/screens/SplashScreen.kt",
    "app/src/main/java/com/example/ui/screens/HomeScreen.kt",
    "app/src/main/java/com/example/ui/components/TournamentCard.kt",
    "app/src/main/java/com/example/ui/components/VeloRixButton.kt",
    "app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt"
]

for file in files:
    with open(file, "r") as f:
        text = f.read()
    
    # We will just use Modifier.graphicsLayer() without importing, actually Modifier.graphicsLayer() requires import if used as an extension but it is in androidx.compose.ui.graphics.graphicsLayer
    if "import androidx.compose.ui.graphics.graphicsLayer" not in text:
        text = text.replace("import androidx.compose.animation.core.animateFloat", "import androidx.compose.animation.core.animateFloat\nimport androidx.compose.ui.graphics.graphicsLayer")

    with open(file, "w") as f:
        f.write(text)

