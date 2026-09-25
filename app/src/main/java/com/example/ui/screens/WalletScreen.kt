package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.example.R
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.example.ui.components.stretchOverscroll
import com.example.ui.components.VeloRixGlassAlertDialog
import com.example.ui.components.VeloRixGlassDialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.ui.components.VeloRixButton

import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset



import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed

import com.example.ui.viewmodel.PlatformViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(viewModel: PlatformViewModel) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current



    val user by viewModel.userState.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val tournaments by viewModel.tournaments.collectAsState()
    val walletBreakdown by viewModel.walletBreakdown.collectAsState()
    val isRefreshing by viewModel.isRefreshingWallet.collectAsState()
    val actionCooldowns by viewModel.actionCooldownSeconds.collectAsState()
    val withdrawCooldown = maxOf(actionCooldowns["wallet_withdraw"] ?: 0, actionCooldowns["wallet_withdrawal"] ?: 0)
    val depositCooldown = actionCooldowns["wallet_deposit"] ?: 0
    val convertCooldown = actionCooldowns["token_convert"] ?: 0

    val userBalance = user?.balance ?: 0.0

    val upcomingPaidTournaments = remember(tournaments) {
        tournaments.filter { it.entryFee > 0.0 && !it.joined && !it.isFull }
    }
    val minEntryFee = remember(upcomingPaidTournaments) {
        upcomingPaidTournaments.minOfOrNull { it.entryFee } ?: 20.0
    }
    val isLowBalance = remember(userBalance, minEntryFee) {
        userBalance < minEntryFee
    }
    val requiredDeficit = remember(userBalance, minEntryFee) {
        (minEntryFee - userBalance).coerceAtLeast(10.0)
    }

    var showWithdrawDialog by remember { mutableStateOf(false) }
    var withdrawAmount by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var lastWithdrawnAmount by remember { mutableStateOf("") }
    var showWalletConvertDialog by remember { mutableStateOf(false) }
    var convertInput by remember { mutableStateOf("") }

    var showUpiQrDialog by remember { mutableStateOf(false) }
    var selectedTopupAmount by remember { mutableStateOf(0.0) }
    var showDepositSuccessDialog by remember { mutableStateOf(false) }
    var lastDepositedAmount by remember { mutableStateOf(0.0) }
    var lastDepositedUtr by remember { mutableStateOf("") }

    val isAnyPopupOpen = showWithdrawDialog || showReceiptDialog || showUpiQrDialog || showWalletConvertDialog || showDepositSuccessDialog
    val bgBlurRadius by animateDpAsState(
        targetValue = if (isAnyPopupOpen) 22.dp else 0.dp,
        animationSpec = tween(durationMillis = 280, easing = LinearOutSlowInEasing),
        label = "bg_gaussian_blur"
    )

    val context = androidx.compose.ui.platform.LocalContext.current
    
    val launchUpiPayment = { amount: Double ->
        if (amount > 0) {
            selectedTopupAmount = amount
            showUpiQrDialog = true
        }
    }

    val presetAmounts = listOf(50.0, 100.0, 200.0, 500.0)

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .blur(radius = bgBlurRadius)
                .stretchOverscroll()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // App Header Title
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.velorix_logo_image),
                            contentDescription = "Logo",
                            modifier = Modifier.size(36.dp).padding(end = 6.dp)
                        )
                        Text(
                            text = "FUNDS MANAGER",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.secondary,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Text(
                        text = "MY WALLET",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Big Header Wallet Balance card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .testTag("virtual_wallet_card"),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        Color.Black
                                    )
                                )
                            )
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha=0.5f), RoundedCornerShape(24.dp))
                    ) {
                        // Glossy geometric overlay
                        Canvas(modifier = Modifier.fillMaxSize()) {
                             drawCircle(
                                 color = Color.White.copy(alpha = 0.05f),
                                 radius = 300f,
                                 center = Offset(this.size.width, 0f)
                             )
                             drawCircle(
                                 color = Color.White.copy(alpha = 0.03f),
                                 radius = 200f,
                                 center = Offset(0f, this.size.height)
                             )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "VELORIX VIRTUAL CARD",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.velorix_logo_image),
                                    contentDescription = "Chip",
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "AVAILABLE VT TOKENS (WALLET BALANCE)",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    com.example.ui.components.AnimatedRollingCounter(
                                        targetValue = user?.balance?.toInt() ?: 0,
                                        prefix = "VT ",
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }

                                Button(
                                    onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); showWithdrawDialog = true },
                                    enabled = withdrawCooldown == 0,
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("withdraw_action_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.15f),
                                        contentColor = Color.White,
                                        disabledContainerColor = Color.White.copy(alpha = 0.05f),
                                        disabledContentColor = Color.White.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(50.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = if (withdrawCooldown > 0) "WAIT ${withdrawCooldown}S" else "WITHDRAW",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Breakdown chips: Deposited vs Winnings
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "DEPOSITED (MATCHES)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.65f),
                                            letterSpacing = 0.5.sp
                                        )
                                        com.example.ui.components.AnimatedRollingCounter(
                                            targetValue = walletBreakdown.deposited.toInt(),
                                            prefix = "VT ",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NeonGreen.copy(alpha = 0.15f))
                                        .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "WINNINGS (WITHDRAWABLE)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonGreen,
                                            letterSpacing = 0.5.sp
                                        )
                                        com.example.ui.components.AnimatedRollingCounter(
                                            targetValue = walletBreakdown.winnings.toInt(),
                                            prefix = "VT ",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = NeonGreen
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Sub-row displaying Tokens and Conversion
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_untitledui_coins),
                                        contentDescription = "Tokens",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Tokens (10 = 1 VT):",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "${user?.tokens ?: 0} Tokens",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            // Low-balance notification banner for tournament entry fee
            if (isLowBalance) {
                item {
                    LowBalanceNotificationCard(
                        userBalance = userBalance,
                        minEntryFee = minEntryFee,
                        requiredDeficit = requiredDeficit,
                        upcomingCount = upcomingPaidTournaments.size,
                        onQuickAddFunds = { amount ->
                            launchUpiPayment(amount)
                        }
                    )
                }
            }

            // SECTION 1: ADD FUNDS PRESET GRID
            item {
                Text(
                    text = "QUICK ADD FUNDS (UPI)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetAmounts.forEach { amount ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); launchUpiPayment(amount) }
                                .testTag("add_funds_preset_${amount.toInt()}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_bag_plus),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "VT ${amount.toInt()}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                
                // Custom Amount Input
                var customAmount by remember { mutableStateOf("") }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customAmount,
                        onValueChange = { customAmount = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_add_funds_input"),
                        label = { Text("Custom Amount (VT)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Button(
                        onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove);
                            val amt = customAmount.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                launchUpiPayment(amt)
                                customAmount = ""
                            }
                        },
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("custom_add_funds_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("ADD", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // TOKEN TO VT CONVERTER SECTION
            item {
                val userTokens = user?.tokens ?: 0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "TOKEN EXCHANGE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Rate: 10 Tokens = 1 VT Token",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "$userTokens Tokens",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Earned Tokens from Daily Missions and Daily Streaks can be instantly converted to playable VT Tokens in your wallet balance.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                convertInput = if (userTokens >= 10) (userTokens - (userTokens % 10)).toString() else "10"
                                showWalletConvertDialog = true
                            },
                            enabled = userTokens >= 10,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = if (userTokens >= 10) "Convert Tokens to VT (+${userTokens / 10} VT)" else "Need 10+ Tokens to Convert",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.background
                            )
                        }
                    }
                }

                if (showWalletConvertDialog) {
                    VeloRixGlassAlertDialog(
                        onDismissRequest = { showWalletConvertDialog = false },
                        title = { Text("CONVERT TOKENS TO VT", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface) },
                        text = {
                            Column {
                                Text("Exchange Rate: 10 Tokens = 1 VT Token", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                Text("Available: $userTokens Tokens", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = convertInput,
                                    onValueChange = { convertInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Tokens to Convert") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                val entered = convertInput.toIntOrNull() ?: 0
                                val vtAmt = entered / 10
                                Spacer(Modifier.height(8.dp))
                                Text("You will receive: $vtAmt VT Tokens", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val amt = convertInput.toIntOrNull() ?: 0
                                    if (amt >= 10) {
                                        viewModel.convertTokensToVt(amt) { success ->
                                            if (success) {
                                                com.example.util.VeloRixHaptics.paymentSuccess(context, haptic)
                                                com.example.audio.SoundEffectManager.getInstance(context).playBeatItPower()
                                                showWalletConvertDialog = false
                                            }
                                        }
                                    }
                                },
                                enabled = (convertInput.toIntOrNull() ?: 0) >= 10 && (convertInput.toIntOrNull() ?: 0) <= userTokens,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.background)
                            ) {
                                Text("CONFIRM CONVERT", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showWalletConvertDialog = false }) {
                                Text("CANCEL", color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    )
                }
            }

            // REFERRAL COMMISSION SECTION
            item {
                Spacer(modifier = Modifier.height(24.dp))
                val referralCount = user?.referralCount ?: 0
                val currentCommissionTier = when {
                    referralCount <= 1 -> "10%"
                    referralCount in 2..4 -> "12%"
                    else -> "15% (MAX)"
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SQUAD REFERRAL REWARDS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSecondaryContainer, letterSpacing = 1.5.sp)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("EARN 10-15% COMMISSION", fontSize = 9.sp, fontWeight = FontWeight.Black, color = NeonGreen)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Referral stats overview
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("ACTIVE REFERRALS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$referralCount", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("CURRENT TIER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(currentCommissionTier, fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("COMMISSION EARNED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${(user?.referralEarnings ?: 0.0).toInt()} VT", fontSize = 16.sp, fontWeight = FontWeight.Black, color = NeonGreen)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Commission Tier Breakdown Table
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "DYNAMIC DEPOSIT COMMISSION TIERS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• 1st Referred Squadmate:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("10% on every deposit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• 2nd - 4th Squadmates:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("12% on every deposit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• 5+ Squadmates (Max Limit):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("15% MAX CAP", fontSize = 11.sp, fontWeight = FontWeight.Black, color = NeonGreen)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val refCode = user?.referralCode?.ifEmpty { "VRX-${(user?.username ?: "USER").take(4).uppercase()}-${(user?.id?.takeLast(4) ?: "7890").uppercase()}" } ?: ""
                        if (refCode.isNotEmpty()) {
                            Text("Your Unique Share Code:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = refCode,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.2.sp
                                    )
                                    Text(
                                        text = "VERIFIED UNIQUE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonGreen
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        com.example.util.VeloRixHaptics.credentialCopied(context, haptic)
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Referral Code", refCode)
                                        clipboard.setPrimaryClip(clip)
                                        android.widget.Toast.makeText(context, "Copied code: $refCode", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Code")
                                }
                                
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Join Velorix Esports")
                                            putExtra(android.content.Intent.EXTRA_TEXT, "Join Velorix Esports using my referral code $refCode during registration to compete in tournaments and earn rewards!")
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Referral Code"))
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Share Link")
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        if (!user?.referredBy.isNullOrEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                Text("Referred by: ${user?.referredBy} (Linked on registration)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NeonGreen)
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_lock),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text("Referral codes can only be linked during initial registration to maintain fair play.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            // SECTION 2: TRANSACTION HISTORY TITLE & FILTERS
            item {
                var selectedTxFilter by remember { mutableStateOf("ALL") }
                
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TRANSACTION LOG",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "${transactions.size} records",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ALL", "WITHDRAWAL", "ADD_FUNDS", "REWARDS").forEach { filter ->
                            val isSelected = selectedTxFilter == filter
                            val label = when(filter) {
                                "WITHDRAWAL" -> "Withdrawals"
                                "ADD_FUNDS" -> "Deposits"
                                "REWARDS" -> "Rewards"
                                else -> "All"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTxFilter = filter },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // Historic transaction records LazyList
            val filteredTransactions = transactions.filter { tx ->
                true // show all or filter if needed
            }

            if (transactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_bag_off),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "NO RECENT TRANSACTIONS FOUND",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    TransactionRow(tx)
                }
            }
        }

        // DYNAMIC WITHDRAW POPUP DIALOG
        if (showWithdrawDialog) {
            val parsedAmt = withdrawAmount.toDoubleOrNull() ?: 0.0
            val isUpiValid = upiId.trim().length >= 5 && upiId.contains("@")
            val exceedsWinnings = parsedAmt > walletBreakdown.winnings
            val belowMin = parsedAmt > 0 && parsedAmt < 50.0
            val exceedsMax = parsedAmt > 10000.0
            val isFormValid = isUpiValid && parsedAmt >= 50.0 && parsedAmt <= walletBreakdown.winnings && parsedAmt <= 10000.0

            VeloRixGlassAlertDialog(
                onDismissRequest = { showWithdrawDialog = false },
                title = {
                    Text(
                        text = "WITHDRAW WINNINGS",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Withdrawable Winnings Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = NeonGreen.copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("WITHDRAWABLE WINNINGS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                    Text("VT ${walletBreakdown.winnings.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = NeonGreen)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Deposited balance (VT ${walletBreakdown.deposited.toInt()}) is reserved for tournament entries. Only tournament prizes can be withdrawn.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = upiId,
                            onValueChange = { upiId = it.trim() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .testTag("upi_id_input"),
                            label = { Text("UPI ID (e.g. name@okhdfc)") },
                            isError = upiId.isNotBlank() && !isUpiValid,
                            supportingText = if (upiId.isNotBlank() && !isUpiValid) {
                                { Text("Enter a valid UPI ID with '@'", color = NeonRed, fontSize = 11.sp) }
                            } else null,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = withdrawAmount,
                            onValueChange = { withdrawAmount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_amount_input"),
                            label = { Text("Amount (VT) - Min 50") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = exceedsWinnings || belowMin || exceedsMax,
                            supportingText = {
                                if (exceedsWinnings) {
                                    Text("Exceeds withdrawable winnings (VT ${walletBreakdown.winnings.toInt()})", color = NeonRed, fontSize = 11.sp)
                                } else if (belowMin) {
                                    Text("Minimum withdrawal is VT 50", color = NeonRed, fontSize = 11.sp)
                                } else if (exceedsMax) {
                                    Text("Maximum single withdrawal is VT 10,000", color = NeonRed, fontSize = 11.sp)
                                } else {
                                    Text("Min: VT 50 | Max: VT 10,000 per request", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            val amt = withdrawAmount.toDoubleOrNull() ?: 0.0
                            if (isFormValid) {
                                viewModel.withdrawFunds(amt, upiId)
                                lastWithdrawnAmount = amt.toString()
                                showWithdrawDialog = false
                                withdrawAmount = ""
                                upiId = ""
                                showReceiptDialog = true
                            }
                        },
                        enabled = isFormValid && withdrawCooldown == 0,
                        modifier = Modifier.testTag("submit_withdraw_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.background)
                    ) {
                        Text(if (withdrawCooldown > 0) "WAIT ${withdrawCooldown}S" else "CONFIRM", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); showWithdrawDialog = false }) {
                        Text("CANCEL", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            )
        }

        if (showReceiptDialog) {
            VeloRixGlassAlertDialog(
                onDismissRequest = { showReceiptDialog = false },
                title = {
                    Text(
                        text = "WITHDRAWAL SUCCESS",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = NeonGreen
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Your funds have been successfully withdrawn.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Amount Withdrawn:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            Text("VT $lastWithdrawnAmount", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Platform Fee:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            Text("VT 0.00", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.4f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Sent:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            Text("VT $lastWithdrawnAmount", color = NeonGreen, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); 
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, "I just successfully withdrew VT $lastWithdrawnAmount from my Velorix Wallet!")
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share Receipt"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.background)
                    ) {
                        Text("SHARE RECEIPT", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); showReceiptDialog = false }) {
                        Text("CLOSE", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            )
        }

        if (showUpiQrDialog && selectedTopupAmount > 0) {
            com.example.ui.components.UpiPaymentQrDialog(
                amount = selectedTopupAmount,
                purpose = "Wallet Token Top-up",
                onDismiss = {
                    showUpiQrDialog = false
                    selectedTopupAmount = 0.0
                },
                onPaymentSuccess = { paidAmount, utr ->
                    viewModel.addWalletFunds(paidAmount, utrNumber = utr, paymentRef = utr)
                    lastDepositedAmount = paidAmount
                    lastDepositedUtr = utr
                    showUpiQrDialog = false
                    selectedTopupAmount = 0.0
                    showDepositSuccessDialog = true
                }
            )
        }

        if (showDepositSuccessDialog) {
            VeloRixGlassAlertDialog(
                onDismissRequest = { showDepositSuccessDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "PAYMENT CONFIRMED",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Transaction Receipt & Details",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "TOKENS GRANTED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "+${lastDepositedAmount.toInt()} VT",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "Amount Paid: ₹${lastDepositedAmount.toInt()}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Payment Mode:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text("Direct UPI (FamPay / QR)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("12-Digit UTR Ref:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text(lastDepositedUtr, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Status:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text("SUBMITTED / SUCCESS", color = Color(0xFF10B981), fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Your wallet history has been updated. You can now use these tokens to join tournaments immediately!",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            showDepositSuccessDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("DONE & CONTINUE", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
fun TransactionRow(tx: Transaction) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current



    val dateStr = remember(tx.timestamp) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(tx.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .testTag("transaction_row_${tx.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Cash icon indicator
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (tx.isPositive) NeonGreen.copy(alpha = 0.15f) else NeonRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (tx.isPositive) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_wallet_receive),
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CallMade,
                            contentDescription = null,
                            tint = NeonRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = tx.detail,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dateStr,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (tx.status != "SUCCESS") {
                            Text(
                                text = " • ${tx.status}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (tx.status == "PENDING") Color(0xFFFFA500) else NeonRed
                            )
                        }
                    }
                }
            }

            // Positive/negative green red amount display
            val prefix = if (tx.isPositive) "+" else "-"
            Text(
                text = "$prefix VT ${tx.amount.toInt()}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = if (tx.isPositive) NeonGreen else NeonRed
            )
        }
    }
}

@Composable
fun LowBalanceNotificationCard(
    userBalance: Double,
    minEntryFee: Double,
    requiredDeficit: Double,
    upcomingCount: Int,
    onQuickAddFunds: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var isDismissed by remember { mutableStateOf(false) }
    if (isDismissed) return

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_warning")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .testTag("low_balance_notification_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f)
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.error.copy(alpha = borderAlpha)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_alert),
                                contentDescription = "Low Balance Warning",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "INSUFFICIENT FUNDS ALERT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = MaterialTheme.colorScheme.error
                                ) {
                                    Text(
                                        text = "ACTION REQ",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Wallet balance insufficient for tournament entry",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    IconButton(
                        onClick = { isDismissed = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Current Balance: VT ${userBalance.toInt()} • Min Entry Fee: VT ${minEntryFee.toInt()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Deficit: Need VT ${requiredDeficit.toInt()} more to join ${if (upcomingCount > 0) "$upcomingCount active tournament(s)" else "upcoming matches"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onQuickAddFunds(requiredDeficit)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TOP-UP VT ${requiredDeficit.toInt()} VIA UPI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

