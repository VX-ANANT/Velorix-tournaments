import re

with open("app/src/main/java/com/example/ui/components/TournamentCard.kt", "r") as f:
    text = f.read()

# Remove the `.shadow(...)` from TournamentCard.kt
# It spans multiple lines, so regex with DOTALL is best.
text = re.sub(r'\.shadow\(\s*elevation = if \(!isFull\) 16\.dp else 0\.dp,\s*shape = RoundedCornerShape\(6\.dp\),\s*spotColor = MaterialTheme\.colorScheme\.secondary,\s*ambientColor = MaterialTheme\.colorScheme\.secondary\s*\)', '', text, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/components/TournamentCard.kt", "w") as f:
    f.write(text)

