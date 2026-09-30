package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * High-performance, memory-efficient shimmer brush for lazy loading screen skeletons.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val infiniteTransition = rememberInfiniteTransition(label = "screen_shimmer")
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1800f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "screen_shimmer_translate"
    )

    val baseColor = Color(0xFF1E2333).copy(alpha = 0.65f)
    val shimmerHighlight = Color(0xFF38435C).copy(alpha = 0.55f)
    val accentShimmer = Color(0xFF38BDF8).copy(alpha = 0.12f)

    return Brush.linearGradient(
        colors = listOf(
            baseColor,
            shimmerHighlight,
            accentShimmer,
            shimmerHighlight,
            baseColor
        ),
        start = Offset(translateAnim - 450f, translateAnim - 450f),
        end = Offset(translateAnim, translateAnim)
    )
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    brush: Brush = rememberShimmerBrush(),
    cornerRadius: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

@Composable
fun SkeletonCircle(
    size: Dp,
    modifier: Modifier = Modifier,
    brush: Brush = rememberShimmerBrush()
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brush)
    )
}

/**
 * Beautiful full-screen skeleton for HomeScreen
 */
@Composable
fun HomeScreenSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 1. Top Header Row: User greeting & quick balances
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonCircle(size = 46.dp, brush = brush)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    SkeletonBox(modifier = Modifier.width(80.dp).height(12.dp), brush = brush)
                    Spacer(modifier = Modifier.height(6.dp))
                    SkeletonBox(modifier = Modifier.width(120.dp).height(16.dp), brush = brush)
                }
            }
            Row {
                SkeletonBox(modifier = Modifier.width(72.dp).height(32.dp), brush = brush, cornerRadius = 16.dp)
                Spacer(modifier = Modifier.width(8.dp))
                SkeletonCircle(size = 36.dp, brush = brush)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Carousel Banner Placeholder
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            brush = brush,
            cornerRadius = 18.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) { idx ->
                SkeletonBox(
                    modifier = Modifier
                        .width(if (idx == 0) 56.dp else 84.dp)
                        .height(34.dp),
                    brush = brush,
                    cornerRadius = 17.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4. Section Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBox(modifier = Modifier.width(140.dp).height(18.dp), brush = brush)
            SkeletonBox(modifier = Modifier.width(60.dp).height(12.dp), brush = brush)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Tournament Card Skeletons
        TournamentCardSkeleton()
    }
}

/**
 * Beautiful full-screen skeleton for MatchesScreen
 */
@Composable
fun MatchesScreenSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBox(modifier = Modifier.width(150.dp).height(24.dp), brush = brush)
            SkeletonBox(modifier = Modifier.width(80.dp).height(32.dp), brush = brush, cornerRadius = 16.dp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tabs switcher row (Upcoming, Ongoing, Completed)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141A28)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) {
                Box(modifier = Modifier.weight(1f).padding(4.dp)) {
                    SkeletonBox(modifier = Modifier.fillMaxSize(), brush = brush, cornerRadius = 10.dp)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Tournament match cards
        TournamentCardSkeleton()
    }
}

/**
 * Beautiful full-screen skeleton for LeaderboardScreen
 */
@Composable
fun LeaderboardScreenSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBox(modifier = Modifier.width(160.dp).height(24.dp), brush = brush)
            SkeletonBox(modifier = Modifier.width(90.dp).height(30.dp), brush = brush, cornerRadius = 15.dp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Top 3 Podium Skeleton (2nd, 1st, 3rd)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // Rank 2
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SkeletonCircle(size = 52.dp, brush = brush)
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBox(modifier = Modifier.width(60.dp).height(12.dp), brush = brush)
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBox(modifier = Modifier.width(74.dp).height(70.dp), brush = brush, cornerRadius = 12.dp)
            }
            // Rank 1 (Tallest)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SkeletonCircle(size = 64.dp, brush = brush)
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBox(modifier = Modifier.width(70.dp).height(14.dp), brush = brush)
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBox(modifier = Modifier.width(86.dp).height(95.dp), brush = brush, cornerRadius = 14.dp)
            }
            // Rank 3
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SkeletonCircle(size = 52.dp, brush = brush)
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBox(modifier = Modifier.width(60.dp).height(12.dp), brush = brush)
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBox(modifier = Modifier.width(74.dp).height(55.dp), brush = brush, cornerRadius = 12.dp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Rank list rows
        repeat(4) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131824)),
                border = BorderStroke(1.dp, Color(0xFF2A344A).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBox(modifier = Modifier.size(24.dp), brush = brush, cornerRadius = 6.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    SkeletonCircle(size = 38.dp, brush = brush)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        SkeletonBox(modifier = Modifier.width(110.dp).height(14.dp), brush = brush)
                        Spacer(modifier = Modifier.height(6.dp))
                        SkeletonBox(modifier = Modifier.width(70.dp).height(10.dp), brush = brush)
                    }
                    SkeletonBox(modifier = Modifier.width(60.dp).height(18.dp), brush = brush, cornerRadius = 9.dp)
                }
            }
        }
    }
}

