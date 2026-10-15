import re
with open("app/build.gradle.kts", "r") as f:
    text = f.read()

text = re.sub(r'versionName = ".*?"', 'versionName = "V1-0.0-Dev"', text)

with open("app/build.gradle.kts", "w") as f:
    f.write(text)
