import re

with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "r") as f:
    text = f.read()

text = text.replace("collectAsState()", "collectAsStateWithLifecycle()")
text = text.replace("collectAsState(initial = emptyList())", "collectAsStateWithLifecycle(initialValue = emptyList())")

imports = """
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.collectAsState
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
"""
if "import com.patrykandpatrick.vico.core.cartesian.data.lineSeries" not in text:
    text = text.replace("import androidx.lifecycle.compose.collectAsStateWithLifecycle", imports)
    
text = text.replace("com.patrykandpatrick.vico.core.cartesian.data.lineSeries {", "lineSeries {")

with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "w") as f:
    f.write(text)