/**
 * Beautiful full-screen skeleton for WalletScreen
 */
@Composable
fun WalletScreenSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBox(modifier = Modifier.width(140.dp).height(24.dp), brush = brush)
            SkeletonCircle(size = 36.dp, brush = brush)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Vault Card Skeleton
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131824)),
            border = BorderStroke(1.dp, Color(0xFF2A344A).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SkeletonBox(modifier = Modifier.width(110.dp).height(14.dp), brush = brush)
                    SkeletonBox(modifier = Modifier.width(65.dp).height(20.dp), brush = brush, cornerRadius = 10.dp)
                }
                Column {
                    SkeletonBox(modifier = Modifier.width(160.dp).height(32.dp), brush = brush)
                    Spacer(modifier = Modifier.height(6.dp))
                    SkeletonBox(modifier = Modifier.width(90.dp).height(12.dp), brush = brush)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SkeletonBox(modifier = Modifier.weight(1f).height(38.dp), brush = brush, cornerRadius = 12.dp)
                    SkeletonBox(modifier = Modifier.weight(1f).height(38.dp), brush = brush, cornerRadius = 12.dp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Transaction history title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBox(modifier = Modifier.width(140.dp).height(16.dp), brush = brush)
            SkeletonBox(modifier = Modifier.width(50.dp).height(12.dp), brush = brush)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Transaction items
        repeat(3) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131824)),
                border = BorderStroke(1.dp, Color(0xFF2A344A).copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonCircle(size = 40.dp, brush = brush)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        SkeletonBox(modifier = Modifier.width(130.dp).height(14.dp), brush = brush)
                        Spacer(modifier = Modifier.height(6.dp))
                        SkeletonBox(modifier = Modifier.width(80.dp).height(10.dp), brush = brush)
                    }
                    SkeletonBox(modifier = Modifier.width(55.dp).height(16.dp), brush = brush)
                }
            }
        }
    }
}

/**
 * Beautiful full-screen skeleton for ProfileScreen
 */
@Composable
fun ProfileScreenSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBox(modifier = Modifier.width(110.dp).height(24.dp), brush = brush)
            SkeletonCircle(size = 36.dp, brush = brush)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Profile Avatar & Identity Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131824)),
            border = BorderStroke(1.dp, Color(0xFF2A344A).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SkeletonCircle(size = 76.dp, brush = brush)
                Spacer(modifier = Modifier.height(12.dp))
                SkeletonBox(modifier = Modifier.width(140.dp).height(18.dp), brush = brush)
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBox(modifier = Modifier.width(100.dp).height(12.dp), brush = brush, cornerRadius = 6.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(3) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SkeletonBox(modifier = Modifier.width(45.dp).height(16.dp), brush = brush)
                            Spacer(modifier = Modifier.height(4.dp))
                            SkeletonBox(modifier = Modifier.width(55.dp).height(10.dp), brush = brush)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Setting action rows
        repeat(3) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131824)),
                border = BorderStroke(1.dp, Color(0xFF2A344A).copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonCircle(size = 32.dp, brush = brush)
                    Spacer(modifier = Modifier.width(14.dp))
                    SkeletonBox(modifier = Modifier.width(140.dp).height(14.dp), brush = brush)
                    Spacer(modifier = Modifier.weight(1f))
                    SkeletonBox(modifier = Modifier.size(16.dp), brush = brush, cornerRadius = 4.dp)
                }
            }
        }
    }
}

