import os

files_to_fix = [
    "app/src/main/java/com/example/MainActivity.kt",
    "app/src/main/java/com/example/ui/components/VeloRixButton.kt",
    "app/src/main/java/com/example/ui/screens/HomeScreen.kt",
    "app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt",
    "app/src/main/java/com/example/ui/screens/SplashScreen.kt",
    "app/src/main/java/com/example/ui/components/TournamentCard.kt"
]

for file in files_to_fix:
    with open(file, "r") as f:
        text = f.read()
    
    if "import androidx.compose.animation.core.animateFloat" not in text:
        text = text.replace("import androidx.compose.runtime.Composable", "import androidx.compose.runtime.Composable\nimport androidx.compose.animation.core.animateFloat")
    if "import androidx.compose.ui.draw.scale" not in text and "scale" in text:
        text = text.replace("import androidx.compose.runtime.Composable", "import androidx.compose.runtime.Composable\nimport androidx.compose.ui.draw.scale")

    text = text.replace("androidx.compose.ui.draw.scale", "scale")
    
    with open(file, "w") as f:
        f.write(text)

