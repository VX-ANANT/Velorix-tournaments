with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    text = f.read()

text = text.replace(
"""                        isJoined = match.joined,
                        liveUpdate = liveUpdatesMap[match.id],
                        onClick""",
"""                        isJoined = match.joined,
                        onClick"""
)

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(text)
