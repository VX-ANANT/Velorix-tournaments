import re

for screen in ["app/src/main/java/com/example/ui/screens/HomeScreen.kt", "app/src/main/java/com/example/ui/screens/MatchesScreen.kt"]:
    with open(screen, "r") as f:
        text = f.read()

    # Make sure we declare liveUpdatesMap in MatchesScreen
    if "MatchesScreen" in text and "val liveUpdatesMap" not in text:
        text = text.replace(
            "val joinedMatches = tournaments.filter { it.joined }",
            "val joinedMatches = tournaments.filter { it.joined }\n    val liveUpdatesMap by viewModel.liveMatchUpdates.collectAsState()"
        )

    # In TournamentCard replace:
    # filledSlots = match.filledSlots,
    # maxSlots = match.maxSlots,
    text = text.replace(
        "maxSlots = match.maxSlots,",
        "maxSlots = match.maxSlots,\n                        liveUpdate = liveUpdatesMap[match.id],"
    )

    with open(screen, "w") as f:
        f.write(text)
