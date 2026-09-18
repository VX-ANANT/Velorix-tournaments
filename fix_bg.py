with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

start_idx = text.find("// Global Background")
if start_idx != -1:
    end_idx = text.find("Scaffold(", start_idx)
    if end_idx != -1:
        text = text[:start_idx] + "// Global Background\n                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {\n                    " + text[end_idx:]

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)

