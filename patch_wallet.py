import re
with open("app/src/main/java/com/example/ui/screens/WalletScreen.kt", "r") as f:
    text = f.read()

target = "            // SECTION 2: TRANSACTION HISTORY TITLE"

replacement = """            // REFERRAL SECTION
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("REFERRAL SYSTEM", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSecondaryContainer, letterSpacing = 1.5.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val refCode = user?.referralCode ?: ""
                        if (refCode.isNotEmpty()) {
                            Text("Your Referral Code: $refCode", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("Share this with friends to earn tokens!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        if (user?.referredBy.isNullOrEmpty()) {
                            Text("Were you referred by a friend? Enter their code to get 50 bonus tokens!", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.height(8.dp))
                            var refInput by remember { mutableStateOf("") }
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = refInput,
                                    onValueChange = { refInput = it },
                                    placeholder = { Text("Referral Code") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { 
                                        if(refInput.isNotBlank()) viewModel.applyReferralCode(refInput)
                                        refInput = ""
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("CLAIM")
                                }
                            }
                        } else {
                            Text("Referred by: ${user?.referredBy}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("You received 50 bonus tokens!", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }
            }
            // SECTION 2: TRANSACTION HISTORY TITLE"""

text = text.replace(target, replacement)
with open("app/src/main/java/com/example/ui/screens/WalletScreen.kt", "w") as f:
    f.write(text)
