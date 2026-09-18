with open("app/src/main/java/com/example/ui/screens/MatchesScreen.kt", "r") as f:
    text = f.read()

# Let's count { and } up to before `@Composable\nfun UpcomingJoinedRow`
idx = text.find("@Composable\nfun UpcomingJoinedRow")
sub = text[:idx]
open_c = sub.count("{")
close_c = sub.count("}")

print(f"Open: {open_c}, Close: {close_c}, Diff: {open_c - close_c}")
