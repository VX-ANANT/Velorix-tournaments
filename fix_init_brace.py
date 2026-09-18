with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "remoteConfig.addOnConfigUpdateListener" in line:
        start_idx = i
        break

for i in range(start_idx, len(lines)):
    if "    }" in line and "viewModelScope.launch {" in lines[i+2] if i+2 < len(lines) else False:
        lines[i] = ""
        break

with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
    f.writelines(lines)
