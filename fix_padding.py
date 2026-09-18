import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

# Replace padding
old_padding = """                                    .padding(
                                        top = innerPadding.calculateTopPadding(),
                                        bottom = innerPadding.calculateBottomPadding()
                                    )"""
new_padding = """                                    .padding(
                                        top = innerPadding.calculateTopPadding()
                                        // bottom padding removed to allow content to scroll behind the glass bottom bar
                                    )"""

text = text.replace(old_padding, new_padding)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)

