package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.vectorResource
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.User
import com.example.ui.components.VeloRixButton
import com.example.ui.components.VeloRixGlassAlertDialog
import com.example.ui.components.ReportProblemDialog
import com.example.ui.components.MyReportsDialog
import com.example.ui.components.stretchOverscroll
import com.example.ui.viewmodel.PlatformViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: PlatformViewModel,
    onLogout: () -> Unit,
    onNavigateToSupport: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onOpenAdminSituations: () -> Unit = {}
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val user by viewModel.userState.collectAsStateWithLifecycle()
    val unreadNotifCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle(0)
    val matchStats by viewModel.matchStats.collectAsStateWithLifecycle(emptyList())
    val showConfetti by viewModel.showConfetti.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val actionCooldowns by viewModel.actionCooldownSeconds.collectAsStateWithLifecycle()
    val profileCooldown = actionCooldowns["profile_update"] ?: 0
    val convertCooldown = actionCooldowns["token_convert"] ?: 0
    val reportCooldown = actionCooldowns["report_submission"] ?: 0
    val context = androidx.compose.ui.platform.LocalContext.current

    var isEditing by remember { mutableStateOf(false) }
    var editedUsername by remember { mutableStateOf(user?.username ?: "") }
    var editedPhone by remember { mutableStateOf(user?.phoneOrEmail ?: "") }
    var editedSocial by remember { mutableStateOf(user?.socialLink ?: "") }
    var editedBio by remember { mutableStateOf(user?.bio ?: "") }
    var editedFreeFireId by remember { mutableStateOf(user?.freeFireId ?: "") }
    var editedInGameName by remember { mutableStateOf(user?.inGameName ?: "") }
    var editedAvatarUrl by remember { mutableStateOf(user?.avatarUrl ?: "") }

    var showAvatarDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteReason by remember { mutableStateOf("") }
    var deleteDetails by remember { mutableStateOf("") }
    var showConvertDialog by remember { mutableStateOf(false) }
    var tokensToConvertInput by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }
    var showMyReportsDialog by remember { mutableStateOf(false) }
    var showAboutDeveloperModal by remember { mutableStateOf(false) }

    val isAnyPopupOpen = showAvatarDialog || showDeleteDialog || showConvertDialog || showReportDialog || showMyReportsDialog || showAboutDeveloperModal
    val bgBlurRadius by animateDpAsState(
        targetValue = if (isAnyPopupOpen) 22.dp else 0.dp,
        animationSpec = tween(durationMillis = 280, easing = LinearOutSlowInEasing),
        label = "profile_bg_blur"
    )

    LaunchedEffect(user) {
        val u = user
        if (!isEditing && u != null) {
            editedUsername = u.username
            editedPhone = u.phoneOrEmail
            editedSocial = u.socialLink
            editedBio = u.bio
            editedFreeFireId = u.freeFireId
            editedInGameName = u.inGameName
            editedAvatarUrl = u.avatarUrl
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text("Profile Settings", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    actions = {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                onNavigateToAbout()
                            }
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(com.example.R.drawable.ic_user_question),
                                contentDescription = "About",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                onNavigateToSettings()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        TextButton(
                            enabled = if (isEditing) profileCooldown == 0 else true,
                            onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); 
                            if (isEditing) {
                                val trimmedId = editedFreeFireId.trim()
                                if (trimmedId.isNotEmpty() && !com.example.ui.components.GameIdValidator.isValid(trimmedId)) {
                                    val err = com.example.ui.components.GameIdValidator.getErrorMessage(trimmedId) ?: "Invalid ID Format: Game ID must be an authentic 8-12 digit player UID (e.g. 5123984129)."
                                    android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                                    return@TextButton
                                }
                                val currentUser = user ?: return@TextButton
                                val updatedUser = currentUser.copy(
                                    username = editedUsername.trim(),
                                    phoneOrEmail = editedPhone.trim(),
                                    socialLink = editedSocial.trim(),
                                    bio = editedBio.trim(),
                                    avatarUrl = editedAvatarUrl,
                                    freeFireId = trimmedId,
                                    inGameName = editedInGameName.trim()
                                )
                                viewModel.updateProfile(updatedUser) { success ->
                                    if (success) {
                                        isEditing = false
                                    }
                                }
                            } else {
                                isEditing = true
                            }
                        }) {
                            val buttonLabel = when {
                                isEditing && profileCooldown > 0 -> "Wait ${profileCooldown}s"
                                isEditing -> "Save"
                                else -> "Edit"
                            }
                            Text(buttonLabel, color = if (isEditing && profileCooldown > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        ) { padding ->
            val currentUser = user
            if (currentUser == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val user = currentUser

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .blur(radius = bgBlurRadius)
                        .stretchOverscroll()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                // Header Avatar with edit photo launcher
                com.example.ui.components.UserAvatar(
                    avatarUrl = if (isEditing) editedAvatarUrl else user.avatarUrl,
                    username = user.username,
                    size = 100.dp,
                    showEditBadge = true,
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        showAvatarDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    showAvatarDialog = true
                }) {
                    Text("Change Profile Picture", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(user.username, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(user.phoneOrEmail, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Spacer(modifier = Modifier.height(32.dp))

                // Rewards Section
                ProfileSectionCard(title = "App Rewards & Token Converter", description = "Earn Tokens via Daily Missions and convert to VT Tokens (10 Tokens = 1 VT).") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Wallet Balance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("VT ${currentUser.balance.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Tokens Balance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${currentUser.tokens} Tokens", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Conversion Rate Highlight & Quick Convert Button
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Exchange Rate",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "10 Tokens = 1 VT Token",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    tokensToConvertInput = if (currentUser.tokens >= 10) (currentUser.tokens - (currentUser.tokens % 10)).toString() else "10"
                                    showConvertDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                enabled = currentUser.tokens >= 10,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text(
                                    text = if (currentUser.tokens >= 10) "Convert to VT" else "Need 10+ Tokens",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Text("Level Progress", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (currentUser.tokens % 100) / 100f }, 
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Text("${currentUser.tokens % 100}/100 Tokens to next Level Badge", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Spacer(Modifier.height(16.dp))
                    Text("Login Streak: ${currentUser.loginStreak} Days", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (currentUser.loginStreak % 7) / 7f }, 
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Text("${currentUser.loginStreak % 7}/7 Days for weekly streak reward", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Convert Tokens Dialog
                if (showConvertDialog) {
                    VeloRixGlassAlertDialog(
                        onDismissRequest = { showConvertDialog = false },
                        title = {
                            Text("CONVERT TOKENS TO VT", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        },
                        text = {
                            Column {
                                Text(
                                    "Exchange rate: 10 Tokens = 1 VT Token",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Available: ${currentUser.tokens} Tokens",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = tokensToConvertInput,
                                    onValueChange = { tokensToConvertInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Tokens to Convert") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                    )
                                )
                                val enteredTokens = tokensToConvertInput.toIntOrNull() ?: 0
                                val willReceiveVt = enteredTokens / 10
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "You will receive: $willReceiveVt VT Tokens",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        confirmButton = {
                                Button(
                                    onClick = {
                                        val amount = tokensToConvertInput.toIntOrNull() ?: 0
                                        if (amount >= 10) {
                                            viewModel.convertTokensToVt(amount) { success ->
                                                if (success) {
                                                    showConvertDialog = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = convertCooldown == 0 && (tokensToConvertInput.toIntOrNull() ?: 0) >= 10 && (tokensToConvertInput.toIntOrNull() ?: 0) <= currentUser.tokens,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.background
                                    )
                                ) {
                                    Text(if (convertCooldown > 0) "WAIT ${convertCooldown}S" else "CONFIRM CONVERT", fontWeight = FontWeight.Bold)
                                }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConvertDialog = false }) {
                                Text("CANCEL", color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Preferences Card: Theme Switcher & System Match Controller
                val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
                val isSystemMatch = themeMode == "system"

                ProfileSectionCard(
                    title = "App Appearance & Theme",
                    description = "Customize color scheme or enable automatic matching with device theme."
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // System Match Toggle Row
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSystemMatch) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SettingsBrightness,
                                            contentDescription = "System Match",
                                            tint = if (isSystemMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "System Match",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isSystemMatch) "Auto-sync active (${if (isSystemDark) "Dark" else "Light"} mode)" else "Manual theme override active",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = isSystemMatch,
                                    onCheckedChange = { checked ->
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        if (checked) {
                                            viewModel.setThemeMode("system")
                                        } else {
                                            // Default to the current system appearance if turning off system match
                                            viewModel.setThemeMode(if (isSystemDark) "dark" else "light")
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "SELECT COLOR SCHEME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3-Option Theme Cards: Light, Dark, System Auto
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val options = listOf(
                                Triple("light", "Light", Icons.Default.LightMode),
                                Triple("dark", "Dark", Icons.Default.DarkMode),
                                Triple("system", "System", Icons.Default.Smartphone)
                            )

                            options.forEach { (modeKey, modeTitle, modeIcon) ->
                                val isSelected = themeMode == modeKey
                                val activeBorder = if (isSelected) {
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            viewModel.setThemeMode(modeKey)
                                        },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = activeBorder
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 14.dp, horizontal = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = modeIcon,
                                            contentDescription = modeTitle,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = modeTitle,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // General Settings Card
                ProfileSectionCard(title = "General Information", description = "Your personal details and how we can reach you.") {
                    if (isEditing) {
                        OutlinedTextField(
                            value = editedUsername,
                            onValueChange = { editedUsername = it },
                            label = { Text("Display Name") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true,
                            leadingIcon = { Icon(androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_iconsax_profile), contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        OutlinedTextField(
                            value = editedPhone,
                            onValueChange = { editedPhone = it },
                            label = { Text("Phone / Email Address") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true,
                            leadingIcon = { Icon(androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_phone), contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        OutlinedTextField(
                            value = editedSocial,
                            onValueChange = { editedSocial = it },
                            label = { Text("Social Media Link") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        OutlinedTextField(
                            value = editedBio,
                            onValueChange = { editedBio = it },
                            label = { Text("Bio") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            maxLines = 3
                        )
                    } else {
                        ProfileInfoRow("Display Name", currentUser.username)
                        ProfileInfoRow("Phone / Email", currentUser.phoneOrEmail)
                        ProfileInfoRow("Social Media", currentUser.socialLink.ifEmpty { "Not provided" })
                        ProfileInfoRow("Bio", currentUser.bio.ifEmpty { "No bio provided." })
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // Gaming Identity Card
                ProfileSectionCard(title = "Gaming Identity", description = "Link your Free Fire UID to participate in tournaments seamlessly.") {
                    if (isEditing) {
                        com.example.ui.components.GameIdInputField(
                            value = editedFreeFireId,
                            onValueChange = { editedFreeFireId = it },
                            label = "Free Fire UID (8-12 Digits)",
                            placeholder = "e.g. 5123984129",
                            testTag = "profile_game_id_input",
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = editedInGameName,
                            onValueChange = { editedInGameName = it },
                            label = { Text("In-Game Name") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true,
                            leadingIcon = { Icon(androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_profile), contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    } else {
                        ProfileInfoRow("Free Fire UID", currentUser.freeFireId.ifEmpty { "Not linked" })
                        ProfileInfoRow("In-Game Name", currentUser.inGameName.ifEmpty { "Not set" })
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Career Statistics Card
                val displayMatchesPlayed = if (matchStats.isEmpty()) 0 else maxOf(matchStats.size, currentUser.matchesPlayed)
                val displayWins = if (matchStats.isEmpty()) 0 else maxOf(matchStats.count { it.position == 1 }, currentUser.totalWins)
                val displayKills = if (matchStats.isEmpty()) 0 else maxOf(matchStats.sumOf { it.kills }, currentUser.totalKills)

                ProfileSectionCard(title = "Career Statistics", description = "Your overall performance and milestones across all tournaments.") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatBox(title = "Matches Played", value = displayMatchesPlayed.toString(), icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_untitledui_target))
                        StatBox(title = "Total Wins", value = displayWins.toString(), icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_untitledui_trophy))
                        StatBox(title = "Total Kills", value = displayKills.toString(), icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_cursor))
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // Tournament History Card
                ProfileSectionCard(
                    title = "Tournament History",
                    description = "Past tournaments participated in, including match outcomes, placements, and earned rewards."
                ) {
                    if (matchStats.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_folder),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No tournament records yet",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Join and complete tournaments to see match outcomes and winnings here.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            matchStats.forEach { stat ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    color = when (stat.position) {
                                                        1 -> Color(0xFFFFD700).copy(alpha = 0.2f)
                                                        2 -> Color(0xFFC0C0C0).copy(alpha = 0.2f)
                                                        3 -> Color(0xFFCD7F32).copy(alpha = 0.2f)
                                                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    },
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        if (stat.position in 1..3) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.EmojiEvents,
                                                                contentDescription = null,
                                                                tint = when (stat.position) {
                                                                    1 -> Color(0xFFFFD700)
                                                                    2 -> Color(0xFFC0C0C0)
                                                                    3 -> Color(0xFFCD7F32)
                                                                    else -> MaterialTheme.colorScheme.primary
                                                                },
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                        }
                                                        Text(
                                                            text = when (stat.position) {
                                                                1 -> "WINNER"
                                                                2 -> "RUNNER-UP"
                                                                3 -> "3RD PLACE"
                                                                else -> "RANK #${stat.position}"
                                                            },
                                                            color = when (stat.position) {
                                                                1 -> Color(0xFFFFD700)
                                                                2 -> Color(0xFFC0C0C0)
                                                                3 -> Color(0xFFCD7F32)
                                                                else -> MaterialTheme.colorScheme.primary
                                                            },
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Black
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = stat.game.ifBlank { "Free Fire" },
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            Surface(
                                                color = Color(0xFF1B4E3E),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = stat.status.ifBlank { "COMPLETED" },
                                                    color = Color(0xFF4ECCA3),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = stat.tournamentTitle.ifBlank { "Championship Arena" },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.MyLocation,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "${stat.kills} Kills",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.SportsEsports,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = stat.matchNo,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Text(
                                                text = if (stat.winnings > 0) "+VT ${stat.winnings.toInt()} Won" else "+${stat.tokensEarned} Tokens",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF4ECCA3)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Referral Program Card
                ProfileSectionCard(
                    title = "Referral & Invite Program",
                    description = "Invite friends to Velorix and earn 50 bonus tokens for every verified referral."
                ) {
                    val refCode = currentUser.referralCode.ifEmpty { "VT" + (currentUser.id.takeLast(4).ifEmpty { "7890" }).uppercase() }

                    Column {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("YOUR REFERRAL CODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(refCode, fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("Referral Code", refCode)
                                            clipboard?.setPrimaryClip(clip)
                                            android.widget.Toast.makeText(context, "Referral Code Copied: $refCode", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            val sendIntent = android.content.Intent().apply {
                                                action = android.content.Intent.ACTION_SEND
                                                putExtra(android.content.Intent.EXTRA_TEXT, "Join Velorix Esports and compete for cash prize pools! Use my invite code: $refCode to get 50 FREE bonus tokens!")
                                                type = "text/plain"
                                            }
                                            context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Invite Code"))
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.background)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.background)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (currentUser.referredBy.isNotBlank()) {
                                        "Joined with invite code ${currentUser.referredBy}. Share your referral code with friends to earn 50 bonus tokens each when they sign up!"
                                    } else {
                                        "Share your referral code with friends! When they enter your code during registration, you both receive 50 bonus tokens."
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Notification Center Card
                ProfileSectionCard(title = "Notification Center", description = "Review past tournament reminders, match results, and system announcements.") {
                    Surface(
                        onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); onNavigateToNotifications() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_untitledui_bell),
                                    contentDescription = "Notifications",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "View Alert History",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (unreadNotifCount > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "$unreadNotifCount New",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "Custom room credentials, results & payouts",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Support & Reporting Section
                ProfileSectionCard(title = "Help & Customer Support", description = "24/7 Gemini AI assistant, instant complaint reporting, and ticket status.") {
                    Surface(
                        onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); onNavigateToSupport() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF16181D),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(
                                    Color(0xFF4E95FF).copy(alpha = 0.6f),
                                    Color(0xFF9B72CB).copy(alpha = 0.4f),
                                    Color(0xFFD96570).copy(alpha = 0.3f)
                                )
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GeminiStarLogo(size = 32.dp, animated = true)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Launch Gemini Assistant",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "Instant answers & screenshot analysis",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open",
                                tint = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Report Problem Button
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                showReportDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x22EF4444),
                            border = BorderStroke(1.dp, Color(0x55EF4444))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReportProblem,
                                    contentDescription = null,
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Report Issue",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                        }

                        // My Tickets Button
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                showMyReportsDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ConfirmationNumber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "My Tickets",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                
                // Admin Panel Situation Controls (Visible to Admin & dev accounts)
                if (currentUser.role.contains("admin", ignoreCase = true) || currentUser.phoneOrEmail.equals("anantisback47@gmail.com", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(24.dp))
                    ProfileSectionCard(
                        title = "Admin Panel: Situation Controls",
                        description = "Simulate and toggle app states (Ban, Maintenance, Suspension, Force Update, VPN Block) in real-time."
                    ) {
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                onOpenAdminSituations()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E1B18),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = "Admin",
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "System Situation Tester",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFF59E0B))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "ADMIN",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                    Text(
                                        "Test Ban, Maintenance & Update screens",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open",
                                    tint = Color(0xFFF59E0B)
                                )
                            }
                        }
                    }
                }

                // About Section Card (Format from Reference Screenshot 1)
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onNavigateToAbout()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF1E1718),
                    border = BorderStroke(1.dp, Color(0xFF2C2426))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF282022)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(com.example.R.drawable.ic_user_question),
                                contentDescription = "About",
                                tint = Color(0xFFE5E0E1),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(18.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "About",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF0ECEC)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "App info and licenses",
                                fontSize = 13.sp,
                                color = Color(0xFFA69E9F)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Danger Zone Card
                ProfileSectionCard(title = "Danger Zone", description = "Irreversible actions related to your account security.", isDanger = true) {
                    OutlinedButton(
                        onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); viewModel.exportUserData() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_cloud_download), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (currentUser.dataExported) "EXPORTED: RESYNC DATA" else "EXPORT ACCOUNT DATA")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); onLogout() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LOGOUT OF THIS DEVICE")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); showDeleteDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE91E63)),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFFE91E63).copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DELETE ACCOUNT")
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

        if (showAvatarDialog) {
            com.example.ui.components.AvatarSelectionDialog(
                currentAvatarUrl = if (isEditing) editedAvatarUrl else (user?.avatarUrl ?: ""),
                userId = user?.id ?: "",
                onDismiss = { showAvatarDialog = false },
                onSelectAvatar = { newUrl ->
                    editedAvatarUrl = newUrl
                    viewModel.updateAvatar(newUrl)
                }
            )
        }

        if (showDeleteDialog) {
            val reasons = listOf("Not using the app anymore", "Too many bugs", "Privacy concerns", "Other")
            VeloRixGlassAlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Account", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Column {
                        Text("We're sad to see you go. Please tell us why:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        reasons.forEach { reason ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); deleteReason = reason }
                                    .padding(vertical = 4.dp)
                            ) {
                                androidx.compose.material3.RadioButton(
                                    selected = (deleteReason == reason),
                                    onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); deleteReason = reason }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(reason, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        if (deleteReason == "Other" || deleteReason.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = deleteDetails,
                                onValueChange = { deleteDetails = it },
                                label = { Text("More details") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove);
                            viewModel.requestAccountDeletion(deleteReason, deleteDetails)
                            showDeleteDialog = false
                            onLogout()
                        },
                        enabled = deleteReason.isNotEmpty()
                    ) {
                        Text("Submit & Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); showDeleteDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            )
        }

        if (showReportDialog) {
            ReportProblemDialog(
                platformViewModel = viewModel,
                initialCategory = "GENERAL_SUPPORT",
                onDismiss = { showReportDialog = false }
            )
        }

        if (showMyReportsDialog) {
            MyReportsDialog(
                platformViewModel = viewModel,
                onOpenNewReport = { showReportDialog = true },
                onDismiss = { showMyReportsDialog = false }
            )
        }

        if (showAboutDeveloperModal) {
            com.example.ui.components.DeveloperPopupDialog(
                onDismissRequest = { showAboutDeveloperModal = false },
                onOpenSettings = {
                    showAboutDeveloperModal = false
                    onNavigateToSettings()
                }
            )
        }

        com.example.ui.components.ConfettiAnimation(isVisible = showConfetti, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun ProfileSectionCard(title: String, description: String, isDanger: Boolean = false, content: @Composable () -> Unit) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isDanger) Color(0xFFE91E63).copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isDanger) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current



    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        Text(value, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun StatBox(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current



    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
