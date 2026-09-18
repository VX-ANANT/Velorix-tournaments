import re
with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

# Replace Scaffold background to Transparent
text = text.replace("containerColor = MaterialTheme.colorScheme.background,", "containerColor = Color.Transparent,")

# Add a Box around NavHost with gradient
if 'NavHost(' in text and 'modifier = Modifier.fillMaxSize()' in text:
    old_nav = "                    NavHost(\n                        navController = navController,\n                        startDestination = \"splash\",\n                        modifier = Modifier.fillMaxSize()\n                    )"
    new_nav = "                    Box(\n                        modifier = Modifier.fillMaxSize().background(\n                            androidx.compose.ui.graphics.Brush.verticalGradient(\n                                colors = listOf(\n                                    Color(0xFF0F172A),\n                                    Color(0xFF09090B)\n                                )\n                            )\n                        )\n                    ) {\n                    NavHost(\n                        navController = navController,\n                        startDestination = \"splash\",\n                        modifier = Modifier.fillMaxSize()\n                    )"
    text = text.replace(old_nav, new_nav)
    # The box is closed later, I need to close the Box where NavHost ends.
    # Actually, it's easier to just wrap the whole `Box(modifier = Modifier.fillMaxSize().background...` in the outermost Surface.
