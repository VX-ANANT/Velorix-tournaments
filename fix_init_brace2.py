with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "            })" in line:
        # Check lines after it
        if i+1 < len(lines) and "        }" in lines[i+1]:
            if i+2 < len(lines) and "    }" in lines[i+2]:
                print("Removing brace at index", i+2)
                lines[i+2] = ""
        break

with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
    f.writelines(lines)
