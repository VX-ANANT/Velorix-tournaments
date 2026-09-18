import os

files = [
    "app/src/main/java/com/example/ui/screens/MatchesScreen.kt",
    "app/src/main/java/com/example/ui/screens/TournamentDetailsScreen.kt",
]

for file in files:
    with open(file, "r") as f:
        text = f.read()
    
    text = text.replace("Color(0xFF18181B)", "MaterialTheme.colorScheme.surface")
    text = text.replace("Color(0xFF27272A)", "MaterialTheme.colorScheme.surfaceVariant")
    
    with open(file, "w") as f:
        f.write(text)
