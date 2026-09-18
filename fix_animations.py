import re

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    text = f.read()

# Add animateContentSize to TournamentCard
text = text.replace(
    "colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),",
    "colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),\n        modifier = modifier.animateContentSize(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)),"
)
text = text.replace("import androidx.compose.animation.core.tween", "import androidx.compose.animation.core.tween\nimport androidx.compose.animation.animateContentSize")

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(text)

