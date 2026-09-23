package com.example.ui.screens
import androidx.compose.animation.animateContentSize
import com.example.ui.components.stretchOverscroll

import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ContentCopy
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.vectorResource
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Tournament
import com.example.ui.components.VeloRixButton




import com.example.ui.theme.NeonGreen

import com.example.ui.components.TournamentCardSkeleton
import com.example.ui.viewmodel.PlatformViewModel

import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    viewModel: PlatformViewModel,
    onNavigateToTournament: (String) -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val tournaments by viewModel.tournaments.collectAsState()
    val isRefreshing by viewModel.isRefreshingHome.collectAsState()
    val isLoadingTournaments by viewModel.isLoadingTournaments.collectAsState()
    val user by viewModel.userState.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val matchStats by viewModel.matchStats.collectAsState(emptyList())
    val joinedMatches = tournaments.filter { it.joined }
    val liveUpdatesMap by viewModel.liveMatchUpdates.collectAsState()

    val tabs = listOf("UPCOMING", "COMPLETED")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val selectedTabIndex = pagerState.currentPage
    
    var selectedGameFilter by remember { mutableStateOf("ALL") }
    var selectedFeeFilter by remember { mutableStateOf("ALL") }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(700)) + androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(700), initialOffsetY = { it / 5 }),
        modifier = Modifier.fillMaxSize()
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshHomeData() },
            modifier = Modifier
                .fillMaxSize()
                .testTag("matches_list_pull_to_refresh")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
        // App Header Title
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "MY esports CAMPAIGN",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "MY MATCHES",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        // Filter Chips
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Game Filter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedGameFilter == "ALL") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); selectedGameFilter = "ALL" }
                    .padding(8.dp)
            ) { Text("ALL", color = if (selectedGameFilter == "ALL") Color.Black else MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedGameFilter == "BGMI") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); selectedGameFilter = "BGMI" }
                    .padding(8.dp)
            ) { Text("BGMI", color = if (selectedGameFilter == "BGMI") Color.Black else MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedGameFilter == "Free Fire") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); selectedGameFilter = "Free Fire" }
                    .padding(8.dp)
            ) { Text("FREE FIRE", color = if (selectedGameFilter == "Free Fire") Color.Black else MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold) }

            Spacer(modifier = Modifier.weight(1f))
            
            // Fee Filter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedFeeFilter == "ALL") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); selectedFeeFilter = "ALL" }
                    .padding(8.dp)
            ) { Text("ALL", color = if (selectedFeeFilter == "ALL") Color.Black else MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedFeeFilter == "FREE") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); selectedFeeFilter = "FREE" }
                    .padding(8.dp)
            ) { Text("FREE", color = if (selectedFeeFilter == "FREE") Color.Black else MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedFeeFilter == "PAID") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); selectedFeeFilter = "PAID" }
                    .padding(8.dp)
            ) { Text("PAID", color = if (selectedFeeFilter == "PAID") Color.Black else MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        }

        // Custom selector TabRow
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                    modifier = Modifier.testTag("match_tab_$index"),
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            if (page == 0) {
            // UPCOMING REGISTERED MATCHES LIST
            val filteredUpcoming = remember(joinedMatches, selectedGameFilter, selectedFeeFilter) {
                joinedMatches.filter { match ->
                    val notCompleted = !match.status.equals("COMPLETED", ignoreCase = true) && !match.status.equals("CANCELLED", ignoreCase = true)
                    val gameMatch = when (selectedGameFilter) {
                        "ALL" -> true
                        "Free Fire" -> match.game.contains("Free", ignoreCase = true) || match.game.contains("FF", ignoreCase = true)
                        "BGMI" -> match.game.contains("BGMI", ignoreCase = true) || match.game.contains("PUBG", ignoreCase = true)
                        else -> match.game.equals(selectedGameFilter, ignoreCase = true)
                    }
                    val feeMatch = selectedFeeFilter == "ALL" || 
                                   (selectedFeeFilter == "FREE" && match.entryFee == 0.0) ||
                                   (selectedFeeFilter == "PAID" && match.entryFee > 0.0)
                    notCompleted && gameMatch && feeMatch
                }
            }
            if (filteredUpcoming.isEmpty()) {
                if (isRefreshing || isLoadingTournaments) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().stretchOverscroll(),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        items(3, key = { "skeleton_match_$it" }) {
                            TournamentCardSkeleton()
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_cursor),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "NO ENROLLED MATCHES YET",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Join hosted tournaments on your Home board! Data updates automatically.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().stretchOverscroll(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filteredUpcoming, key = { it.id }) { match ->
                        UpcomingJoinedRow(match = match, onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); onNavigateToTournament(match.id) })
                    }
                }
            }
        } else {
            // COMPLETED HISTORY
            val completedMatches = remember(tournaments, transactions, user, matchStats) {
                val list = mutableListOf<CompletedMatchItem>()

                // 1. From real recorded match stats
                matchStats.forEach { stat ->
                    val isWin = stat.position == 1
                    list.add(
                        CompletedMatchItem(
                            title = stat.tournamentTitle.ifBlank { "Tournament #${stat.matchNo}" },
                            game = stat.game.ifBlank { "Battle Royale" },
                            date = if (stat.timestamp > 0) SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(stat.timestamp)) else "Completed",
                            rank = if (isWin) "#1 Winner" else "#${stat.position}",
                            reward = if (stat.winnings > 0) "VT ${stat.winnings.toInt()}" else "VT 0",
                            kills = stat.kills
                        )
                    )
                }

                // 2. From joined completed tournaments
                tournaments.filter { it.joined && it.status.equals("COMPLETED", ignoreCase = true) }.forEach { match ->
                    if (list.none { it.title.equals(match.title, ignoreCase = true) }) {
                        val winningTx = transactions.firstOrNull { it.type == "WINNINGS" && it.detail.contains(match.title, ignoreCase = true) }
                        val wonAmount = winningTx?.amount ?: 0.0
                        val isWin = wonAmount > 0
                        val userKills = if ((user?.totalKills ?: 0) > 0) ((user?.totalKills ?: 1) % 6 + 1) else 0
                        list.add(
                            CompletedMatchItem(
                                title = match.title,
                                game = if (match.game.contains("BGMI", ignoreCase = true)) "BGMI" else "Free Fire",
                                date = match.dateTimeStr.ifBlank { "Completed" },
                                rank = if (isWin) "#1 Winner" else "Completed",
                                reward = if (isWin) "VT ${wonAmount.toInt()}" else "VT 0",
                                kills = userKills
                            )
                        )
                    }
                }

                // 3. From WINNINGS transactions
                transactions.filter { it.type == "WINNINGS" }.forEach { tx ->
                    if (list.none { it.title.equals(tx.detail, ignoreCase = true) || tx.detail.contains(it.title, ignoreCase = true) }) {
                        val game = if (tx.detail.contains("BGMI", ignoreCase = true)) "BGMI" else "Free Fire"
                        list.add(
                            CompletedMatchItem(
                                title = tx.detail.ifBlank { "Tournament Championship" },
                                game = game,
                                date = if (tx.timestamp > 0) SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(tx.timestamp)) else "Past Match",
                                rank = "#1 Winner",
                                reward = "VT ${tx.amount.toInt()}",
                                kills = 0
                            )
                        )
                    }
                }

                list
            }

            val filteredCompleted = completedMatches.filter { match ->
                val gameMatch = when (selectedGameFilter) {
                    "ALL" -> true
                    "Free Fire" -> match.game.contains("Free", ignoreCase = true) || match.game.contains("FF", ignoreCase = true)
                    "BGMI" -> match.game.contains("BGMI", ignoreCase = true) || match.game.contains("PUBG", ignoreCase = true)
                    else -> match.game.equals(selectedGameFilter, ignoreCase = true)
                }
                gameMatch 
            }

            if (filteredCompleted.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "NO COMPLETED MATCHES",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().stretchOverscroll(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filteredCompleted) { match ->
                        CompletedRow(match)
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
fun UpcomingJoinedRow(match: Tournament, onClick: () -> Unit) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = LocalContext.current
    val hasCredentials = match.roomId.isNotBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(1.dp, if (hasCredentials) NeonGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); onClick() }
            .testTag("upcoming_match_${match.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (match.game.contains("BGMI", ignoreCase = true)) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = match.game,
                            color = if (match.game.contains("BGMI", ignoreCase = true)) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasCredentials) "ROOM CREDENTIALS LIVE" else "ENROLLED • UPCOMING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasCredentials) NeonGreen else MaterialTheme.colorScheme.secondary,
                        letterSpacing = 0.5.sp
                    )
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onClick()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_send),
                        contentDescription = "Details",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = match.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${match.mapType} • ${match.perspective} • ${match.dateTimeStr}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Room Credentials alert card
            if (hasCredentials) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ROOM ID",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = match.roomId,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Room ID", match.roomId))
                                Toast.makeText(context, "Room ID copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(androidx.compose.ui.graphics.vector.ImageVector.vectorResource(id = com.example.R.drawable.ic_untitledui_copy), contentDescription = "Copy Room ID", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ROOM PASSWORD",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = match.roomPassword.ifBlank { "None" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonGreen
                            )
                        }
                        if (match.roomPassword.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Room Password", match.roomPassword))
                                    com.example.audio.SoundEffectManager.getInstance(context).playBadSnap()
                                    Toast.makeText(context, "Room Password copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(androidx.compose.ui.graphics.vector.ImageVector.vectorResource(id = com.example.R.drawable.ic_untitledui_copy), contentDescription = "Copy Password", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_home),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ROOM DETAILS BROADCAST",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Available 15 mins before custom lobby starts",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompletedRow(match: CompletedMatchItem) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current



    val isWin = !match.reward.contains("VT 0")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (match.game == "BGMI") MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = match.game,
                            color = if (match.game == "BGMI") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = match.date,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isWin) {
                    Icon(
                        imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_matches),
                        contentDescription = "Victory",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = match.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Accomplishments and stats summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "YOUR STANDING",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = match.rank,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isWin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "KILLS",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${match.kills} Kills",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "WON REWARD",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = match.reward,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isWin) NeonGreen else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

data class CompletedMatchItem(
    val title: String,
    val game: String,
    val date: String,
    val rank: String,
    val reward: String,
    val kills: Int
)

