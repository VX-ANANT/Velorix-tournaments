import re

with open("app/src/main/java/com/example/ui/components/TournamentCard.kt", "r") as f:
    text = f.read()

# Update Card styling
text = text.replace("Color(0xFF18181B).copy(alpha=0.8f)", "MaterialTheme.colorScheme.surfaceVariant")
text = text.replace("Color(0xFF3F3F46).copy(alpha=0.5f)", "MaterialTheme.colorScheme.outline")
text = text.replace("RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)", "RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)")

# Update Progress bar colors
text = text.replace("PinkishRedAccent", "MaterialTheme.colorScheme.secondary")
text = text.replace("Color(0xFF27272A)", "MaterialTheme.colorScheme.surface")

# Also the text fields and icons
text = text.replace("Color(0xFFF4F4F5)", "MaterialTheme.colorScheme.onSurface")
text = text.replace("Color(0xFFA1A1AA)", "MaterialTheme.colorScheme.onSurfaceVariant")

with open("app/src/main/java/com/example/ui/components/TournamentCard.kt", "w") as f:
    f.write(text)

