package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import com.example.ui.components.stretchOverscroll
import com.example.ui.components.ApplyDialogWindowBlur
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.LeaderboardPlayer
import com.example.ui.components.UserAvatar
import com.example.ui.theme.Bronze
import com.example.ui.theme.Gold
import com.example.ui.theme.Silver
import com.example.ui.viewmodel.PlatformViewModel

data class FounderTier(
    val id: String,
    val title: String,
    val priceInr: Double,
    val tokensReward: Int,
    val bonusPercent: String,
    val badgeName: String,
    val accentColor: Color,
    val isPopular: Boolean = false,
    val benefits: List<String>
)

val FounderTiers = listOf(
    FounderTier(
        id = "tier_50",
        title = "Starter Supporter",
        priceInr = 50.0,
        tokensReward = 60,
        bonusPercent = "+20% Bonus",
        badgeName = "Bronze Founder Crest",
        accentColor = Color(0xFFCD7F32),
        benefits = listOf(
            "60 VT Founder Tokens at launch",
            "Bronze Founder profile crest",
            "1x Standard Tournament entry pass",
            "Early access notification for Stable Release"
        )
    ),
    FounderTier(
        id = "tier_100",
        title = "Elite Contender",
        priceInr = 100.0,
        tokensReward = 150,
        bonusPercent = "+50% Bonus",
        badgeName = "Silver Founder Crest",
        accentColor = Color(0xFF94A3B8),
        isPopular = true,
        benefits = listOf(
            "150 VT Founder Tokens at launch",
            "Silver Founder profile crest & verified flair",
            "2x Complimentary Tournament entry passes",
            "Priority queue for custom room allotment"
        )
    ),
    FounderTier(
        id = "tier_500",
        title = "Champion Patron",
        priceInr = 500.0,
        tokensReward = 850,
        bonusPercent = "+70% Bonus",
        badgeName = "Gold Founder Crest",
        accentColor = Color(0xFFFFD700),
        benefits = listOf(
            "850 VT Founder Tokens at launch",
            "Gold Founder profile crest & custom killfeed banner",
            "Season 1 Grand Championship VIP Pass",
            "24/7 Priority VIP Concierge support",
            "Exclusive access to verified Pro Scrims"
        )
    ),
    FounderTier(
        id = "tier_1000",
        title = "Legendary Vanguard",
        priceInr = 1000.0,
        tokensReward = 2000,
        bonusPercent = "+100% 2x Bonus",
        badgeName = "Diamond Founder Crest",
        accentColor = Color(0xFF38BDF8),
        benefits = listOf(
            "2,000 VT Founder Tokens at launch (Double Tokens)",
            "Obsidian & Diamond animated Founder profile frame",
            "Lifetime 10% discount on all tournament entries",
            "Unlimited complimentary entry to Season 1 Grand Events",
            "Guaranteed slot reservation & Pro Gamer badge"
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(viewModel: PlatformViewModel) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.recordLeaderboardView()
    }

    val players by viewModel.leaderboard.collectAsState()
    val isRefreshing by viewModel.isRefreshingHome.collectAsState()
    val user by viewModel.userState.collectAsState()

    val prefs = remember { context.getSharedPreferences("velorix_stable_reg", Context.MODE_PRIVATE) }
    
    // Auto-detect founder status from user profile, email (service.veloxyra@gmail.com / anantisback47@gmail.com) or local prefs
    val isAnantAccount = user?.phoneOrEmail?.equals("service.veloxyra@gmail.com", ignoreCase = true) == true ||
            user?.phoneOrEmail?.equals("anantisback47@gmail.com", ignoreCase = true) == true
    val isUserFounder = user?.isFounder == true ||
            !user?.founderTier.isNullOrBlank() ||
            isAnantAccount ||
            prefs.getBoolean("pre_registered_stable", false)

    val currentTierId = user?.founderTier?.takeIf { it.isNotBlank() }
        ?: prefs.getString("registered_tier_id", null)
        ?: if (isAnantAccount) "tier_1000" else null

    var isPreRegistered by remember(isUserFounder, currentTierId) {
        mutableStateOf(isUserFounder && currentTierId != null)
    }
    var registeredTierId by remember(currentTierId) {
        mutableStateOf(currentTierId)
    }

    var showPassDetailsDialog by remember { mutableStateOf(false) }
    var showTierSelectionDialog by remember { mutableStateOf(false) }
    var showTierQrDialog by remember { mutableStateOf(false) }
    var selectedTierForPayment by remember { mutableStateOf<FounderTier?>(null) }
    var paymentAmountForTier by remember { mutableDoubleStateOf(0.0) }

    val activeTier = remember(registeredTierId, isPreRegistered) {
        FounderTiers.find { it.id == registeredTierId } ?: if (isPreRegistered) FounderTiers.last() else null
    }

    val isAnyPopupOpen = showPassDetailsDialog || showTierSelectionDialog || showTierQrDialog
    val bgBlurRadius by animateDpAsState(
        targetValue = if (isAnyPopupOpen) 22.dp else 0.dp,
        animationSpec = tween(durationMillis = 280, easing = LinearOutSlowInEasing),
        label = "leaderboard_bg_blur"
    )

    val launchUpiForTier: (FounderTier) -> Unit = { tier ->
        val curTier = FounderTiers.find { it.id == registeredTierId }
        val diff = if (curTier != null && tier.priceInr > curTier.priceInr) {
            tier.priceInr - curTier.priceInr
        } else {
            tier.priceInr
        }
        selectedTierForPayment = tier
        paymentAmountForTier = diff
        showTierQrDialog = true
    }

    val top1 = players.find { it.rank == 1 }
    val top2 = players.find { it.rank == 2 }
    val top3 = players.find { it.rank == 3 }
    val restOfPlayers = players.filter { it.rank > 3 }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshHomeData() },
        modifier = Modifier
            .fillMaxSize()
            .blur(radius = bgBlurRadius)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .stretchOverscroll()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.velorix_logo_image),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .padding(end = 6.dp)
                        )
                        Text(
                            text = "VELORIX CHAMPIONS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.secondary,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Text(
                        text = "LEADERBOARD",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (players.isEmpty()) {
                // Feature Notice Card
                item {
                    LeaderboardStableReleaseNotice()
                }

                // Professional Founder Pre-Registration Card
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    FounderPreRegisterCard(
                        isPreRegistered = isPreRegistered,
                        activeTier = activeTier,
                        onOpenTiers = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isPreRegistered && activeTier != null) {
                                showPassDetailsDialog = true
                            } else {
                                showTierSelectionDialog = true
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else {
                // Founder card for active season
                item {
                    FounderPreRegisterCard(
                        isPreRegistered = isPreRegistered,
                        activeTier = activeTier,
                        onOpenTiers = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isPreRegistered && activeTier != null) {
                                showPassDetailsDialog = true
                            } else {
                                showTierSelectionDialog = true
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Podium Section
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_untitledui_trophy),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "TOP 3 WARRIORS",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                top2?.let {
                                    PodiumColumn(
                                        player = it,
                                        podiumColor = Silver,
                                        height = 110,
                                        modifier = Modifier.weight(1f)
                                    )
                                } ?: Spacer(modifier = Modifier.weight(1f))
                                top1?.let {
                                    PodiumColumn(
                                        player = it,
                                        podiumColor = Gold,
                                        height = 150,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                } ?: Spacer(modifier = Modifier.weight(1.2f))
                                top3?.let {
                                    PodiumColumn(
                                        player = it,
                                        podiumColor = Bronze,
                                        height = 95,
                                        modifier = Modifier.weight(1f)
                                    )
                                } ?: Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 0.dp, end = 8.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "RANKING LIST",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "TOTAL EARNINGS",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                items(restOfPlayers, key = { "${it.rank}_${it.username}" }) { player ->
                    LeaderboardRow(player)
                }
            }
        }
    }

    if (showPassDetailsDialog && activeTier != null) {
        FounderPassDetailsDialog(
            tier = activeTier,
            user = user,
            onDismiss = { showPassDetailsDialog = false },
            onUpgradeTier = {
                showPassDetailsDialog = false
                showTierSelectionDialog = true
            }
        )
    }

    if (showTierSelectionDialog) {
        FounderTiersDialog(
            selectedTierId = registeredTierId,
            onDismiss = { showTierSelectionDialog = false },
            onViewDetails = {
                showTierSelectionDialog = false
                if (activeTier != null) {
                    showPassDetailsDialog = true
                }
            },
            onSelectTier = { tier ->
                showTierSelectionDialog = false
                launchUpiForTier(tier)
            }
        )
    }

    if (showTierQrDialog && selectedTierForPayment != null) {
        val tier = selectedTierForPayment!!
        val isUpgrade = registeredTierId != null && tier.id != registeredTierId
        com.example.ui.components.UpiPaymentQrDialog(
            amount = if (paymentAmountForTier > 0.0) paymentAmountForTier else tier.priceInr,
            purpose = if (isUpgrade) "Upgrade to ${tier.title}" else "Pre-Registration ${tier.title}",
            onDismiss = {
                showTierQrDialog = false
                selectedTierForPayment = null
                paymentAmountForTier = 0.0
            },
            onPaymentSuccess = { paidAmount, txId ->
                viewModel.registerFounderPass(
                    tierId = tier.id,
                    tokensReward = tier.tokensReward,
                    priceInr = tier.priceInr,
                    paymentRef = txId
                )
                viewModel.addWalletFunds(tier.priceInr, utrNumber = txId, paymentRef = txId)
                isPreRegistered = true
                registeredTierId = tier.id
                prefs.edit()
                    .putBoolean("pre_registered_stable", true)
                    .putString("registered_tier_id", tier.id)
                    .putInt("reserved_tokens", tier.tokensReward)
                    .apply()

                showTierQrDialog = false
                selectedTierForPayment = null
                paymentAmountForTier = 0.0

                Toast.makeText(
                    context,
                    "Founder Pass Confirmed: ${tier.title} activated successfully!",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
}

@Composable
fun FounderPreRegisterCard(
    isPreRegistered: Boolean,
    activeTier: FounderTier?,
    onOpenTiers: () -> Unit
) {
    // Subtle dark slate/carbon luxury background with matte metallic gradient
    val carbonGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF161922),
            Color(0xFF0F1117)
        )
    )

    val refinedBorder = BorderStroke(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = if (isPreRegistered) {
                listOf(Color(0xFF059669), Color(0xFF10B981).copy(alpha = 0.5f), Color(0xFF047857))
            } else {
                listOf(
                    Color(0xFF334155),
                    Color(0xFF1E293B),
                    Color(0xFF475569).copy(alpha = 0.6f)
                )
            }
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(refinedBorder, RoundedCornerShape(20.dp))
            .clickable { onOpenTiers() }
            .testTag("founder_pre_register_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(carbonGradient)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Tag & Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isPreRegistered) Color(0xFF064E3B) else Color(0xFF1E293B)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = if (isPreRegistered) Icons.Default.CheckCircle else Icons.Default.Stars,
                            contentDescription = null,
                            tint = if (isPreRegistered) Color(0xFF34D399) else Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPreRegistered) "FOUNDER STATUS ACTIVE" else "EARLY PATRON PROGRAM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPreRegistered) Color(0xFF34D399) else Color(0xFFCBD5E1),
                            letterSpacing = 1.2.sp
                        )
                    }

                    Text(
                        text = if (isPreRegistered) "PASS ENROLLED" else "TIERS: ₹50 – ₹1000",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title and Subtitle
                Text(
                    text = if (isPreRegistered && activeTier != null) {
                        "Enrolled in ${activeTier.title}"
                    } else {
                        "Pre-Register for the Stable Release"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isPreRegistered && activeTier != null) {
                        "Your account is accredited with ${activeTier.tokensReward} reserved VT Tokens and ${activeTier.badgeName} on launch day."
                    } else {
                        "Support platform development to secure exclusive early supporter packages, launch-day bonus token reserves, and verified founder credentials."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick perks row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FounderPerkPill(
                        label = "Bonus Tokens",
                        icon = Icons.Default.FlashOn,
                        modifier = Modifier.weight(1f)
                    )
                    FounderPerkPill(
                        label = "Founder Crest",
                        icon = Icons.Default.WorkspacePremium,
                        modifier = Modifier.weight(1f)
                    )
                    FounderPerkPill(
                        label = "VIP Entry Passes",
                        icon = Icons.Default.MilitaryTech,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Button
                Button(
                    onClick = onOpenTiers,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPreRegistered) Color(0xFF0F766E) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isPreRegistered) Icons.Default.Check else Icons.Default.Payment,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPreRegistered) {
                                "View Founder Pass Details"
                            } else {
                                "Select Supporter Tier & Pre-Register"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FounderPerkPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E2430))
            .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FounderTiersDialog(
    selectedTierId: String?,
    onDismiss: () -> Unit,
    onViewDetails: () -> Unit = {},
    onSelectTier: (FounderTier) -> Unit
) {
    val currentTier = FounderTiers.find { it.id == selectedTierId }
    val currentTierIndex = FounderTiers.indexOfFirst { it.id == selectedTierId }

    var activeSelection by remember {
        mutableStateOf(currentTier ?: FounderTiers[1])
    }

    Dialog(onDismissRequest = onDismiss) {
        ApplyDialogWindowBlur()
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF20B0E14)),
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color(0x40FFFFFF),
                        Color(0x14FFFFFF),
                        Color(0x06FFFFFF)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Xiaomi Top Handle Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0x28FFFFFF))
                )

                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x18FFFFFF))
                                .border(BorderStroke(1.dp, Color(0x20FFFFFF)), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_iconsax_price),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "PRE-REGISTRATION TIERS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Founder Packages",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = (-0.3).sp
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Select a patron tier to pre-register and securely complete via UPI. All bonuses will be credited on launch day.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Tier Cards List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(FounderTiers, key = { it.id }) { tier ->
                        val tierIndex = FounderTiers.indexOfFirst { it.id == tier.id }
                        val isSelected = activeSelection.id == tier.id
                        val isAlreadyOwned = selectedTierId == tier.id
                        val isLowerTier = selectedTierId != null && currentTierIndex != -1 && tierIndex < currentTierIndex
                        val isHigherTier = selectedTierId != null && currentTierIndex != -1 && tierIndex > currentTierIndex
                        val upgradeDifference = if (isHigherTier && currentTier != null) (tier.priceInr - currentTier.priceInr).toInt() else 0

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) tier.accentColor else if (isAlreadyOwned) Color(0xFF10B981) else Color(0xFF1E2638)
                                    ),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { activeSelection = tier },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF171D2B) else Color(0xFF131722)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(tier.accentColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Stars,
                                                contentDescription = null,
                                                tint = tier.accentColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = tier.title,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isAlreadyOwned) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFF064E3B)
                                                    ) {
                                                        Text(
                                                            text = "CURRENT TIER",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF34D399),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                } else if (isLowerTier) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFF0C4A6E)
                                                    ) {
                                                        Text(
                                                            text = "PERKS INCLUDED",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF38BDF8),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                } else if (isHigherTier) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFF78350F)
                                                    ) {
                                                        Text(
                                                            text = "UPGRADE (+₹$upgradeDifference)",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFFBBF24),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = tier.badgeName,
                                                fontSize = 11.sp,
                                                color = tier.accentColor,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    // Price Badge
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isHigherTier) "+₹$upgradeDifference" else "₹${tier.priceInr.toInt()}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isHigherTier) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = tier.bonusPercent,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }

                                if (isLowerTier) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "All benefits fully covered in your ${currentTier?.title ?: "current tier"}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Benefits list
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    tier.benefits.forEach { benefit ->
                                        Row(
                                            verticalAlignment = Alignment.Top,
                                            modifier = Modifier.padding(vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "•",
                                                color = tier.accentColor,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                            Text(
                                                text = benefit,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Checkout Bar
                val activeIndex = FounderTiers.indexOfFirst { it.id == activeSelection.id }
                val isCurrentSelectionActive = selectedTierId == activeSelection.id
                val isCurrentSelectionLower = selectedTierId != null && currentTierIndex != -1 && activeIndex < currentTierIndex
                val isCurrentSelectionHigher = selectedTierId != null && currentTierIndex != -1 && activeIndex > currentTierIndex
                val upgradeCost = if (isCurrentSelectionHigher && currentTier != null) (activeSelection.priceInr - currentTier.priceInr).toInt() else activeSelection.priceInr.toInt()

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF161B26),
                    border = BorderStroke(1.dp, Color(0xFF263044))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = when {
                                    isCurrentSelectionActive -> "Current Active Pass"
                                    isCurrentSelectionLower -> "Included in Your Pass"
                                    isCurrentSelectionHigher -> "Upgrade Pass (Pay Difference)"
                                    else -> "Selected Package"
                                },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = when {
                                    isCurrentSelectionActive -> "${activeSelection.title} (Active)"
                                    isCurrentSelectionLower -> "${activeSelection.title} • Already Unlocked"
                                    isCurrentSelectionHigher -> "${activeSelection.title} (+₹$upgradeCost)"
                                    else -> "${activeSelection.title} (₹${activeSelection.priceInr.toInt()})"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (isCurrentSelectionActive) {
                            Button(
                                onClick = onViewDetails,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF059669)
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "View Pass Details",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else if (isCurrentSelectionLower) {
                            Button(
                                onClick = onViewDetails,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color(0xFF38BDF8)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Already Unlocked",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else if (isCurrentSelectionHigher) {
                            Button(
                                onClick = { onSelectTier(activeSelection) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Upgrade for ₹$upgradeCost",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = { onSelectTier(activeSelection) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Payment,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pay ₹${activeSelection.priceInr.toInt()} with UPI",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
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

@Composable
fun FounderPassDetailsDialog(
    tier: FounderTier,
    user: com.example.data.model.User?,
    onDismiss: () -> Unit,
    onUpgradeTier: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val emailOrId = user?.phoneOrEmail?.ifEmpty { "service.veloxyra@gmail.com" } ?: "service.veloxyra@gmail.com"
    val username = user?.username?.ifEmpty { "Player" } ?: "Player"
    val serialCode = "VLX-${tier.id.uppercase()}-${(user?.id?.takeLast(4)?.uppercase() ?: "4792")}-2026"
    val isTopTier = tier.id == "tier_1000"

    Dialog(onDismissRequest = onDismiss) {
        ApplyDialogWindowBlur()
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(28.dp))
                .testTag("founder_pass_details_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF20B0E14)),
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color(0x40FFFFFF),
                        Color(0x14FFFFFF),
                        Color(0x06FFFFFF)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Xiaomi Top Handle Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0x28FFFFFF))
                )

                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x18FFFFFF))
                                .border(BorderStroke(1.dp, Color(0x20FFFFFF)), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_iconsax_price),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MEMBERSHIP PASSPORT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Official Founder Pass",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = (-0.3).sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Official Pass Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            BorderStroke(
                                1.5.dp,
                                Brush.linearGradient(
                                    listOf(
                                        tier.accentColor,
                                        tier.accentColor.copy(alpha = 0.3f),
                                        Color(0xFF3B82F6)
                                    )
                                )
                            ),
                            RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141824))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // Badge / Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF064E3B),
                                border = BorderStroke(1.dp, Color(0xFF10B981))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "STATUS: ACTIVE & VERIFIED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34D399)
                                    )
                                }
                            }

                            Text(
                                text = "LEVEL 4 VANGUARD",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = tier.accentColor,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = tier.title.uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = tier.badgeName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = tier.accentColor
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFF263044))
                        Spacer(modifier = Modifier.height(12.dp))

                        // User & Serial Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "PASS HOLDER",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = username,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = emailOrId,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "CREDENTIAL ID",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = serialCode,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tier.accentColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Token Reserve Highlight
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF131B2B),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Reserved Launch Tokens",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "VT ${tier.tokensReward} Tokens",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${tier.bonusPercent} BONUS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Perks Breakdown
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "UNLOCKED FOUNDER PRIVILEGES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    tier.benefits.forEach { benefit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = benefit,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onUpgradeTier()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = if (isTopTier) "Explore All Tiers" else "Upgrade / Switch Tier",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Done",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardStableReleaseNotice() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .border(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Leaderboard",
                    tint = Gold,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "STABLE RELEASE FEATURE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "For now the functionality will be unlocked in the Stable Release",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Live player rankings, seasonal rating points, and global hall of fame leaderboards are currently in active development and will debut in the upcoming Stable Release.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeaturePreviewItem(
                    icon = Icons.Default.MilitaryTech,
                    title = "Seasonal Tier Rankings",
                    description = "Climb divisions from Bronze to Grandmaster based on match performance."
                )
                FeaturePreviewItem(
                    icon = Icons.Default.WorkspacePremium,
                    title = "Top Earner Rewards",
                    description = "End-of-season token prize pools distributed to top 100 players."
                )
                FeaturePreviewItem(
                    icon = Icons.Default.Verified,
                    title = "Verified Esports Stats",
                    description = "Authentic kill ratios, survival streaks, and custom room records."
                )
            }
        }
    }
}

