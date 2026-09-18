import re

with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "r") as f:
    text = f.read()

# Find the start of LoginScreen
login_screen_idx = text.find("fun LoginScreen(")

# The part before LoginScreen is fine, we just need to replace LoginScreen with a clean version
header = text[:login_screen_idx]

clean_login = """fun LoginScreen(
    viewModel: PlatformViewModel,
    onAuthSuccess: () -> Unit,
    onSwitchToSignUp: () -> Unit
) {
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isAuthLoading by viewModel.isAuthLoading.collectAsState(initial = false)
    
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

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = emailOrPhone,
            onValueChange = { emailOrPhone = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("email_input"),
            label = { Text("Email or Phone") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .testTag("password_input"),
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )
        
        Text(
            text = "Forgot Password?",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 24.dp, end = 4.dp)
                .clickable { showForgotPasswordDialog = true }
        )

        // Submit Button
        Button(
            onClick = {
                val method = if (emailOrPhone.contains("@")) "email" else "phone"
                viewModel.login(emailOrPhone, password, method, onComplete = onAuthSuccess)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("submit_login_button"),
            shape = RoundedCornerShape(12.dp),
            enabled = !isAuthLoading
        ) {
            if (isAuthLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // OR Divider
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            Text(" OR ", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        val context = androidx.compose.ui.platform.LocalContext.current
        var activityContext: android.content.Context = context
        while (activityContext is android.content.ContextWrapper) {
            if (activityContext is android.app.Activity) break
            activityContext = activityContext.baseContext
        }
        
        // Google Login Button
        AuthOptionButton(
            text = "Continue with Google",
            icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_mail),
            onClick = { viewModel.loginWithGoogle(activityContext, onComplete = onAuthSuccess) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Don't have an account? Sign Up",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.clickable { onSwitchToSignUp() }.padding(8.dp).align(Alignment.CenterHorizontally).testTag("toggle_auth_mode")
        )
    }
}
"""

registration_screen_idx = text.find("@Composable\nfun RegistrationScreen(")

if registration_screen_idx != -1:
    footer = text[registration_screen_idx:]
    with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "w") as f:
        f.write(header + clean_login + "\n" + footer)
else:
    print("Could not find RegistrationScreen")