/**
 * Beautiful full-screen skeleton for TournamentDetailsScreen
 */
@Composable
fun TournamentDetailsSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back button & Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonCircle(size = 38.dp, brush = brush)
            Spacer(modifier = Modifier.width(14.dp))
            SkeletonBox(modifier = Modifier.width(160.dp).height(20.dp), brush = brush)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hero Image Banner
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            brush = brush,
            cornerRadius = 20.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Title and Game Mode
        SkeletonBox(modifier = Modifier.width(220.dp).height(22.dp), brush = brush)
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonBox(modifier = Modifier.width(130.dp).height(14.dp), brush = brush)

        Spacer(modifier = Modifier.height(18.dp))

        // Stats Matrix (Prize, Fee, Format)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(3) {
                SkeletonBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp),
                    brush = brush,
                    cornerRadius = 14.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Progress bar for slots
        SkeletonBox(modifier = Modifier.fillMaxWidth().height(10.dp), brush = brush, cornerRadius = 5.dp)

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Join Action Button
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            brush = brush,
            cornerRadius = 16.dp
        )
    }
}

/**
 * Beautiful full-screen skeleton for NotificationCenterScreen
 */
@Composable
fun NotificationCenterSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonCircle(size = 38.dp, brush = brush)
            Spacer(modifier = Modifier.width(14.dp))
            SkeletonBox(modifier = Modifier.width(150.dp).height(22.dp), brush = brush)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) {
                SkeletonBox(modifier = Modifier.width(80.dp).height(32.dp), brush = brush, cornerRadius = 16.dp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        repeat(4) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131824)),
                border = BorderStroke(1.dp, Color(0xFF2A344A).copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    SkeletonCircle(size = 36.dp, brush = brush)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        SkeletonBox(modifier = Modifier.width(140.dp).height(14.dp), brush = brush)
                        Spacer(modifier = Modifier.height(6.dp))
                        SkeletonBox(modifier = Modifier.fillMaxWidth(0.9f).height(12.dp), brush = brush)
                        Spacer(modifier = Modifier.height(8.dp))
                        SkeletonBox(modifier = Modifier.width(60.dp).height(10.dp), brush = brush)
                    }
                }
            }
        }
    }
}

/**
 * Beautiful full-screen skeleton for SettingsScreen
 */
@Composable
fun SettingsScreenSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonCircle(size = 38.dp, brush = brush)
            Spacer(modifier = Modifier.width(14.dp))
            SkeletonBox(modifier = Modifier.width(120.dp).height(22.dp), brush = brush)
        }

        Spacer(modifier = Modifier.height(20.dp))

        repeat(2) {
            SkeletonBox(modifier = Modifier.width(100.dp).height(14.dp), brush = brush)
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131824)),
                border = BorderStroke(1.dp, Color(0xFF2A344A).copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    repeat(3) { itemIdx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SkeletonCircle(size = 28.dp, brush = brush)
                            Spacer(modifier = Modifier.width(12.dp))
                            SkeletonBox(modifier = Modifier.width(130.dp).height(14.dp), brush = brush)
                            Spacer(modifier = Modifier.weight(1f))
                            SkeletonBox(modifier = Modifier.width(36.dp).height(20.dp), brush = brush, cornerRadius = 10.dp)
                        }
                        if (itemIdx < 2) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * General purpose fallback screen skeleton
 */
@Composable
fun GenericScreenSkeleton(
    title: String = "Loading",
    modifier: Modifier = Modifier
) {
    val brush = rememberShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonCircle(size = 38.dp, brush = brush)
            Spacer(modifier = Modifier.width(14.dp))
            SkeletonBox(modifier = Modifier.width(130.dp).height(20.dp), brush = brush)
        }

        Spacer(modifier = Modifier.height(18.dp))

        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            brush = brush,
            cornerRadius = 16.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        repeat(3) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(vertical = 4.dp),
                brush = brush,
                cornerRadius = 14.dp
            )
        }
    }
}
