with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    text = f.read()

target = """            items(filteredTournaments) { match ->
                TournamentCard(
                    match = match,"""

replacement = """            if (filteredTournaments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_matches),
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.Unspecified,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "NO TOURNAMENTS AVAILABLE",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Data updates automatically. Please check back later.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
            items(filteredTournaments) { match ->
                TournamentCard(
                    match = match,"""

target2 = """                    onClick = { onNavigateToTournament(match.id) }
                )
            }
        }
    }
}

@Composable"""

replacement2 = """                    onClick = { onNavigateToTournament(match.id) }
                )
            }
            } // end of else
        }
    }
}

@Composable"""

text = text.replace(target, replacement)
text = text.replace(target2, replacement2)

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(text)
