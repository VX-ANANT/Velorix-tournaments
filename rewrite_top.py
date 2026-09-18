with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if line.startswith(".ui.screensimport"):
        new_lines.append(line.replace(".ui.screensimport", "import"))
    else:
        new_lines.append(line)

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.writelines(new_lines)
