with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

# Replace Surface color
text = text.replace(
    "                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {\n                    Box(modifier = Modifier.fillMaxSize()) {",
    "                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {\n                    Box(modifier = Modifier.fillMaxSize().background(\n                            androidx.compose.ui.graphics.Brush.verticalGradient(\n                                colors = listOf(\n                                    Color(0xFF0F172A),\n                                    Color(0xFF020617)\n                                )\n                            )\n                        )) {"
)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)
