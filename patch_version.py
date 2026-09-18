import re
with open("app/build.gradle.kts", "r") as f:
    text = f.read()

text = re.sub(r'versionName = ".*?"', 'versionName = "Dev-0.0.2-Release-global"', text)

with open("app/build.gradle.kts", "w") as f:
    f.write(text)
