import re
with open("app/src/main/java/com/example/ui/components/TournamentCard.kt", "r") as f:
    text = f.read()

text = text.replace("RoundedCornerShape(8.dp)", "RoundedCornerShape(50.dp)")

with open("app/src/main/java/com/example/ui/components/TournamentCard.kt", "w") as f:
    f.write(text)
