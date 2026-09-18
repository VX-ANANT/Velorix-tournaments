import re
with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    text = f.read()

if "repository.initializeMissions()" not in text:
    target = "    init {\n"
    if target in text:
        text = text.replace(target, target + "        viewModelScope.launch {\n            repository.initializeMissions()\n        }\n")
    else:
        text = text.replace("    fun saveSearchQuery", "    init {\n        viewModelScope.launch {\n            repository.initializeMissions()\n        }\n    }\n\n    fun saveSearchQuery")
    
    with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
        f.write(text)

