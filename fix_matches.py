with open("app/src/main/java/com/example/ui/screens/MatchesScreen.kt", "r") as f:
    text = f.read()

# We need to add UpcomingJoinedRow and CompletedRow if they are missing or broken.
if "fun UpcomingJoinedRow" not in text:
    # Append the missing code to the end of the file
    pass # Wait, let's just write the whole thing.
