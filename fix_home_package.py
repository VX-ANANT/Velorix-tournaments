with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    text = f.read()

text = text.replace("package com.example.ui.screens.ui.screensimport", "package com.example.ui.screens\nimport")
# just in case
text = text.replace("package com.example.ui.screens.ui.screens", "package com.example.ui.screens")

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(text)
