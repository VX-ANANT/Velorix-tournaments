with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

text = text.replace(
    "color = MaterialTheme.colorScheme.surfaceVariant,",
    "color = Color(0xFF1E293B).copy(alpha = 0.8f),"
).replace(
    "tonalElevation = 4.dp",
    "tonalElevation = 0.dp,\n                                    border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))"
)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)
