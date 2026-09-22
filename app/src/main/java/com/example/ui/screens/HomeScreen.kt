package com.example.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.res.vectorResource
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.*
import com.example.ui.components.stretchOverscroll
import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Tournament
import com.example.ui.components.TopBannerCarousel
import com.example.ui.components.VeloRixButton
import com.example.ui.components.TournamentCard
import com.example.ui.components.TournamentCardSkeleton
import com.example.ui.theme.NeonGreen
import com.example.ui.viewmodel.PlatformViewModel
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.shape.CircleShape
import com.example.ui.components.VeloRixGlassDialog
import androidx.compose.ui.window.DialogProperties
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PlatformViewModel,
    onNavigateToTournament: (String) -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToLeaderboard: () -> Unit = {}
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val user by viewModel.userState.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    var showWalletQuickPopup by remember { mutableStateOf(false) }
    val isAnyPopupOpen = showWalletQuickPopup
    val bgBlurRadius by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isAnyPopupOpen) 22.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 280, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
        label = "home_bg_blur"
    )
    val unreadNotifCount by viewModel.unreadNotificationCount.collectAsState()
    val tournaments by viewModel.tournaments.collectAsState()
    val missions by viewModel.missions.collectAsState()
    val banners by viewModel.banners.collectAsState()
    val liveUpdatesMap by viewModel.liveMatchUpdates.collectAsState()
    val isRefreshing by viewModel.isRefreshingHome.collectAsState()
    val isLoadingTournaments by viewModel.isLoadingTournaments.collectAsState()
    val isFirebaseConnected by viewModel.isFirebaseConnected.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val lastSyncedTimestamp by viewModel.lastSyncedTimestamp.collectAsState()
    val actionCooldowns by viewModel.actionCooldownSeconds.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedFee by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) }
    val searchHistory by viewModel.searchHistory.collectAsState()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val categories = listOf("All", "BGMI", "Free Fire")

    // Filtered tournaments memoization
    val filteredTournaments = remember(tournaments, selectedCategory, searchQuery, selectedFee) {
        tournaments.filter { t ->
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Free Fire" -> t.game.contains("Free", ignoreCase = true) || t.game.contains("FF", ignoreCase = true) || t.game.equals("Free Fire", ignoreCase = true)
                "BGMI" -> t.game.contains("BGMI", ignoreCase = true) || t.game.contains("PUBG", ignoreCase = true) || t.game.contains("Battleground", ignoreCase = true) || t.game.equals("BGMI", ignoreCase = true)
                else -> t.game.equals(selectedCategory, ignoreCase = true)
            }
            val matchesSearch = searchQuery.isBlank() || 
                t.title.contains(searchQuery, ignoreCase = true) ||
                t.game.contains(searchQuery, ignoreCase = true) ||
                t.mapType.contains(searchQuery, ignoreCase = true)
            val matchesFee = selectedFee == "All" || (selectedFee == "Free" && t.entryFee == 0.0) || (selectedFee == "Paid" && t.entryFee > 0.0)
            matchesCategory && matchesSearch && matchesFee
        }
    }
    val speechRecognizerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.getOrNull(0) ?: ""
            searchQuery = spokenText
            viewModel.saveSearchQuery(spokenText)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshHomeData() },
            modifier = Modifier
                .fillMaxSize()
                .testTag("tournament_list_pull_to_refresh")
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = bgBlurRadius)
                    .stretchOverscroll()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
            // Top User profile and Wallet details bar
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.example.ui.components.UserAvatar(
                            avatarUrl = user?.avatarUrl ?: "",
                            username = user?.username ?: "Warrior",
                            size = 46.dp,
                            onClick = { onNavigateToProfile() }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user?.username ?: "Warrior",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isFirebaseConnected) Color(0xFF22C55E) else Color(0xFFEF4444))
                                )
                                Text(
                                    text = if (isFirebaseConnected) "Cloud Online" else "Offline",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFirebaseConnected) MaterialTheme.colorScheme.primary else Color(0xFFEF4444),
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sleek Xiaomi-Style Gemini AI Logo Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFF232732),
                                            Color(0xFF14161C)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        listOf(
                                            Color(0xFF4285F4).copy(alpha = 0.7f),
                                            Color(0xFF9B72CB).copy(alpha = 0.5f),
                                            Color(0xFFEA4335).copy(alpha = 0.6f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    onNavigateToSupport()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            GeminiStarLogo(size = 24.dp, animated = true)
                        }

                        // Notification Bell Icon with Badge
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    onNavigateToNotifications()
                                }
                                .testTag("home_notification_bell_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_untitledui_bell),
                                contentDescription = "Notifications",
                                tint = if (unreadNotifCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            if (unreadNotifCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-3).dp, y = 3.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                )
                            }
                        }

                        // Balance container pill on the right
                        Row(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .clickable { 
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    showWalletQuickPopup = true
                                }
                                .padding(horizontal = 12.dp)
                                .testTag("home_wallet_balance_pill"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_custom_wallet),
                                contentDescription = "Wallet",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "VT ${user?.balance?.toInt() ?: 0}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Real-Time Dynamic Admin Banners (Only displayed when active campaigns exist in Firestore/RTDB)
            if (banners.isNotEmpty()) {
                item {
                    TopBannerCarousel(
                        banners = banners,
                        onNavigateToTournament = onNavigateToTournament,
                        onNavigateToWallet = onNavigateToWallet,
                        onNavigateToSupport = onNavigateToSupport,
                        onNavigateToLeaderboard = onNavigateToLeaderboard,
                        platformViewModel = viewModel
                    )
                }
            }

            // In-App Platform Missions Card Section
            item {
                InAppMissionsSection(
                    missions = missions,
                    user = user,
                    onClaimMission = { mission -> viewModel.claimMission(mission) },
                    onNavigateToWallet = onNavigateToWallet,
                    onNavigateToProfile = onNavigateToProfile,
                    onNavigateToSupport = onNavigateToSupport
                )
            }

            // Real-Time Firebase Network & Sync Status Bar
            item {
                com.example.ui.components.TournamentSyncStatusBar(
                    isConnected = isFirebaseConnected,
                    isSyncing = isSyncing,
                    lastSyncedTimestamp = lastSyncedTimestamp,
                    cachedTournamentCount = tournaments.size,
                    onForceSync = { viewModel.refreshHomeData() },
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )
            }

            // Categories Filter chip row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BROWSE MATCHES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 2.sp
                    )
                    Icon(
                        imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_untitledui_filter_lines),
                        contentDescription = "Filter",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); selectedCategory = category }
                                .padding(horizontal = 22.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = category.toUpperCase(Locale.current),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by title...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .onFocusChanged { searchFocused = it.isFocused },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            viewModel.saveSearchQuery(searchQuery)
                            focusManager.clearFocus()
                        }
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_untitledui_search),
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove);
                            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            }
                            speechRecognizerLauncher.launch(intent)
                        }) {
                            Icon(
                                imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_send),
                                contentDescription = "Voice Search",
                                tint = Color.Unspecified
                            )
                        }
                    }
                )
                if (searchFocused && searchHistory.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Recent Searches", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
                            searchHistory.forEach { historyQuery ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove);
                                            searchQuery = historyQuery
                                            viewModel.saveSearchQuery(historyQuery)
                                            focusManager.clearFocus()
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_timer), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(historyQuery, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            // Upcoming Matches list
            if (filteredTournaments.isEmpty()) {
                if (isRefreshing || isLoadingTournaments) {
                    items(4, key = { "skeleton_$it" }) {
                        TournamentCardSkeleton()
                    }
                } else {
                    item(key = "no_matches_banner") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_cursor),
                                contentDescription = "No matches",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(50.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "NO HOSTED MATCHES AVAILABLE",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Data updates automatically. Please check back later.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredTournaments, key = { it.id }) { match ->
                    val gameThumbnailUrl = if (match.game.contains("BGMI", true)) {
                        "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=800&q=80"
                    } else {
                        "https://images.unsplash.com/photo-1552820728-8b83bb6b773f?auto=format&fit=crop&w=800&q=80"
                    }
                    val joinCooldown = maxOf(
                        actionCooldowns["tournament_join_${match.id}"] ?: 0,
                        actionCooldowns["slot_join_${match.id}"] ?: 0
                    )
                    TournamentCard(
                        modifier = Modifier.animateItem(),
                        thumbnailUrl = gameThumbnailUrl,
                        title = match.title,
                        entryFee = match.entryFee,
                        prizePool = match.prizePool,
                        filledSlots = match.filledSlots,
                        maxSlots = match.maxSlots,
                        liveUpdate = liveUpdatesMap[match.id],
                        mapType = match.mapType,
                        perspective = match.perspective,
                        isJoined = match.joined,
                        joinCooldownSeconds = joinCooldown,
                        onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); onNavigateToTournament(match.id) },
                        onJoinClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); onNavigateToTournament(match.id) }
                    )
                }
            }
        }
        }

        if (showWalletQuickPopup) {
            HomeWalletBreakdownPopup(
                user = user,
                transactions = transactions,
                onDismiss = { showWalletQuickPopup = false },
                onNavigateToWallet = {
                    showWalletQuickPopup = false
                    onNavigateToWallet()
                }
            )
        }
    }
}

