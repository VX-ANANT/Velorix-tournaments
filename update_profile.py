import re

with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "r") as f:
    text = f.read()

# Add states
state_injections = """
    val missions by viewModel.missions.collectAsState()
    val showConfetti by viewModel.showConfetti.collectAsState()
    val matchStats by viewModel.matchStats.collectAsState(initial = emptyList())
"""
text = text.replace("    val user by viewModel.userState.collectAsState()", "    val user by viewModel.userState.collectAsState()\n" + state_injections)

# Add Box wrapping Scaffold
text = text.replace("    Scaffold(", "    Box(modifier = Modifier.fillMaxSize()) {\n    Scaffold(")

# Find end of Scaffold and add Confetti
# This can be tricky, but we know Scaffold ends with:
#                     Spacer(modifier = Modifier.height(16.dp))
#                 }
#             }
#         }
#     }
# }
# Wait, let's just use regex for the end of the file.
end_of_file_replacement = """
    } // Box end
}
"""
# Or we can just find the last }
idx = text.rfind("}")
if idx != -1:
    text = text[:idx] + "        com.example.ui.components.ConfettiAnimation(isVisible = showConfetti, modifier = Modifier.fillMaxSize())\n    }\n}\n"

# Replace the Chart and add missions
chart_start = text.find("// WINNING TRENDS CHART (Recharts conceptual equivalent)")
chart_end = text.find("                    Spacer(modifier = Modifier.height(24.dp))", chart_start) 
if chart_end == -1:
    chart_end = text.find("                    Spacer(modifier = Modifier.height(16.dp))", chart_start)

if chart_start != -1 and chart_end != -1:
    new_ui = """
                    // VICO K/D TRENDS CHART
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp)
                    ) {
                        Text("RECENT KILLS TREND", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                        
                        val modelProducer = remember { com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer() }
                        
                        LaunchedEffect(matchStats) {
                            val recentStats = matchStats.sortedBy { it.timestamp }.takeLast(10)
                            val killsData = if (recentStats.isNotEmpty()) recentStats.map { it.kills as Number } else listOf(0)
                            modelProducer.runTransaction {
                                com.patrykandpatrick.vico.core.cartesian.data.lineSeries {
                                    series(killsData)
                                }
                            }
                        }
                        
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)) {
                            com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost(
                                chart = com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart(
                                    com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer()
                                ),
                                modelProducer = modelProducer,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // DAILY MISSIONS
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp)
                    ) {
                        Text("DAILY MISSIONS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                        
                        missions.forEach { mission ->
                            val progressFloat = if (mission.target > 0) (mission.progress.toFloat() / mission.target).coerceIn(0f, 1f) else 0f
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mission.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(mission.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { progressFloat },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    )
                                    Text("${mission.progress} / ${mission.target}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Button(
                                    onClick = { viewModel.claimMission(mission) },
                                    enabled = mission.isCompleted && !mission.isClaimed,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text(if (mission.isClaimed) "Claimed" else "Claim", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                            if (mission != missions.last()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            }
                        }
                    }
"""
    text = text[:chart_start] + new_ui + text[chart_end:]

with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "w") as f:
    f.write(text)

