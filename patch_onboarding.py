import re
with open("app/src/main/java/com/example/ui/screens/OnboardingScreen.kt", "r") as f:
    text = f.read()

# Replace socialLink with dob
text = text.replace("var socialLink by remember { mutableStateOf(\"\") }", "var dob by remember { mutableStateOf(\"\") }")
text = text.replace("BasicProfileStep(name, phone, socialLink, { name = it }, { phone = it }, { socialLink = it })", "BasicProfileStep(name, phone, dob, { name = it }, { phone = it }, { dob = it })")
text = text.replace("socialLink: String", "dob: String")
text = text.replace("onSocialChange", "onDobChange")

# Update BasicProfileStep textfield
target_tf = """        OutlinedTextField(
            value = socialLink,
            onValueChange = onSocialChange,
            label = { Text("Social Link (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(20.dp)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        )"""

repl_tf = """        OutlinedTextField(
            value = dob,
            onValueChange = onDobChange,
            label = { Text("Date of Birth (DD/MM/YYYY)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (name.length >= 2 && dob.length >= 2 && phone.length >= 2) {
            val refCode = (name.take(2) + dob.take(2) + phone.takeLast(2)).uppercase()
            Text(text = "Your Referral Code: $refCode", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        
        if (phone.isBlank() || dob.isBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "⚠ Mobile and DOB are mandatory to proceed.", color = Color.Red, fontSize = 12.sp)
        }
"""
text = text.replace(target_tf, repl_tf)

text = text.replace("0 -> name.isNotBlank() && phone.isNotBlank()", "0 -> name.isNotBlank() && phone.isNotBlank() && dob.isNotBlank()")

# Fix save onboarding logic
target_save = """                        } else {
                            viewModel.completeOnboarding(
                                mapOf(
                                    "name" to name,
                                    "phone" to phone,
                                    "socialLink" to socialLink,
                                    "inGameName" to inGameName,
                                    "freeFireId" to freeFireId,
                                    "theme" to selectedTheme
                                )
                            )
                            onComplete()
                        }"""
repl_save = """                        } else {
                            viewModel.completeOnboarding(
                                mapOf(
                                    "name" to name,
                                    "phone" to phone,
                                    "dob" to dob,
                                    "inGameName" to inGameName,
                                    "freeFireId" to freeFireId,
                                    "theme" to selectedTheme
                                )
                            )
                            onComplete()
                        }"""
text = text.replace(target_save, repl_save)

with open("app/src/main/java/com/example/ui/screens/OnboardingScreen.kt", "w") as f:
    f.write(text)
