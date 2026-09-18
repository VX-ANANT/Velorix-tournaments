with open("app/src/main/java/com/example/ui/components/TournamentCard.kt", "r") as f:
    text = f.read()

text = text.replace("import androidx.compose.foundation.shape.RoundedCornerShape(6.dp)", "")
text = text.replace("val MaterialTheme.colorScheme.secondary = Color(0xFFC4B5FD)", "val PinkishRedAccent = Color(0xFFC4B5FD)")

with open("app/src/main/java/com/example/ui/components/TournamentCard.kt", "w") as f:
    f.write(text)

