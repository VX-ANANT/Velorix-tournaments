with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    text = f.read()

# find first "import androidx.compose.material3.MaterialTheme"
idx = text.find("import androidx.compose.material3.MaterialTheme")
if idx != -1:
    text = "package com.example.ui.screens\n\n" + text[idx:]
else:
    print("Not found!")

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(text)

