import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

# Add imports
imports = """import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
"""

text = text.replace("import com.example.ui.theme.CyberpunkYellow", imports + "import com.example.ui.theme.CyberpunkYellow")

# Fix usages
text = text.replace("val hazeState = remember { dev.chrisbanes.haze.HazeState() }", "val hazeState = remember { HazeState() }")
text = text.replace(".dev.chrisbanes.haze.haze(", ".haze(")
text = text.replace("style = dev.chrisbanes.haze.HazeStyle(", "style = HazeStyle(")
text = text.replace("hazeState: dev.chrisbanes.haze.HazeState", "hazeState: HazeState")
text = text.replace(".dev.chrisbanes.haze.hazeChild(", ".hazeChild(")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)

