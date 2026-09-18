import re
with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "r") as f:
    text = f.read()

# Update AuthOptionButton
target_btn = """fun AuthOptionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,"""

replacement_btn = """fun AuthOptionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,"""
text = text.replace(target_btn, replacement_btn)

# Update the googleSignInLauncher logic to prevent multiple clicks and also add the fallback.
# In LoginScreen
target_login = """    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.loginWithGoogleToken(idToken, onComplete = onAuthSuccess)
            } else {
                viewModel.showError("Google Login Failed: Missing ID Token")
            }
        } catch (e: ApiException) {
            viewModel.showError("Google Login Failed: Code ${e.statusCode}")
        } catch (e: Exception) {
            viewModel.showError("Google Login Failed: ${e.message}")
        }
    }"""

replacement_login = """    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.loginWithGoogleToken(idToken, onComplete = onAuthSuccess)
            } else {
                viewModel.showError("Google Login Failed: Missing ID Token")
            }
        } catch (e: ApiException) {
            viewModel.showError("Google Login Failed: Code ${e.statusCode}")
        } catch (e: Exception) {
            viewModel.showError("Google Login Failed: ${e.message}")
        }
    }
    val isAuthLoading by viewModel.isAuthLoading.collectAsState(initial = false)"""
text = text.replace(target_login, replacement_login)

# Update RegistrationScreen
target_reg = """    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.loginWithGoogleToken(idToken, onComplete = onAuthSuccess)
            } else {
                viewModel.showError("Google Login Failed: Missing ID Token")
            }
        } catch (e: ApiException) {
            viewModel.showError("Google Login Failed: Code ${e.statusCode}")
        } catch (e: Exception) {
            viewModel.showError("Google Login Failed: ${e.message}")
        }
    }"""
replacement_reg = """    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.loginWithGoogleToken(idToken, onComplete = onAuthSuccess)
            } else {
                viewModel.showError("Google Login Failed: Missing ID Token")
            }
        } catch (e: ApiException) {
            viewModel.showError("Google Login Failed: Code ${e.statusCode}")
        } catch (e: Exception) {
            viewModel.showError("Google Login Failed: ${e.message}")
        }
    }
    // isAuthLoading is already defined in RegistrationScreen"""
text = text.replace(target_reg, replacement_reg)


# Replace Login button usages
target_login_btn = """        AuthOptionButton(
            text = "Continue with Google",
            icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_mail),
            onClick = { 
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken("27931798964-h6fiau2df3i6e3049fontnctpsh06763.apps.googleusercontent.com")
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(activityContext, gso)
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        )"""

replacement_login_btn = """        AuthOptionButton(
            text = "Continue with Google",
            icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_mail),
            enabled = !isAuthLoading,
            onClick = { 
                if (isAuthLoading) return@AuthOptionButton
                viewModel.loginWithGoogle(
                    context = activityContext,
                    onComplete = onAuthSuccess,
                    onFallback = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken("27931798964-h6fiau2df3i6e3049fontnctpsh06763.apps.googleusercontent.com")
                            .requestEmail()
                            .build()
                        val googleSignInClient = GoogleSignIn.getClient(activityContext, gso)
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    }
                )
            }
        )"""
text = text.replace(target_login_btn, replacement_login_btn)

# Replace Reg button usages
target_reg_btn = """        AuthOptionButton(
            text = "Sign up with Google",
            icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_mail),
            onClick = { 
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken("27931798964-h6fiau2df3i6e3049fontnctpsh06763.apps.googleusercontent.com")
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(activityContext, gso)
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        )"""
replacement_reg_btn = """        AuthOptionButton(
            text = "Sign up with Google",
            icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_mail),
            enabled = !isAuthLoading,
            onClick = { 
                if (isAuthLoading) return@AuthOptionButton
                viewModel.loginWithGoogle(
                    context = activityContext,
                    onComplete = onAuthSuccess,
                    onFallback = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken("27931798964-h6fiau2df3i6e3049fontnctpsh06763.apps.googleusercontent.com")
                            .requestEmail()
                            .build()
                        val googleSignInClient = GoogleSignIn.getClient(activityContext, gso)
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    }
                )
            }
        )"""
text = text.replace(target_reg_btn, replacement_reg_btn)

# Let's also disable the primary buttons for regular login/signup
text = text.replace("""            onClick = { viewModel.login(emailOrPhone, password, onAuthSuccess) }""", """            enabled = !isAuthLoading,
            onClick = { if (!isAuthLoading) viewModel.login(emailOrPhone, password, onAuthSuccess) }""")

text = text.replace("""            onClick = { viewModel.register(username, emailOrPhone, password, onAuthSuccess) }""", """            enabled = !isAuthLoading,
            onClick = { if (!isAuthLoading) viewModel.register(username, emailOrPhone, password, onAuthSuccess) }""")


with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "w") as f:
    f.write(text)
print("AuthScreen Buttons Patched!")
