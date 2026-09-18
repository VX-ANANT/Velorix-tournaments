import re

with open("app/src/main/java/com/example/ui/components/TournamentCard.kt", "r") as f:
    text = f.read()

# Replace rounded shapes
text = text.replace("RoundedCornerShape(24.dp)", "RoundedCornerShape(8.dp)")
text = text.replace("RoundedCornerShape(16.dp)", "RoundedCornerShape(8.dp)")
text = text.replace("RoundedCornerShape(12.dp)", "RoundedCornerShape(8.dp)")
text = text.replace("CircleShape", "RoundedCornerShape(6.dp)")

# Replace PinkishRedAccent
text = text.replace("PinkishRedAccent", "MaterialTheme.colorScheme.secondary")

# Remove infinite transition for pulse
# Replace targetValue = if (isPressed) 0.95f else (if (!isFull) pulseScale else 1f)
# with targetValue = if (isPressed) 0.98f else 1f

text = re.sub(r'val infiniteTransition.*?label = "btnPulse"\n\s*\)', '', text, flags=re.DOTALL)
text = text.replace("val pulseScale by infiniteTransition.animateFloat(", "")
text = text.replace("targetValue = if (isPressed) 0.95f else (if (!isFull) pulseScale else 1f),", "targetValue = if (isPressed) 0.98f else 1f,")

with open("app/src/main/java/com/example/ui/components/TournamentCard.kt", "w") as f:
    f.write(text)
