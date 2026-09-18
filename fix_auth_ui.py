import re

with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "r") as f:
    text = f.read()

forgot_password_ui = """
        var showForgotPasswordDialog by remember { mutableStateOf(false) }

        if (showForgotPasswordDialog) {
            var resetEmail by remember { mutableStateOf("") }
            var resetStatus by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showForgotPasswordDialog = false },
                title = { Text("Reset Password") },
                text = {
                    Column {
                        Text("Enter your email address to receive a password reset link.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        if (resetStatus.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(resetStatus, color = if (resetStatus.contains("sent", true)) Color.Green else Color.Red, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.resetPassword(resetEmail) { success, message ->
                            resetStatus = message
                        }
                    }) {
                        Text("Send Reset Link")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotPasswordDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
"""

text = text.replace("        // Submit Button", forgot_password_ui + "\n        // Submit Button")

forgot_password_btn = """
        Text(
            text = "Forgot Password?",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 16.dp, end = 4.dp)
                .clickable { showForgotPasswordDialog = true }
        )
"""

text = text.replace("            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))\n        )", "            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))\n        )\n" + forgot_password_btn)

with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "w") as f:
    f.write(text)

