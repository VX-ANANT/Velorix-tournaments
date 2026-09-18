package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R
import androidx.compose.runtime.*
import com.example.ui.components.stretchOverscroll
import com.example.ui.components.VeloRixGlassAlertDialog
import com.example.ui.components.ReportProblemDialog
import com.example.ui.components.MyReportsDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.viewmodel.ChatSession
import com.example.ui.viewmodel.CustomerSupportViewModel
import com.example.ui.viewmodel.PlatformViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val imageUri: Uri? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// Official Google Gemini brand colors
val GeminiBlue = Color(0xFF4285F4)
val GeminiPurple = Color(0xFF9B72CB)
val GeminiPink = Color(0xFFD96570)
val GeminiOrange = Color(0xFFF4A261)
val GeminiDarkBg = Color(0xFF0C0D10)
val GeminiPillBg = Color(0xFF1E1F22)
val GeminiBorderColor = Color(0x33FFFFFF)
val GeminiUserMsgBg = Color(0xFF282A2D)

val GeminiGradientBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF4E95FF),
        Color(0xFF8E7CF7),
        Color(0xFFD96570),
        Color(0xFFF4A261)
    )
)

/**
 * Official Google Gemini Star Logo utilizing the exact uploaded brand asset
 */
@Composable
fun GeminiStarLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    animated: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gemini_star")
    val pulseScale by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Image(
        painter = painterResource(id = R.drawable.ic_gemini_brand),
        contentDescription = "Gemini Logo",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSupportScreen(
    platformViewModel: PlatformViewModel? = null,
    supportViewModel: CustomerSupportViewModel = viewModel(),
    onNavigateBack: (() -> Unit)? = null
) {
    var messageText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showKeyDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showMyReportsDialog by remember { mutableStateOf(false) }
    var initialReportCategory by remember { mutableStateOf("GENERAL_SUPPORT") }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val chatHistory by supportViewModel.chatMessages.collectAsState()
    val sessions by supportViewModel.sessions.collectAsState()
    val currentSessionId by supportViewModel.currentSessionId.collectAsState()
    val isSending by supportViewModel.isSending.collectAsState()
    val customApiKey by supportViewModel.customApiKey.collectAsState()
    val selectedModel by supportViewModel.selectedModel.collectAsState()
    val useThinking by supportViewModel.useThinking.collectAsState()
    val cooldownSeconds by supportViewModel.cooldownSeconds.collectAsState()
    val autoClearOnExit by supportViewModel.autoClearOnExit.collectAsState()
    val errorMessage by supportViewModel.errorMessage.collectAsState()

    val userState = platformViewModel?.userState?.collectAsState()?.value
    val tournaments = platformViewModel?.tournaments?.collectAsState()?.value ?: emptyList()

    val registeredCount = tournaments.count { it.joined }
    val userDisplayName = remember(userState) {
        userState?.username?.takeIf { it.isNotBlank() && it != "Player" && it != "User" } ?: "Player"
    }

    // RLS: Bind user ID whenever authenticated user changes so history is 100% isolated per user ID
    LaunchedEffect(userState?.id) {
        supportViewModel.bindUser(userState?.id)
    }

    // Auto-clear on self-backing / leaving screen composition
    DisposableEffect(Unit) {
        onDispose {
            supportViewModel.onScreenExit()
        }
    }

    // Handle system back navigation (self-backing)
    androidx.activity.compose.BackHandler(enabled = true) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            supportViewModel.onScreenExit()
            onNavigateBack?.invoke()
        }
    }

    val userContextSummary = remember(userState, registeredCount) {
        if (userState != null) {
            "User: ${userState.username} (ID: ${userState.id}), Free Fire ID: ${if (userState.freeFireId.isBlank()) "Not linked" else userState.freeFireId}, Wallet: ₹${userState.balance}, Joined Matches: $registeredCount"
        } else null
    }

    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    LaunchedEffect(chatHistory.size, isSending) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    val modelLabel = when (selectedModel) {
        "gemini-3.7-flash" -> "Gemini 3.7 Flash"
        "gemini-3.1-pro-preview" -> "Pro Extended"
        "gemini-3.1-flash-lite-preview" -> "Flash Lite"
        else -> "Gemini 3.7 Flash"
    }

    // Modal Navigation Drawer for Chat History & Sessions
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(310.dp)
                    .fillMaxHeight(),
                drawerContainerColor = Color(0xFF131417),
                drawerContentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                ) {
                    // Drawer Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GeminiStarLogo(size = 28.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Gemini AI",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF4ADE80),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        "RLS Isolated per User",
                                        fontSize = 10.sp,
                                        color = Color(0xFF4ADE80)
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { scope.launch { drawerState.close() } }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Drawer",
                                tint = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // New Chat Action Button
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            supportViewModel.startNewChat()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF1F2125),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_untitledui_plus),
                                contentDescription = "New Chat",
                                tint = GeminiBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "New chat",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Your Isolated Sessions",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    // Sessions List
                    val activeSessions = sessions.filter { it.messages.isNotEmpty() }
                    if (activeSessions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No chats for this account",
                                fontSize = 13.sp,
                                color = Color.Gray.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).stretchOverscroll(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(activeSessions, key = { it.id }) { session ->
                                val isSelected = session.id == currentSessionId
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        supportViewModel.switchSession(session.id)
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF282A2E) else Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_untitledui_message_chat_circle),
                                            contentDescription = null,
                                            tint = if (isSelected) GeminiBlue else Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = session.title,
                                            color = if (isSelected) Color.White else Color(0xFFD0D0D0),
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                supportViewModel.deleteSession(session.id)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_untitledui_x_close),
                                                contentDescription = "Delete",
                                                tint = Color.Gray.copy(alpha = 0.6f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Official Support Reports & Tickets in Drawer
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            scope.launch { drawerState.close() }
                            showMyReportsDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E2026),
                        border = BorderStroke(1.dp, GeminiBlue.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ConfirmationNumber,
                                    contentDescription = null,
                                    tint = GeminiBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "My Support Tickets",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            scope.launch { drawerState.close() }
                            initialReportCategory = "GENERAL_SUPPORT"
                            showReportDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x22F59E0B),
                        border = BorderStroke(1.dp, Color(0x55F59E0B))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.ReportProblem,
                                contentDescription = null,
                                tint = Color(0xFFFCD34D),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Submit Manual Report",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFCD34D)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 10.dp))

                    // Auto-Clear on Exit Option
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF181A1F),
                        border = BorderStroke(1.dp, Color(0x22FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Auto-Clear on Back",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    "Wipes search on exit",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = autoClearOnExit,
                                onCheckedChange = { supportViewModel.setAutoClearOnExit(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = GeminiBlue,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF282A2E)
                                ),
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Wipe All History Button
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            supportViewModel.clearAllHistoryForUser()
                            Toast.makeText(context, "All chat & search history cleared!", Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x22EF4444),
                        border = BorderStroke(1.dp, Color(0x44EF4444))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_alert),
                                contentDescription = "Clear All History",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Wipe Account History",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFCA5A5)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // User Specific Identification Card in Drawer
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1B1D22),
                        border = BorderStroke(1.dp, Color(0x22FFFFFF))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(GeminiBlue, GeminiPurple))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        userDisplayName.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            userDisplayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_untitledui_check_circle),
                                            contentDescription = "Verified",
                                            tint = GeminiBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        if (userState != null && userState.freeFireId.isNotBlank()) "FF UID: ${userState.freeFireId}" else "Free Fire Account",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                IconButton(
                                    onClick = { showKeyDialog = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_untitledui_settings),
                                        contentDescription = "Settings",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (userState != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF141518))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Wallet Balance",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        "₹${userState.balance}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4ADE80)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GeminiDarkBg)
        ) {
            // Subtle ambient blue/indigo gradient glow at bottom (matching screenshot)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x1F1A2A5A),
                                Color(0x40101C3D),
                                Color(0x70091124)
                            )
                        )
                    )
            )

            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: 2-bar menu icon (exact Gemini app icon)
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    scope.launch { drawerState.open() }
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(18.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.dp)
                                            .background(Color.White, RoundedCornerShape(1.dp))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.dp)
                                            .background(Color.White, RoundedCornerShape(1.dp))
                                    )
                                }
                            }

                            // Center: Model selector dropdown pill ("Gemini 3.5 Flash ▾")
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { showModelMenu = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = modelLabel,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Select Model",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showModelMenu,
                                    onDismissRequest = { showModelMenu = false },
                                    modifier = Modifier
                                        .background(Color(0xFF1E1F22))
                                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Rounded.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = GeminiBlue,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text("Gemini 3.7 Flash", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                    Text("Next-gen speed & hybrid reasoning support", fontSize = 11.sp, color = Color.Gray)
                                                }
                                            }
                                        },
                                        onClick = {
                                            supportViewModel.setModel("gemini-3.7-flash")
                                            showModelMenu = false
                                        },
                                        leadingIcon = {
                                            if (selectedModel == "gemini-3.7-flash") {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = GeminiBlue)
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Bolt,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFBBF24),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text("Gemini 3.5 Flash", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                    Text("Fast, real-time responses for esports Q&A", fontSize = 11.sp, color = Color.Gray)
                                                }
                                            }
                                        },
                                        onClick = {
                                            supportViewModel.setModel("gemini-3.5-flash")
                                            showModelMenu = false
                                        },
                                        leadingIcon = {
                                            if (selectedModel == "gemini-3.5-flash") {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = GeminiBlue)
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Psychology,
                                                    contentDescription = null,
                                                    tint = Color(0xFFA78BFA),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text("Gemini 3.1 Pro (Extended)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                    Text("Deep reasoning for complex tournament strategies", fontSize = 11.sp, color = Color.Gray)
                                                }
                                            }
                                        },
                                        onClick = {
                                            supportViewModel.setModel("gemini-3.1-pro-preview")
                                            showModelMenu = false
                                        },
                                        leadingIcon = {
                                            if (selectedModel == "gemini-3.1-pro-preview") {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = GeminiBlue)
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Speed,
                                                    contentDescription = null,
                                                    tint = Color(0xFF38BDF8),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text("Gemini 3.1 Flash Lite", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                    Text("Lightweight high-speed verification", fontSize = 11.sp, color = Color.Gray)
                                                }
                                            }
                                        },
                                        onClick = {
                                            supportViewModel.setModel("gemini-3.1-flash-lite-preview")
                                            showModelMenu = false
                                        },
                                        leadingIcon = {
                                            if (selectedModel == "gemini-3.1-flash-lite-preview") {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = GeminiBlue)
                                            }
                                        }
                                    )
                                }
                            }

                            // Right: Report Issue + Clear Search Button + New Chat + Close button
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        showReportDialog = true
                                    },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReportProblem,
                                        contentDescription = "Report Problem",
                                        tint = Color(0xFFFCA5A5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        supportViewModel.clearChat()
                                        Toast.makeText(context, "Search & chat cleared", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_untitledui_trash),
                                        contentDescription = "Clear Chat",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        supportViewModel.startNewChat()
                                    },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_untitledui_plus),
                                        contentDescription = "New Chat",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (onNavigateBack != null) {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            supportViewModel.onScreenExit()
                                            onNavigateBack()
                                        },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_untitledui_x_close),
                                            contentDescription = "Close",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    val density = LocalDensity.current
                    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
                    
                    // Floating Gemini Capsule Writing Bar (clean messaging layout)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = if (isImeVisible) 8.dp else 18.dp
                            )
                    ) {
                        // Rate Limit & Error Banner
                        AnimatedVisibility(
                            visible = errorMessage != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x33EF4444),
                                border = BorderStroke(1.dp, Color(0x66EF4444))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = errorMessage ?: "",
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { supportViewModel.clearError() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_untitledui_x_close),
                                            contentDescription = "Dismiss",
                                            tint = Color(0xFFFCA5A5),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Image attachment preview strip if selected
                        AnimatedVisibility(
                            visible = selectedImageUri != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1E2024))
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Attachment",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Screenshot Attached",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        "Gemini will verify UID/Receipt details",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                IconButton(onClick = { selectedImageUri = null }) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_untitledui_x_close),
                                        contentDescription = "Remove",
                                        tint = Color.Red.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        // Floating Gemini Capsule Writing Bar (exact reference UI)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 58.dp),
                            shape = RoundedCornerShape(32.dp),
                            color = GeminiPillBg,
                            border = BorderStroke(1.dp, GeminiBorderColor),
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left: Plus button for attachments
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        imagePickerLauncher.launch("image/*")
                                    },
                                    enabled = !isSending && cooldownSeconds == 0,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_untitledui_plus),
                                        contentDescription = "Add Attachment",
                                        tint = if (selectedImageUri != null) GeminiBlue else Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Center: Text input field ("Ask Gemini")
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (messageText.isEmpty()) {
                                        Text(
                                            text = if (cooldownSeconds > 0) "Rate limit active (wait ${cooldownSeconds}s)..." else "Ask Gemini",
                                            color = if (cooldownSeconds > 0) Color(0xFFFCA5A5) else Color.Gray,
                                            fontSize = 16.sp
                                        )
                                    }
                                    BasicTextField(
                                        value = messageText,
                                        onValueChange = { messageText = it },
                                        textStyle = TextStyle(
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            lineHeight = 22.sp
                                        ),
                                        cursorBrush = SolidColor(GeminiBlue),
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !isSending && cooldownSeconds == 0,
                                        maxLines = 4
                                    )
                                }

                                // Right: Mic icon
                                IconButton(
                                    onClick = {
                                        Toast.makeText(context, "Voice input ready. Tap keyboard mic to speak!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Voice Input",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(2.dp))

                                // Right: Waveform / Send Action Button in Circular Badge
                                val hasInput = messageText.isNotBlank() || selectedImageUri != null
                                val canSend = hasInput && !isSending && cooldownSeconds == 0
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                cooldownSeconds > 0 -> Color(0xFF374151)
                                                hasInput && !isSending -> GeminiBlue
                                                else -> Color(0xFF282E47)
                                            }
                                        )
                                        .clickable(
                                            enabled = canSend,
                                            onClick = {
                                                val msg = messageText
                                                val uri = selectedImageUri
                                                messageText = ""
                                                selectedImageUri = null
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                supportViewModel.sendMessage(
                                                    userText = msg,
                                                    attachedImageUri = uri,
                                                    userContextSummary = userContextSummary
                                                )
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (cooldownSeconds > 0) {
                                        Text(
                                            text = "${cooldownSeconds}s",
                                            color = Color(0xFFFCA5A5),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else if (hasInput) {
                                        Icon(
                                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_mail_send),
                                            contentDescription = "Send",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        // Gemini Waveform Audio Icon
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = "Gemini Live Audio",
                                            tint = Color(0xFFA5B4FC),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (chatHistory.isEmpty()) {
                        // Empty State matching reference screenshot:
                        // Centered Gemini star + "What's next, [Username]?" + suggestion chips
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            GeminiStarLogo(size = 46.dp, animated = true)

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "What's next, $userDisplayName?",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            // Quick suggestion chips with Material Icons
                            data class SuggestionItem(val icon: androidx.compose.ui.graphics.vector.ImageVector, val iconTint: Color, val text: String, val isReport: Boolean = false)

                            val suggestions = listOf(
                                SuggestionItem(Icons.Rounded.ReportProblem, Color(0xFFF87171), "Report a problem to Admin panel", isReport = true),
                                SuggestionItem(Icons.Rounded.SportsEsports, Color(0xFF60A5FA), "Link Free Fire UID & verify stats"),
                                SuggestionItem(Icons.Rounded.EmojiEvents, Color(0xFFFBBF24), "How do I get Room ID & Password?"),
                                SuggestionItem(Icons.Rounded.AccountBalanceWallet, Color(0xFF4ADE80), "How to withdraw tournament prizes?"),
                                SuggestionItem(Icons.Rounded.ReceiptLong, Color(0xFFA78BFA), "Verify my payment transaction receipt")
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                suggestions.forEach { item ->
                                    Surface(
                                        onClick = {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            if (item.isReport) {
                                                showReportDialog = true
                                            } else {
                                                supportViewModel.sendMessage(
                                                    userText = item.text,
                                                    userContextSummary = userContextSummary
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0x331E1F22),
                                        border = BorderStroke(1.dp, Color(0x22FFFFFF))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = null,
                                                tint = item.iconTint,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = item.text,
                                                color = Color(0xFFD4D4D8),
                                                fontSize = 13.5.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Chat Messages List
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .stretchOverscroll()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(chatHistory, key = { it.id }) { message ->
                                if (message.isUser) {
                                    // User message bubble (Right aligned, dark rounded pill)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Surface(
                                            modifier = Modifier.widthIn(max = 290.dp),
                                            shape = RoundedCornerShape(
                                                topStart = 20.dp,
                                                topEnd = 20.dp,
                                                bottomStart = 20.dp,
                                                bottomEnd = 6.dp
                                            ),
                                            color = GeminiUserMsgBg,
                                            border = BorderStroke(1.dp, Color(0x22FFFFFF))
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                if (message.imageUri != null) {
                                                    AsyncImage(
                                                        model = message.imageUri,
                                                        contentDescription = "Screenshot",
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .heightIn(max = 180.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .padding(bottom = 8.dp),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                                Text(
                                                    text = message.text,
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    lineHeight = 22.sp
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Gemini message bubble (Left aligned with Gemini Star icon)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        GeminiStarLogo(
                                            size = 24.dp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = message.text,
                                                color = Color(0xFFF1F3F4),
                                                fontSize = 15.sp,
                                                lineHeight = 23.sp
                                            )

                                            // Action buttons (Copy, Share feedback)
                                            Row(
                                                modifier = Modifier
                                                    .padding(top = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("Gemini Response", message.text))
                                                        Toast.makeText(context, "Copied response to clipboard", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_untitledui_copy),
                                                        contentDescription = "Copy",
                                                        tint = Color.Gray,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        Toast.makeText(context, "Thanks for your feedback!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.ThumbUp,
                                                        contentDescription = "Helpful",
                                                        tint = Color.Gray,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Thinking / Loading state
                            if (isSending) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        GeminiStarLogo(size = 24.dp, animated = true)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = if (useThinking) "Gemini is analyzing with deep reasoning..." else "Gemini is typing...",
                                            color = Color.Gray,
                                            fontSize = 14.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Key Settings Dialog
    if (showKeyDialog) {
        var inputKey by remember { mutableStateOf(customApiKey) }
        VeloRixGlassAlertDialog(
            onDismissRequest = { showKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GeminiStarLogo(size = 24.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Gemini API Key", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column {
                    Text(
                        "Configure your Google Gemini API key to enable 24/7 AI tournament assistance & screenshot verification.",
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = inputKey,
                        onValueChange = { inputKey = it },
                        label = { Text("API Key") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GeminiBlue,
                            unfocusedBorderColor = Color(0x44FFFFFF)
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = GeminiBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Get free API keys at: https://aistudio.google.com/",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GeminiBlue
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        supportViewModel.saveApiKey(inputKey)
                        showKeyDialog = false
                    }
                ) {
                    Text("Save Key", color = GeminiBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showKeyDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Manual Report Submission Dialog
    if (showReportDialog && platformViewModel != null) {
        ReportProblemDialog(
            platformViewModel = platformViewModel,
            initialCategory = initialReportCategory,
            onDismiss = { showReportDialog = false }
        )
    }

    // My Reports / Tickets Status Dialog
    if (showMyReportsDialog && platformViewModel != null) {
        MyReportsDialog(
            platformViewModel = platformViewModel,
            onOpenNewReport = { showReportDialog = true },
            onDismiss = { showMyReportsDialog = false }
        )
    }
}

