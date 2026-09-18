with open("app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt", "r") as f:
    text = f.read()

target = """        // Podium Section
        item {
            Card("""

replacement = """        if (players.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "NO RANKINGS AVAILABLE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Leaderboard updates automatically based on recent match results.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        } else {
        // Podium Section
        item {
            Card("""

text = text.replace(target, replacement)

target2 = """            // Rest of the Ranking List
            items(restOfPlayers) { player ->
                LeaderboardRow(player = player)
            }
        }"""

replacement2 = """            // Rest of the Ranking List
            items(restOfPlayers) { player ->
                LeaderboardRow(player = player)
            }
        } // End of else block
        }"""

text = text.replace(target2, replacement2)

with open("app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt", "w") as f:
    f.write(text)
