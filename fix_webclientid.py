import re
with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    text = f.read()

text = text.replace('val webClientId = "931215440748-e87a2m5b09j81c8b350l9g1p7345607k.apps.googleusercontent.com"', 'val webClientId = "27931798964-h6fiau2df3i6e3049fontnctpsh06763.apps.googleusercontent.com"')

with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
    f.write(text)
