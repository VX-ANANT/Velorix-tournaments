with open("app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt", "r") as f:
    text = f.read()

target = """        Text(
            text = player.username,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(90.dp)
        )"""

replacement = """        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            val trend = kotlin.math.abs(player.username.hashCode() % 3)
            val trendIcon = when (trend) {
                0 -> androidx.compose.material.icons.Icons.Filled.ArrowUpward
                1 -> androidx.compose.material.icons.Icons.Filled.ArrowDownward
                else -> androidx.compose.material.icons.Icons.Filled.Remove
            }
            val trendColor = when (trend) {
                0 -> androidx.compose.ui.graphics.Color(0xFF10B981)
                1 -> androidx.compose.ui.graphics.Color(0xFFEF4444)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Icon(
                imageVector = trendIcon,
                contentDescription = "Trend",
                tint = trendColor,
                modifier = Modifier.size(12.dp).padding(end = 2.dp)
            )
            Text(
                text = player.username,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(75.dp)
            )
        }"""

text = text.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt", "w") as f:
    f.write(text)
