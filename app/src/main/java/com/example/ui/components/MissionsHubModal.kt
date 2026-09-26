package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Mission
import com.example.data.model.User
import com.example.data.repository.MissionsPool
import com.example.ui.theme.NeonGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionsHubModal(
    missions: List<Mission>,
    user: User?,
    onClaimMission: (Mission) -> Unit,
    onNavigateToMatches: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var selectedTab by remember { mutableStateOf("DAILY") }

    val dailyMissions = remember(missions) { missions.filter { it.category == "DAILY" } }
    val weeklyMissions = remember(missions) { missions.filter { it.category == "WEEKLY" } }
    val monthlyMissions = remember(missions) { missions.filter { it.category == "MONTHLY" } }

    val activeMissions = when (selectedTab) {
        "DAILY" -> dailyMissions
        "WEEKLY" -> weeklyMissions
        "MONTHLY" -> monthlyMissions
        else -> dailyMissions
    }

    val todayDate = remember { MissionsPool.getTodayIstDate() }
    val claimedFromDailyMissions = remember(missions) {
        missions.filter { it.category == "DAILY" && it.isClaimed }
            .sumOf { it.rewardCurrency.toInt() }
    }
    val userClaimedToday = if (user?.lastMissionClaimDate == todayDate) {
        user.dailyMissionsTokensClaimed
    } else {
        0
    }
    val dailyClaimedToday = maxOf(userClaimedToday, claimedFromDailyMissions)

    val dailyCap = MissionsPool.DAILY_MISSION_REWARD_CAP_TOKENS

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.88f)
                    .clickable(enabled = false) {}
                    .testTag("missions_hub_modal"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Top Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                            )
                                        )
                                    )
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MilitaryTech,
                                    contentDescription = "Missions",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "MISSIONS DIRECTIVE",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Daily, Weekly & Monthly Operations",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tactical Daily Reward Cap & Streak Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_untitledui_shield_tick),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "DAILY TOKEN CAP LIMIT",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Text(
                                    text = "$dailyClaimedToday / $dailyCap Tokens",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (dailyClaimedToday >= dailyCap) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val capFraction = (dailyClaimedToday.toFloat() / dailyCap.toFloat()).coerceIn(0f, 1f)
                            val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = capFraction,
                                animationSpec = androidx.compose.animation.core.tween(durationMillis = 600, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                label = "cap_progress_anim"
                            )
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (dailyClaimedToday >= dailyCap) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = Color(0xFFF97316),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Streak: ${user?.loginStreak ?: 0} Days",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF97316)
                                    )
                                }

                                Text(
                                    text = "Resets in: ${MissionsPool.getDailyTimeRemainingFormatted()}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tab Selector Row (Daily, Weekly, Monthly)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tabs = listOf(
                            Triple("DAILY", "Daily (5)", dailyMissions.count { it.isCompleted && !it.isClaimed }),
                            Triple("WEEKLY", "Weekly (5)", weeklyMissions.count { it.isCompleted && !it.isClaimed }),
                            Triple("MONTHLY", "Monthly (5)", monthlyMissions.count { it.isCompleted && !it.isClaimed })
                        )

                        tabs.forEach { (tabKey, title, pendingCount) ->
                            val isSelected = selectedTab == tabKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        selectedTab = tabKey
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (pendingCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else Color(0xFF22C55E))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Period Reset Info Banner
                    val periodInfo = when (selectedTab) {
                        "DAILY" -> "Daily Missions reset every midnight at 12:00 AM IST (Time left: ${MissionsPool.getDailyTimeRemainingFormatted()})"
                        "WEEKLY" -> "Weekly Operations reset every Monday at 12:00 AM IST (Time left: ${MissionsPool.getWeeklyTimeRemainingFormatted()})"
                        "MONTHLY" -> "Monthly Season Directives reset on the 1st of every month (${MissionsPool.getMonthlyTimeRemainingFormatted()})"
                        else -> ""
                    }

                    Text(
                        text = periodInfo,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Missions List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(activeMissions, key = { it.id }) { mission ->
                            MissionDetailedCard(
                                mission = mission,
                                onClaim = { onClaimMission(mission) },
                                onAction = {
                                    when (mission.actionType) {
                                        "MATCH_PLAY", "KILL_COUNT", "TOP_RANK" -> {
                                            onDismiss()
                                            onNavigateToMatches()
                                        }
                                        "REFERRAL_SHARE" -> {
                                            val refCode = user?.referralCode?.ifEmpty { "VRX-${(user.username).take(4).uppercase()}" } ?: ""
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "Join Velorix Esports")
                                                putExtra(Intent.EXTRA_TEXT, "Join Velorix Esports using my referral code $refCode during registration to get squad rewards and compete for cash prizes!")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Referral Code"))
                                        }
                                        "WALLET_CONVERT" -> {
                                            onDismiss()
                                            onNavigateToWallet()
                                        }
                                        "PROFILE_EDIT" -> {
                                            onDismiss()
                                            onNavigateToProfile()
                                        }
                                        "LEADERBOARD_VIEW" -> {
                                            onDismiss()
                                            onNavigateToLeaderboard()
                                        }
                                        else -> {
                                            onDismiss()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MissionDetailedCard(
    mission: Mission,
    onClaim: () -> Unit,
    onAction: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val progressFraction = (mission.progress.toFloat() / mission.target.coerceAtLeast(1)).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (mission.isCompleted && !mission.isClaimed) 1.5.dp else 1.dp,
                color = if (mission.isCompleted && !mission.isClaimed) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = mission.badge.ifEmpty { mission.category },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = mission.title,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                        .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "+${mission.rewardCurrency.toInt()} Tokens",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = mission.description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Operational Progress",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${mission.progress} / ${mission.target}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (mission.isCompleted) Color(0xFF22C55E) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action / Claim Button
            if (mission.isClaimed) {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF22C55E),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "COMPLETED & CLAIMED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF22C55E)
                        )
                    }
                }
            } else if (mission.isCompleted) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onClaim()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "CLAIM +${mission.rewardCurrency.toInt()} COMBAT TOKENS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onAction()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = when (mission.actionType) {
                                "MATCH_PLAY", "KILL_COUNT", "TOP_RANK" -> "DEPLOY IN TOURNAMENT"
                                "REFERRAL_SHARE" -> "SHARE REFERRAL CODE"
                                "WALLET_CONVERT" -> "GO TO TOKEN CONVERTER"
                                "PROFILE_EDIT" -> "UPDATE PROFILE"
                                "LEADERBOARD_VIEW" -> "VIEW LEADERBOARDS"
                                else -> "PROCEED"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