@Composable
fun FeaturePreviewItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun PodiumColumn(
    player: LeaderboardPlayer,
    podiumColor: Color,
    height: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        UserAvatar(
            avatarUrl = player.avatarUrl,
            username = player.username,
            size = if (player.rank == 1) 56.dp else 48.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            val trend = kotlin.math.abs(player.username.hashCode() % 3)
            val trendIcon = when (trend) {
                0 -> Icons.Default.ArrowUpward
                1 -> Icons.Default.ArrowDownward
                else -> Icons.Default.Remove
            }
            val trendColor = when (trend) {
                0 -> Color(0xFF10B981)
                1 -> Color(0xFFEF4444)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Icon(
                imageVector = trendIcon,
                contentDescription = "Trend",
                tint = trendColor,
                modifier = Modifier
                    .size(12.dp)
                    .padding(end = 2.dp)
            )
            Text(
                text = player.username,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(75.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "VT ${player.totalWinnings.toInt()}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            podiumColor.copy(alpha = 0.85f),
                            podiumColor.copy(alpha = 0.25f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "#${player.rank}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.background
                )
                Text(
                    text = "${player.tokens} TOKENS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun LeaderboardRow(player: LeaderboardPlayer) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .testTag("leaderboard_row_${player.rank}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#${player.rank}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.width(36.dp)
                )
                val trend = kotlin.math.abs(player.username.hashCode() % 3)
                val trendIcon = when (trend) {
                    0 -> Icons.Default.ArrowUpward
                    1 -> Icons.Default.ArrowDownward
                    else -> Icons.Default.Remove
                }
                val trendColor = when (trend) {
                    0 -> Color(0xFF10B981)
                    1 -> Color(0xFFEF4444)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Icon(
                    imageVector = trendIcon,
                    contentDescription = "Trend",
                    tint = trendColor,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 4.dp)
                )
                UserAvatar(
                    avatarUrl = player.avatarUrl,
                    username = player.username,
                    size = 36.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = player.username,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${player.tokens} TOTAL TOKENS",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                text = "VT ${player.totalWinnings.toInt()}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
