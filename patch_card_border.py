with open("app/src/main/java/com/example/ui/components/TournamentCard.kt", "r") as f:
    text = f.read()

text = text.replace("        colors = CardDefaults.cardColors(containerColor = CardSurfaceLight),\n        shape = RoundedCornerShape(20.dp),\n        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)",
"""        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B).copy(alpha=0.8f)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF3F3F46).copy(alpha=0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)""")

with open("app/src/main/java/com/example/ui/components/TournamentCard.kt", "w") as f:
    f.write(text)
