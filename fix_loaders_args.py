import re
with open("app/src/main/java/com/example/ui/components/GeometricLoaders.kt", "r") as f:
    text = f.read()

target = """@Composable
fun AnimatedLoaders(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        CircleLoader()
        Spacer(Modifier.width(16.dp))
        TriangleLoader()
        Spacer(Modifier.width(16.dp))
        RectLoader()
    }
}"""

replacement = """@Composable
fun AnimatedLoaders(
    modifier: Modifier = Modifier, 
    pathColor: Color = Color(0xFF2F3545), 
    dotColor: Color = Color(0xFF5628EE)
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        CircleLoader(pathColor = pathColor, dotColor = dotColor)
        Spacer(Modifier.width(16.dp))
        TriangleLoader(pathColor = pathColor, dotColor = dotColor)
        Spacer(Modifier.width(16.dp))
        RectLoader(pathColor = pathColor, dotColor = dotColor)
    }
}"""

text = text.replace(target, replacement)
with open("app/src/main/java/com/example/ui/components/GeometricLoaders.kt", "w") as f:
    f.write(text)
