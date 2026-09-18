with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

text = text.replace("androidx.navigation.compose.composable(\"main\")", "composable(\"main\")")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)

