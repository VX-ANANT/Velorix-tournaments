import re
with open("app/src/main/java/com/example/ui/screens/OnboardingScreen.kt", "r") as f:
    text = f.read()

# Replace socialLink with dob inside the 'save' block
text = text.replace("socialLink = socialLink.ifEmpty { currentUser.socialLink },", "dob = dob.ifEmpty { currentUser.dob },\n                                    fullName = name.ifEmpty { currentUser.fullName },\n                                    mobileNo = phone.ifEmpty { currentUser.mobileNo },")

# Replace the text field
target = """        OutlinedTextField(
            value = socialLink,
            onValueChange = onDobChange,
            label = { Text("Social Media Link (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(20.dp)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        )"""

replacement = """        OutlinedTextField(
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
        }"""

text = text.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/OnboardingScreen.kt", "w") as f:
    f.write(text)
