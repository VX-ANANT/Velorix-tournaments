package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.LegalComplianceModal
import com.example.ui.components.LegalTab
import com.example.ui.viewmodel.PlatformViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: PlatformViewModel,
    onComplete: () -> Unit
) {
    val user by viewModel.userState.collectAsStateWithLifecycle()
    var currentStep by remember { mutableStateOf(0) }
    
    // Form fields
    var selectedTheme by remember { mutableStateOf("system") }
    
    // Step 0 Fields
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var selectedState by remember { mutableStateOf("Delhi") }
    var isAgeAccepted by remember { mutableStateOf(true) }
    var showLegalModal by remember { mutableStateOf(false) }
    var selectedLegalTab by remember { mutableStateOf(LegalTab.TERMS) }

    // Step 1 Fields
    var inGameName by remember { mutableStateOf("") }
    var freeFireId by remember { mutableStateOf("") }

    LaunchedEffect(user) {
        user?.let { u ->
            if (name.isBlank()) {
                val candidateName = u.fullName.ifBlank { u.username }
                if (candidateName.isNotBlank() && !candidateName.startsWith("Player_")) {
                    name = candidateName
                }
            }
            if (phone.isBlank()) {
                val candidatePhone = u.mobileNo.ifBlank { u.phoneOrEmail }
                if (candidatePhone.isNotBlank() && !candidatePhone.contains("@")) {
                    phone = candidatePhone
                }
            }
        }
    }

    val totalSteps = 4

    val isIdValid = freeFireId.isBlank() || com.example.ui.components.GameIdValidator.isValid(freeFireId)
    val canProceed = when(currentStep) {
        0 -> name.isNotBlank() && phone.isNotBlank() && dob.isNotBlank() && isAgeAccepted
        1 -> inGameName.isNotBlank() && freeFireId.isNotBlank() && com.example.ui.components.GameIdValidator.isValid(freeFireId)
        else -> true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        if (currentStep < totalSteps - 1) {
                            currentStep++
                        } else {
                            // Save
                            val currentUser = user
                            if (currentUser != null) {
                                val calculatedAge = com.example.util.ComplianceEngine.calculateAge(dob)
                                val is18Plus = calculatedAge >= 18
                                val updatedUser = currentUser.copy(
                                    username = name.ifEmpty { currentUser.username },
                                    phoneOrEmail = phone.ifEmpty { currentUser.phoneOrEmail },
                                    dob = dob.ifEmpty { currentUser.dob },
                                    state = selectedState.ifEmpty { currentUser.state },
                                    isAgeVerified = is18Plus,
                                    legalConsentAccepted = true,
                                    legalConsentTimestamp = System.currentTimeMillis(),
                                    fullName = name.ifEmpty { currentUser.fullName },
                                    mobileNo = phone.ifEmpty { currentUser.mobileNo },
                                    inGameName = inGameName.ifEmpty { currentUser.inGameName },
                                    freeFireId = freeFireId.ifEmpty { currentUser.freeFireId },
                                    balance = currentUser.balance + 10.0 // Starter Bonus
                                )
                                viewModel.updateProfile(updatedUser)
                            }
                            viewModel.completeOnboarding(selectedTheme)
                            onComplete()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = canProceed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (currentStep < totalSteps - 1) "Continue" else "Claim & Enter",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Step indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until totalSteps) {
                    val isSelected = currentStep >= i
                    val width by animateFloatAsState(if (isSelected) 32f else 12f)
                    val color by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(6.dp)
                            .width(width.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(400, delayMillis = 90)) + slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth })).togetherWith(
                        fadeOut(animationSpec = tween(90)) + slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth })
                    )
                }, label = "onboarding_step"
            ) { step ->
                when (step) {
                    0 -> BasicProfileStep(
                        name = name,
                        phone = phone,
                        dob = dob,
                        selectedState = selectedState,
                        onStateChange = { selectedState = it },
                        isAgeAccepted = isAgeAccepted,
                        onAgeAcceptedChange = { isAgeAccepted = it },
                        onNameChange = { name = it },
                        onPhoneChange = { phone = it },
                        onDobChange = { dob = it },
                        onOpenLegal = { tab ->
                            selectedLegalTab = tab
                            showLegalModal = true
                        }
                    )
                    1 -> ProfileSetupStep(inGameName, freeFireId, { inGameName = it }, { freeFireId = it })
                    2 -> ThemeSelectionStep(selectedTheme) { selectedTheme = it }
                    3 -> WelcomeBonusStep()
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
fun BasicProfileStep(
    name: String,
    phone: String,
    dob: String,
    selectedState: String,
    onStateChange: (String) -> Unit,
    isAgeAccepted: Boolean,
    onAgeAcceptedChange: (Boolean) -> Unit,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onDobChange: (String) -> Unit,
    onOpenLegal: (LegalTab) -> Unit
) {
    val calculatedAge = remember(dob) { com.example.util.ComplianceEngine.calculateAge(dob) }
    val is18Plus = remember(dob) { com.example.util.ComplianceEngine.is18Plus(dob) }
    val isRestrictedState = remember(selectedState) { com.example.util.ComplianceEngine.isRestrictedTerritory(selectedState) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create Profile",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Set up your basic details to connect with others.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_profile), contentDescription = null, modifier = Modifier.size(20.dp)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        )
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_phone), contentDescription = null, modifier = Modifier.size(20.dp)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        )
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
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

        // Dynamic Statutory Age Verification Feedback
        if (dob.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            if (is18Plus) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_untitledui_shield_tick),
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "STATUTORY MAJORITY CONFIRMED (AGE: $calculatedAge)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = "Eligible for real-money competitive tournament brackets and token prize liquidation under PROG Act 2025 & MeitY PROG Rules 2026.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (calculatedAge in 1..17) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "JUNIOR CADET CLASSIFICATION (AGE: $calculatedAge)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                            Text(
                                text = "Under PROG Act 2025 § 5, monetary stake gaming is barred to minors under 18. Operative account is limited to Free Practice Scrims.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = selectedState,
            onValueChange = onStateChange,
            label = { Text("State of Residence (India)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(20.dp))
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isRestrictedState) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = if (isRestrictedState) Color(0xFFEF4444).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        )

        if (isRestrictedState) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFEF4444).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "STATE STATUTORY RESTRICTION (${selectedState.uppercase()})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                        Text(
                            text = "${com.example.util.ComplianceEngine.getStatutoryCitation(selectedState)} bars real-money tournament entry. Free practice scrims remain open.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (name.length >= 2 && dob.length >= 2 && phone.length >= 2) {
            val refCode = (name.take(2) + dob.take(2) + phone.takeLast(2)).uppercase()
            Text(text = "Your Referral Code: $refCode", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        
        if (phone.isBlank() || dob.isBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Mobile and DOB are mandatory to proceed.", color = Color(0xFFEF4444), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isAgeAccepted,
                    onCheckedChange = onAgeAcceptedChange,
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "18+ Age & PROG Act 2025/2026 Declaration",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "I certify I am 18+ and adhere to PROG Rules 2026 and VeloRix Fair Play Codex.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Review Official Policies",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onOpenLegal(LegalTab.TERMS) }
                            .padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileSetupStep(inGameName: String, freeFireId: String, onNameChange: (String) -> Unit, onIdChange: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Link your Identity",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This allows us to track your tournament stats.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        com.example.ui.components.GameIdInputField(
            value = freeFireId,
            onValueChange = onIdChange,
            label = "Free Fire UID (8-12 Digits)",
            placeholder = "e.g. 5123984129",
            testTag = "onboarding_game_id_input",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = inGameName,
            onValueChange = onNameChange,
            label = { Text("In-Game Name (IGN)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_profile), contentDescription = null, modifier = Modifier.size(20.dp)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
fun ThemeSelectionStep(selectedTheme: String, onThemeSelected: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Choose your side",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select a theme preference for your dashboard.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        ThemeOptionCard("light", "Light Mode", Icons.Default.LightMode, selectedTheme == "light") { onThemeSelected("light") }
        Spacer(modifier = Modifier.height(16.dp))
        ThemeOptionCard("dark", "Dark Mode", Icons.Default.DarkMode, selectedTheme == "dark") { onThemeSelected("dark") }
        Spacer(modifier = Modifier.height(16.dp))
        ThemeOptionCard("system", "System Default", Icons.Default.SettingsSystemDaydream, selectedTheme == "system") { onThemeSelected("system") }
    }
}

@Composable
fun WelcomeBonusStep() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome Bonus",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You're all set! Claim your starter bonus to begin competing.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(64.dp))
        
        // Animated glow box
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "pulse"
        )
        
        Box(
            modifier = Modifier
                .size(160.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(4.dp, MaterialTheme.colorScheme.onPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("+10", fontSize = 36.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
                    Text("VT", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(36.dp))
        Text(
            text = "Added securely to your Velorix Wallet.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_untitledui_shield_tick),
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Skill Gaming Protected • DPDP Act 2023 Compliant",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ThemeOptionCard(themeId: String, title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (isSelected) 1.02f else 1f)
    val borderColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
    val bgColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
