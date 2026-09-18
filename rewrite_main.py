import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

# Replace the surface and glowing orbs with a simple surface.
# In MainActivity.kt around line 180:
# // Global Background
# Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) { ... }
# Then Scaffold inside.

pattern = r"// Global Background\s*Surface\(modifier = Modifier.fillMaxSize\(\), color = Color.Transparent\) \{.*?// Content\s*(Scaffold\()"
replacement = r"// Global Background\n                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {\n                    \1"

new_text = re.sub(pattern, replacement, text, flags=re.DOTALL)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(new_text)

