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
    
    if "import androidx.compose.animation.core.*" not in text:
        text = text.replace("import androidx.compose.runtime.Composable", "import androidx.compose.runtime.Composable\nimport androidx.compose.animation.core.*\nimport androidx.compose.runtime.getValue")
        
    # for MainActivity it uses runtime.* so maybe it doesn't have runtime.Composable explicit
    if "import androidx.compose.animation.core.*" not in text:
        text = text.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\nimport androidx.compose.animation.core.*\nimport androidx.compose.runtime.getValue")
        
    with open(file, "w") as f:
        f.write(text)

