with open("app/src/main/java/com/example/ui/components/TournamentCard.kt", "r") as f:
    text = f.read()

card_old = """        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clickable { onClick() }
                .testTag("tournament_card_${title.replace(" ", "_").lowercase()}"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardSurfaceLight
            ),
            border = BorderStroke(1.dp, Color(0xFF3F3F46).copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        )"""

card_new = """        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clickable { onClick() }
                .testTag("tournament_card_${title.replace(" ", "_").lowercase()}"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF18181B).copy(alpha = 0.7f)
            ),
            border = BorderStroke(1.dp, Color(0xFF27272A)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        )"""

text = text.replace(card_old, card_new)

img_old = """                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = "Game Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )"""

img_new = """                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = "Game Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xFF18181B).copy(alpha = 0.9f)
                                        ),
                                        startY = 100f
                                    )
                                )
                        )"""
text = text.replace(img_old, img_new)

with open("app/src/main/java/com/example/ui/components/TournamentCard.kt", "w") as f:
    f.write(text)
