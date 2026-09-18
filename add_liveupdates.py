with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    text = f.read()

text = text.replace("    val tournaments by viewModel.tournaments.collectAsState()", "    val tournaments by viewModel.tournaments.collectAsState()\n    val liveUpdatesMap by viewModel.liveMatchUpdates.collectAsState()")

# Remove the duplicate isRefreshing definition on line 121
text = text.replace("val isRefreshing by viewModel.isRefreshingHome.collectAsState()\n            PullToRefreshBox(", "PullToRefreshBox(")

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(text)
