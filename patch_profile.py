import re
with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "r") as f:
    text = f.read()

target = """                // Preferences Card"""

replacement = """                // Rewards Section
                ProfileSectionCard(title = "Daily Rewards", description = "Login daily and play matches to earn tokens.") {
                    Text("Total Tokens: ${currentUser.tokens}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (currentUser.tokens % 100) / 100f }, 
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Text("${currentUser.tokens % 100}/100 to next level", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Spacer(Modifier.height(16.dp))
                    Text("Login Streak: ${currentUser.loginStreak} Days", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (currentUser.loginStreak % 7) / 7f }, 
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Text("${currentUser.loginStreak % 7}/7 Days for bonus box", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Preferences Card"""

text = text.replace(target, replacement)
with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "w") as f:
    f.write(text)
