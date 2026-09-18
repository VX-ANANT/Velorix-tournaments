with open("app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt", "r") as f:
    text = f.read()

import re

imports = """import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Remove"""

text = text.replace("import androidx.compose.material.icons.filled.Person", "import androidx.compose.material.icons.filled.Person\n" + imports)

target_row = """                // Rank label
                Text(
                    text = "#${player.rank}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.width(36.dp)
                )"""

replacement_row = """                // Rank label
                Text(
                    text = "#${player.rank}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.width(36.dp)
                )
                
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
                    modifier = Modifier.size(16.dp).padding(end = 4.dp)
                )"""

text = text.replace(target_row, replacement_row)

with open("app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt", "w") as f:
    f.write(text)
