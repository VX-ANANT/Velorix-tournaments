import re

with open("app/src/main/java/com/example/service/MyFirebaseMessagingService.kt", "r") as f:
    text = f.read()

text = text.replace("import kotlinx.coroutines.launch\npackage com.example.service", "package com.example.service\nimport kotlinx.coroutines.launch\n")
text = text.replace("com.example.data.local.AppDatabase", "com.example.data.db.AppDatabase")

with open("app/src/main/java/com/example/service/MyFirebaseMessagingService.kt", "w") as f:
    f.write(text)

