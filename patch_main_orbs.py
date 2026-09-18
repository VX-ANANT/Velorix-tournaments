with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

text = text.replace("Color(0xFFA855F7)", "MaterialTheme.colorScheme.primary")
text = text.replace("Color(0xFFD946EF)", "MaterialTheme.colorScheme.secondary")
text = text.replace("Color(0xFFF43F5E)", "MaterialTheme.colorScheme.tertiary")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)
