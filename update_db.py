import re
with open("app/src/main/java/com/example/data/db/AppDatabase.kt", "r") as f:
    text = f.read()

text = text.replace("version = 12,", "version = 13,")

with open("app/src/main/java/com/example/data/db/AppDatabase.kt", "w") as f:
    f.write(text)
