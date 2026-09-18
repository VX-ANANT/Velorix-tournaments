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
    
    text = text.replace("infiniteRepeatable(", "infiniteRepeatable<Float>(")
    text = text.replace("tween(", "tween<Float>(")
    
    with open(file, "w") as f:
        f.write(text)

