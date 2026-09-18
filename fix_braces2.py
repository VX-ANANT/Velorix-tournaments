with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "        })" in line and "viewModelScope" in lines[i+2] if i+2 < len(lines) else False:
        lines[i] = ""
        break

with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
    f.writelines(lines)
