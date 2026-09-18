package com.example.ui.screens

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing

import androidx.compose.material3.MaterialTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.vectorResource
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.PlatformViewModel

@Composable
fun AuthScreen(
    viewModel: PlatformViewModel,
    onAuthSuccess: () -> Unit,
    onRegisterSuccess: (() -> Unit)? = null
) {
    var isSignUpMode by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(scrollState)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
            val floatOffset by infiniteTransition.animateFloat(
                initialValue = -5f,
                targetValue = 5f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "auth_bounce"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .offset(y = floatOffset.dp)
            ) {
                // Keep it clean and minimal
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.velorix_logo_image),
                    contentDescription = "Velorix Logo",
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.size(80.dp).padding(bottom = 16.dp)
                )

                Text(
                    text = if (isSignUpMode) "Create your account" else "Sign in to continue",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.padding(bottom = 40.dp)
                )

                if (isSignUpMode) {
                    RegistrationScreen(
                        viewModel = viewModel,
                        onAuthSuccess = {
                            if (onRegisterSuccess != null) {
                                onRegisterSuccess()
                            } else {
                                onAuthSuccess()
                            }
                        },
                        onSwitchToLogin = { isSignUpMode = false }
                    )
                } else {
                    LoginScreen(
                        viewModel = viewModel,
                        onAuthSuccess = onAuthSuccess,
                        onSwitchToSignUp = { isSignUpMode = true }
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: PlatformViewModel,
    onAuthSuccess: () -> Unit,
    onSwitchToSignUp: () -> Unit
) {
    val isAuthLoading by viewModel.isAuthLoading.collectAsState(initial = false)

    
    val googleSignInLauncher = rememberLauncherForActivityResult(
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
            when (e.statusCode) {
                12501 -> {
                    // User cancelled Google Sign-In prompt
                    android.util.Log.d("AuthScreen", "Google Sign-In cancelled by user")
                }
                12502 -> {
                    // Sign-in currently in progress
                    viewModel.showError("Google Sign-In session busy. Please tap again.")
                }
                else -> {
                    viewModel.showError("Google Login Failed: Code ${e.statusCode}")
                }
            }
        } catch (e: Exception) {
            viewModel.showError("Google Login Failed: ${e.message}")
        }
    }
    // isAuthLoading is already defined in RegistrationScreen
    

    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    
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
            leadingIcon = { Icon(androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_profile), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
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
            leadingIcon = { Icon(androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_lock), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
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
            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_google_colored),
            enabled = !isAuthLoading
        ) {
            try {
                val webClientId = context.getString(com.example.R.string.default_web_client_id)
                val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
                val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(activityContext, gso)
                try {
                    googleSignInClient.signOut().addOnCompleteListener {
                        try {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        } catch (e: Exception) {
                            viewModel.showError("Google Sign In Error: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                }
            } catch (e: Exception) {
                viewModel.showError("Google Sign In Error: ${e.message}")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Don't have an account? Sign Up",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.clickable { onSwitchToSignUp() }.padding(8.dp).align(Alignment.CenterHorizontally).testTag("toggle_auth_mode")
        )
    }
}

@Composable
fun RegistrationScreen(
    viewModel: PlatformViewModel,
    onAuthSuccess: () -> Unit,
    onSwitchToLogin: () -> Unit
) {
    val isAuthLoading by viewModel.isAuthLoading.collectAsState(initial = false)

    
    val googleSignInLauncher = rememberLauncherForActivityResult(
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
            when (e.statusCode) {
                12501 -> {
                    // User cancelled Google Sign-In prompt
                    android.util.Log.d("AuthScreen", "Google Sign-In cancelled by user")
                }
                12502 -> {
                    // Sign-in currently in progress
                    viewModel.showError("Google Sign-In session busy. Please tap again.")
                }
                else -> {
                    viewModel.showError("Google Login Failed: Code ${e.statusCode}")
                }
            }
        } catch (e: Exception) {
            viewModel.showError("Google Login Failed: ${e.message}")
        }
    }
    // isAuthLoading is already defined in RegistrationScreen
    

    var username by remember { mutableStateOf("") }
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("reg_username_input"),
            label = { Text("Username") },
            leadingIcon = { Icon(androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_profile), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )

        OutlinedTextField(
            value = emailOrPhone,
            onValueChange = { emailOrPhone = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("reg_email_input"),
            label = { Text("Email or Phone") },
            leadingIcon = { Icon(androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_mail), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                .padding(bottom = 16.dp)
                .testTag("reg_password_input"),
            label = { Text("Password") },
            leadingIcon = { Icon(androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_lock), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )

        OutlinedTextField(
            value = referralCode,
            onValueChange = { referralCode = it.uppercase().filter { ch -> ch.isLetterOrDigit() } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .testTag("reg_referral_input"),
            label = { Text("Referral Code (Optional)") },
            placeholder = { Text("e.g. VT7890 (Get +50 Tokens)") },
            leadingIcon = { Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )

        // Submit Button
        Button(
            onClick = {
                val method = if (emailOrPhone.contains("@")) "email" else "phone"
                viewModel.register(
                    username = username,
                    phoneOrEmail = emailOrPhone,
                    passwordHash = password,
                    loginMethod = method,
                    referralCode = referralCode,
                    onComplete = { if (it) onAuthSuccess() }
                )
            },
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("submit_register_button"),
            shape = RoundedCornerShape(12.dp),
            enabled = !isAuthLoading
        ) {
            if (isAuthLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_google_colored),
            enabled = !isAuthLoading
        ) {
            try {
                val webClientId = context.getString(com.example.R.string.default_web_client_id)
                val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
                val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(activityContext, gso)
                try {
                    googleSignInClient.signOut().addOnCompleteListener {
                        try {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        } catch (e: Exception) {
                            viewModel.showError("Google Sign In Error: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                }
            } catch (e: Exception) {
                viewModel.showError("Google Sign In Error: ${e.message}")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Already have an account? Login",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.clickable { onSwitchToLogin() }.padding(8.dp).align(Alignment.CenterHorizontally).testTag("toggle_auth_mode")
        )
    }
}

@Composable
fun AuthOptionButton(
    text: String,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("google_login_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onBackground
        )
    ) {
        if (painter != null) {
            Icon(painter = painter, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Unspecified, modifier = Modifier.size(24.dp))
        } else if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Unspecified, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
    }
}