@Composable
fun InAppMissionsSection(
    missions: List<com.example.data.model.Mission>,
    user: com.example.data.model.User? = null,
    onClaimMission: (com.example.data.model.Mission) -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSupport: () -> Unit = {}
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val unclaimedCount = missions.count { it.isCompleted && !it.isClaimed }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "DAILY MISSIONS & STREAK",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.5.sp
                )
                if (unclaimedCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$unclaimedCount CLAIMABLE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            if (user != null && user.loginStreak > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = Color(0xFFF97316),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${user.loginStreak} Day Streak",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFF97316)
                    )
                }
            } else {
                Text(
                    text = "Earn Free VT Tokens",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(missions, key = { it.id }) { mission ->
                val isDailyCheckin = mission.id == "m_daily_checkin"
                Card(
                    modifier = Modifier
                        .width(264.dp)
                        .border(
                            width = if (mission.isCompleted && !mission.isClaimed) 1.5.dp else 1.dp,
                            color = if (mission.isCompleted && !mission.isClaimed) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mission.title,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
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
                            maxLines = 2,
                            minLines = 2,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress Indicator
                        val progressFraction = (mission.progress.toFloat() / mission.target.coerceAtLeast(1)).coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isDailyCheckin) "Daily Status (IST)" else "Progress",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isDailyCheckin) (if (mission.isClaimed) "Claimed" else "Available") else "${mission.progress}/${mission.target}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
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
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Button with Xiaomi HyperOS smooth styling
                        if (mission.isClaimed) {
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_untitledui_check_circle),
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isDailyCheckin) "CLAIMED TODAY" else "CLAIMED", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (mission.isCompleted) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    onClaimMission(mission)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Text("CLAIM ${mission.rewardCurrency.toInt()} TOKENS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    if (mission.id == "m_topup_pioneer") onNavigateToWallet()
                                    else if (mission.id == "m_profile_master") onNavigateToProfile()
                                    else if (mission.id == "m_support_explorer") onNavigateToSupport()
                                    else onNavigateToWallet()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Text("GO TO TASK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeWalletBreakdownPopup(
    user: com.example.data.model.User?,
    transactions: List<com.example.data.model.Transaction>,
    onDismiss: () -> Unit,
    onNavigateToWallet: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val userBalance = user?.balance ?: 0.0

    // Exact winnings calculation consistent with user stats and transaction history
    val calculatedWinnings = remember(transactions, user) {
        val txWinnings = transactions
            .filter { it.type == "WINNINGS" || it.detail.contains("Prize", ignoreCase = true) || it.detail.contains("Win", ignoreCase = true) || it.detail.contains("Bounty", ignoreCase = true) }
            .sumOf { it.amount }
        maxOf(0.0, txWinnings)
    }

    val winnings = calculatedWinnings.coerceAtMost(userBalance)
    val depositedBalance = (userBalance - winnings).coerceAtLeast(0.0)

    VeloRixGlassDialog(
        onDismissRequest = onDismiss,
        dimAmount = 0.55f,
        blurRadius = 120,
        showTopAccentHandle = false,
        containerColor = Color(0xFF141721),
        borderColor = Color(0xFF00E676).copy(alpha = 0.35f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .testTag("home_wallet_popup_card"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar: Icon, Title & Total Balance, Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFF00E676).copy(alpha = 0.25f),
                                        Color(0xFF00E676).copy(alpha = 0.05f)
                                    )
                                )
                            )
                            .border(1.dp, Color(0xFF00E676).copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_custom_wallet),
                            contentDescription = "Wallet",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "WALLET BALANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.1.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "VT ${userBalance.toInt()}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF00E676).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AVAILABLE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676),
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Breakdown: Deposited & Winnings Cards with clean balanced stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Deposited Balance Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("popup_deposited_balance_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1B202E)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF2E384D).copy(alpha = 0.7f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "DEPOSITED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "VT ${depositedBalance.toInt()}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Entry & Matches",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                }

                // Winnings Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("popup_winnings_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0D251D)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "WINNINGS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E676),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "VT ${winnings.toInt()}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E676)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Withdrawable",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF00E676).copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Action Button with redirect arrow icon and "Wallet" text
            Button(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onNavigateToWallet()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("popup_wallet_redirect_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E676),
                    contentColor = Color(0xFF0A0D14)
                ),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Wallet",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0A0D14)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_maximize_redirect),
                        contentDescription = "Open Wallet",
                        tint = Color(0xFF0A0D14),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}