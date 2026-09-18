with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

old_orbs = """                            // Modern Background Glowing Orbs
                            Box(modifier = Modifier.offset(x = (-100).dp, y = (-100).dp).size(300.dp).blur(120.dp).background(Color(0xFFC4B5FD).copy(alpha = 0.25f), androidx.compose.foundation.shape.CircleShape))
                            Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 100.dp, y = 100.dp).size(300.dp).blur(120.dp).background(Color(0xFFF0ABFC).copy(alpha = 0.25f), androidx.compose.foundation.shape.CircleShape))
                            Box(modifier = Modifier.align(Alignment.CenterEnd).offset(x = 50.dp, y = (-200).dp).size(200.dp).blur(100.dp).background(Color(0xFFF43F5E).copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape))"""

new_orbs = """                            // Modern Background Glowing Orbs
                            Box(modifier = Modifier.offset(x = (-100).dp, y = (-100).dp).size(400.dp).background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color(0xFFA855F7).copy(alpha = 0.25f), Color.Transparent))))
                            Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 100.dp, y = 100.dp).size(400.dp).background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color(0xFFD946EF).copy(alpha = 0.25f), Color.Transparent))))
                            Box(modifier = Modifier.align(Alignment.CenterEnd).offset(x = 50.dp, y = (-200).dp).size(300.dp).background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color(0xFFF43F5E).copy(alpha = 0.15f), Color.Transparent))))"""

text = text.replace(old_orbs, new_orbs)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)
