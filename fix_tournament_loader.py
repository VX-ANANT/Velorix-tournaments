import re
with open("app/src/main/java/com/example/ui/screens/TournamentDetailsScreen.kt", "r") as f:
    text = f.read()

target = """        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }"""

replacement = """        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                com.example.ui.components.AnimatedLoaders(
                    pathColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    dotColor = MaterialTheme.colorScheme.primary
                )
            }
        }"""

text = text.replace(target, replacement)
with open("app/src/main/java/com/example/ui/screens/TournamentDetailsScreen.kt", "w") as f:
    f.write(text)
