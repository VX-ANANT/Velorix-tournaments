with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

target = """                            // Modern Background Glowing Orbs
                            Box(modifier = Modifier.offset(x = (-100).dp, y = (-100).dp).size(400.dp).background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), Color.Transparent))))
                            Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 100.dp, y = 100.dp).size(400.dp).background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f), Color.Transparent))))
                            Box(modifier = Modifier.align(Alignment.CenterEnd).offset(x = 50.dp, y = (-200).dp).size(300.dp).background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), Color.Transparent))))"""

replacement = """                            // Modern Background Glowing Orbs
                            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "orbs")
                            val scale1 by infiniteTransition.animateFloat(
                                initialValue = 0.85f,
                                targetValue = 1.15f,
                                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                    animation = androidx.compose.animation.core.tween(3500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                                ),
                                label = "scale1"
                            )
                            val scale2 by infiniteTransition.animateFloat(
                                initialValue = 1.15f,
                                targetValue = 0.85f,
                                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                    animation = androidx.compose.animation.core.tween(4000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                                ),
                                label = "scale2"
                            )
                            val scale3 by infiniteTransition.animateFloat(
                                initialValue = 0.9f,
                                targetValue = 1.2f,
                                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                    animation = androidx.compose.animation.core.tween(4500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                                ),
                                label = "scale3"
                            )

                            Box(modifier = Modifier.offset(x = (-100).dp, y = (-100).dp).size(400.dp).androidx.compose.ui.draw.scale(scale1).background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), Color.Transparent))))
                            Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 100.dp, y = 100.dp).size(400.dp).androidx.compose.ui.draw.scale(scale2).background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f), Color.Transparent))))
                            Box(modifier = Modifier.align(Alignment.CenterEnd).offset(x = 50.dp, y = (-200).dp).size(300.dp).androidx.compose.ui.draw.scale(scale3).background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), Color.Transparent))))"""

text = text.replace(target, replacement)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)
