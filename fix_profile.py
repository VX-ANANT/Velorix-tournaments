import re

with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "r") as f:
    text = f.read()

state_injections = """
    val missions by viewModel.missions.collectAsState()
    val showConfetti by viewModel.showConfetti.collectAsState()
    val matchStats by viewModel.matchStats.collectAsState(initial = emptyList())
"""
if "val missions by viewModel" not in text:
    text = text.replace("val user by viewModel.userState.collectAsStateWithLifecycle()", "val user by viewModel.userState.collectAsStateWithLifecycle()\n" + state_injections)

with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "w") as f:
    f.write(text)

