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
import com.example.ui.components.LegalComplianceModal
import com.example.ui.components.LegalTab
import com.example.ui.viewmodel.PlatformViewModel

@Composable
fun AuthScreen(
    viewModel: PlatformViewModel,
    onAuthSuccess: () -> Unit,
    onRegisterSuccess: (() -> Unit)? = null
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var showLegalModal by remember { mutableStateOf(false) }
    var selectedLegalTab by remember { mutableStateOf(LegalTab.TERMS) }

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
                        onSwitchToLogin = { isSignUpMode = false },
                        onOpenLegal = { tab ->
                            selectedLegalTab = tab
                            showLegalModal = true
                        }
                    )
                } else {
                    LoginScreen(
                        viewModel = viewModel,
                        onAuthSuccess = onAuthSuccess,
                        onSwitchToSignUp = { isSignUpMode = true },
                        onOpenLegal = { tab ->
                            selectedLegalTab = tab
                            showLegalModal = true
                        }
                    )
                }
            }
        }
    }

    if (showLegalModal) {
        LegalComplianceModal(
            initialTab = selectedLegalTab,
            onDismissRequest = { showLegalModal = false }
        )
    }
}

@Composable
fun LoginScreen(
    viewModel: PlatformViewModel,
    onAuthSuccess: () -> Unit,
    onSwitchToSignUp: () -> Unit,
    onOpenLegal: (LegalTab) -> Unit = {}
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
        
        AuthLegalConsentFootnote(onOpenLegal = onOpenLegal)

        Spacer(modifier = Modifier.height(16.dp))

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
    onSwitchToLogin: () -> Unit,
    onOpenLegal: (LegalTab) -> Unit = {}
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
    var isAgeConfirmed by remember { mutableStateOf(false) }
    var isStateCompliant by remember { mutableStateOf(true) }
    var isTermsAccepted by remember { mutableStateOf(true) }

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
            placeholder = { Text("e.g. VRX-NAME-9999 (Squad Bonus)") },
            leadingIcon = { Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )

        // Statutory & COPPA Compliance Gate Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF12151E),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isAgeConfirmed) Color(0xFF22C55E).copy(alpha = 0.4f) else Color(0xFFEF4444).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Mandatory Age Gate (18+ / COPPA Compliance)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { isAgeConfirmed = !isAgeConfirmed }
                ) {
                    Checkbox(
                        checked = isAgeConfirmed,
                        onCheckedChange = { isAgeConfirmed = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF22C55E),
                            uncheckedColor = Color(0xFFEF4444)
                        )
                    )
                    Spacer(Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "I confirm I am 18 years of age or older",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Mandatory statutory verification for skill-based tournaments (COPPA & IT Rules).",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // State Skill-Gaming Jurisdiction Warranty
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { isStateCompliant = !isStateCompliant }
                ) {
                    Checkbox(
                        checked = isStateCompliant,
                        onCheckedChange = { isStateCompliant = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "I am not a resident of restricted states",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Assam, Odisha, Telangana, Nagaland, Sikkim, Andhra Pradesh.",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Terms of Service & Privacy & DMCA Safe Harbor
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { isTermsAccepted = !isTermsAccepted }
                ) {
                    Checkbox(
                        checked = isTermsAccepted,
                        onCheckedChange = { isTermsAccepted = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "I agree to the Terms of Service, Privacy Policy & Safe Harbor IP guidelines.",
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = "Terms of Service",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onOpenLegal(LegalTab.TERMS) }
                    )
                    Text("•", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text(
                        text = "Privacy Policy",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onOpenLegal(LegalTab.PRIVACY) }
                    )
                    Text("•", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text(
                        text = "Refunds & Escrow",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onOpenLegal(LegalTab.REFUNDS) }
                    )
                }
            }
        }

        // Submit Button
        Button(
            onClick = {
                if (!isAgeConfirmed) {
                    viewModel.showError("Age Verification Required: You must certify you are 18+ to create an account.")
                    return@Button
                }
                if (!isStateCompliant) {
                    viewModel.showError("Jurisdiction Notice: Real-money contests are restricted in your state.")
                    return@Button
                }
                if (!isTermsAccepted) {
                    viewModel.showError("Please accept Terms of Service & Privacy Policy to continue.")
                    return@Button
                }
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
            enabled = !isAuthLoading && isAgeConfirmed && isStateCompliant && isTermsAccepted
        ) {
            if (isAuthLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("Create Account (18+ Verified)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
        
        AuthLegalConsentFootnote(onOpenLegal = onOpenLegal)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Already have an account? Login",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.clickable { onSwitchToLogin() }.padding(8.dp).align(Alignment.CenterHorizontally).testTag("toggle_auth_mode")
        )
    }
}

@Composable
fun AuthLegalConsentFootnote(
    onOpenLegal: (LegalTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_untitledui_shield_tick),
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Skill Gaming • 18+ Only • DPDP Compliant",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8)
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "By continuing, you accept ",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
            )
            Text(
                text = "Terms",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onOpenLegal(LegalTab.TERMS) }
            )
            Text(
                text = " • ",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
            Text(
                text = "Privacy",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onOpenLegal(LegalTab.PRIVACY) }
            )
            Text(
                text = " • ",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
            Text(
                text = "Fair Play",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onOpenLegal(LegalTab.FAIR_PLAY) }
            )
        }
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


