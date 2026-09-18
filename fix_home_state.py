with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    text = f.read()

text = text.replace("    val liveUpdatesMap by viewModel.liveMatchUpdates.collectAsState()", "")
text = text.replace("val isRefreshing by viewModel.isRefreshingHome.collectAsState()", "val isRefreshing by viewModel.isRefreshingHome.collectAsState()")

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(text)
