import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

start_str = "                            bottomBar = {\n"
end_str = "                            }\n                        ) { innerPadding ->\n"

start_idx = text.find(start_str)
end_idx = text.find(end_str, start_idx)

if start_idx != -1 and end_idx != -1:
    replacement = """                            bottomBar = {
                                GlassBottomBar(
                                    currentTab = currentTabState,
                                    onTabSelected = { currentTabState = it }
                                )
"""
    new_text = text[:start_idx] + replacement + text[end_idx:]
    
    # Add GlassBottomBar composable at the end of the file
    glass_bottom_bar_code = """

@Composable
fun GlassBottomBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    val pillTabs = listOf(
        Triple("home", "Home", androidx.compose.ui.graphics.vector.ImageVector.vectorResource(id = R.drawable.ic_iconsax_home)),
        Triple("matches", "Matches", androidx.compose.ui.graphics.vector.ImageVector.vectorResource(id = R.drawable.ic_iconsax_matches)),
        Triple("leaderboard", "Ranks", Icons.Rounded.Leaderboard),
        Triple("wallet", "Wallet", androidx.compose.ui.graphics.vector.ImageVector.vectorResource(id = R.drawable.ic_iconsax_wallet))
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The Glass Pill
        Row(
            modifier = Modifier
                .weight(1f)
                .height(72.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(Color(0x99000000)) // Translucent black
                .border(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(36.dp)
                )
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            pillTabs.forEach { (route, label, icon) ->
                val isSelected = currentTab == route
                
                val weight by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isSelected) 1.2f else 1f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    ), label = "tabWeight"
                )

                Box(
                    modifier = Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(36.dp))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(route) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Glow Effect Behind Icon
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isSelected,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) Color.White else Color.Gray.copy(alpha = 0.8f),
                            modifier = Modifier.size(if (isSelected) 26.dp else 24.dp)
                        )
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isSelected,
                            enter = androidx.compose.animation.expandVertically(expandFrom = Alignment.Top) + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically(shrinkTowards = Alignment.Top) + androidx.compose.animation.fadeOut()
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // The Separate Circle Button (Profile)
        val isProfileSelected = currentTab == "profile"
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color(0x99000000))
                .border(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
                    ),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = { onTabSelected("profile") }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Glow Effect
            androidx.compose.animation.AnimatedVisibility(
                visible = isProfileSelected,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
            Icon(
                imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(id = R.drawable.ic_iconsax_profile),
                contentDescription = "Profile",
                tint = if (isProfileSelected) Color.White else Color.Gray.copy(alpha = 0.8f),
                modifier = Modifier.size(if (isProfileSelected) 28.dp else 24.dp)
            )
        }
    }
}
"""
    new_text += glass_bottom_bar_code
    
    with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
        f.write(new_text)
    print("Updated MainActivity.kt")
else:
    print("Could not find bottomBar block")
