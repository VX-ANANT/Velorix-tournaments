package com.example.ui.components

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Tournament
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class PieChartSlice(
    val label: String,
    val value: Double,
    val percentage: Float,
    val color: Color,
    val gradientColors: List<Color>,
    val subText: String
)

@Composable
fun TournamentPrizePieChart(
    tournament: Tournament,
    modifier: Modifier = Modifier,
    chartSize: Dp = 210.dp
) {
    val haptic = LocalHapticFeedback.current
    var selectedSliceIndex by remember { mutableStateOf<Int?>(null) }
    var chartViewMode by remember { mutableIntStateOf(0) } // 0: Prize Pool Distribution, 1: Slot Occupancy

    val prizeSlices = remember(
        tournament.prizePool,
        tournament.rank1Prize,
        tournament.rank2Prize,
        tournament.rank3Prize,
        tournament.rank4To10Prize,
        tournament.killBounty
    ) {
        val total = tournament.prizePool.coerceAtLeast(1.0)
        val p1 = if (tournament.rank1Prize > 0) tournament.rank1Prize else total * 0.50
        val p2 = if (tournament.rank2Prize > 0) tournament.rank2Prize else total * 0.25
        val p3 = if (tournament.rank3Prize > 0) tournament.rank3Prize else total * 0.15
        val p4_10 = if (tournament.rank4To10Prize > 0) tournament.rank4To10Prize else (total - p1 - p2 - p3).coerceAtLeast(0.0).let { if (it > 0) it else total * 0.10 }

        val sumCalculated = (p1 + p2 + p3 + p4_10).coerceAtLeast(1.0)
        val p1Pct = ((p1 / sumCalculated) * 100f).toFloat()
        val p2Pct = ((p2 / sumCalculated) * 100f).toFloat()
        val p3Pct = ((p3 / sumCalculated) * 100f).toFloat()
        val p4Pct = (100f - p1Pct - p2Pct - p3Pct).coerceAtLeast(0f)

        val list = mutableListOf(
            PieChartSlice(
                label = "Rank 1 (Winner)",
                value = p1,
                percentage = p1Pct,
                color = Color(0xFFFFD700), // Gold
                gradientColors = listOf(Color(0xFFFFE082), Color(0xFFFFB300)),
                subText = "${p1Pct.toInt()}% of Pool"
            ),
            PieChartSlice(
                label = "Rank 2 (Runner-Up)",
                value = p2,
                percentage = p2Pct,
                color = Color(0xFF64B5F6), // Cyan / Silver Blue
                gradientColors = listOf(Color(0xFF90CAF9), Color(0xFF1E88E5)),
                subText = "${p2Pct.toInt()}% of Pool"
            ),
            PieChartSlice(
                label = "Rank 3 (3rd Place)",
                value = p3,
                percentage = p3Pct,
                color = Color(0xFFFF8A65), // Bronze / Coral
                gradientColors = listOf(Color(0xFFFFAB91), Color(0xFFE64A19)),
                subText = "${p3Pct.toInt()}% of Pool"
            ),
            PieChartSlice(
                label = "Rank 4-10 (Top 10)",
                value = p4_10,
                percentage = p4Pct,
                color = Color(0xFF81C784), // Emerald Green
                gradientColors = listOf(Color(0xFFA5D6A7), Color(0xFF388E3C)),
                subText = "${p4Pct.toInt()}% split"
            )
        )
        if (tournament.killBounty > 0) {
            list.add(
                PieChartSlice(
                    label = "Kill Bounty",
                    value = tournament.killBounty,
                    percentage = 0f,
                    color = Color(0xFFEC4899),
                    gradientColors = listOf(Color(0xFFF472B6), Color(0xFFDB2777)),
                    subText = "VT ${tournament.killBounty.toInt()} per kill"
                )
            )
        }
        list
    }

    val slotSlices = remember(tournament.filledSlots, tournament.maxSlots) {
        val total = tournament.maxSlots.coerceAtLeast(1)
        val filled = tournament.filledSlots.coerceIn(0, total)
        val available = (total - filled).coerceAtLeast(0)
        val filledPct = (filled.toFloat() / total.toFloat()) * 100f
        val availPct = 100f - filledPct

        listOf(
            PieChartSlice(
                label = "Booked Slots",
                value = filled.toDouble(),
                percentage = filledPct,
                color = Color(0xFFEF4444),
                gradientColors = listOf(Color(0xFFF87171), Color(0xFFDC2626)),
                subText = "$filled / $total Players"
            ),
            PieChartSlice(
                label = "Available Slots",
                value = available.toDouble(),
                percentage = availPct,
                color = Color(0xFF10B981),
                gradientColors = listOf(Color(0xFF34D399), Color(0xFF059669)),
                subText = "$available Slots Open"
            )
        )
    }

    val activeSlices = if (chartViewMode == 0) prizeSlices else slotSlices

    // Animation progress
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(chartViewMode, tournament.prizePool, tournament.filledSlots) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Mode Selector Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (chartViewMode == 0) "PRIZE POOL BREAKDOWN" else "SLOTS CAPACITY",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = if (chartViewMode == 0) "Interactive Tier Distribution" else "Real-Time Roster Occupancy",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Toggle Button Mode
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (chartViewMode == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    chartViewMode = 0
                                    selectedSliceIndex = null
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Prizes",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (chartViewMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (chartViewMode == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    chartViewMode = 1
                                    selectedSliceIndex = null
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Slots",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (chartViewMode == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Donut Pie Chart Canvas with Center Display
            Box(
                modifier = Modifier
                    .size(chartSize)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(activeSlices) {
                            detectTapGestures { tapOffset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val touchAngle = (Math.toDegrees(
                                    atan2(
                                        (tapOffset.y - center.y).toDouble(),
                                        (tapOffset.x - center.x).toDouble()
                                    )
                                ).toFloat() + 360f + 90f) % 360f // Align with -90 start angle

                                var currentAngle = 0f
                                for (i in activeSlices.indices) {
                                    val sweep = (activeSlices[i].percentage / 100f) * 360f
                                    if (touchAngle >= currentAngle && touchAngle < currentAngle + sweep) {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        selectedSliceIndex = if (selectedSliceIndex == i) null else i
                                        break
                                    }
                                    currentAngle += sweep
                                }
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val radius = (canvasWidth.coerceAtMost(canvasHeight) / 2f) - 16f
                    val strokeWidth = 32f
                    val selectedExtraStroke = 8f

                    var startAngle = -90f

                    activeSlices.forEachIndexed { index, slice ->
                        val isSelected = selectedSliceIndex == index
                        val sweepAngle = (slice.percentage / 100f) * 360f * animationProgress.value

                        if (sweepAngle > 0f) {
                            val currentStroke = if (isSelected) strokeWidth + selectedExtraStroke else strokeWidth
                            val currentRadius = if (isSelected) radius + 4f else radius

                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = slice.gradientColors + slice.gradientColors.first(),
                                    center = center
                                ),
                                startAngle = startAngle + 2f, // 2-degree subtle gap between slices
                                sweepAngle = (sweepAngle - 4f).coerceAtLeast(0.1f),
                                useCenter = false,
                                topLeft = Offset(center.x - currentRadius, center.y - currentRadius),
                                size = Size(currentRadius * 2, currentRadius * 2),
                                style = Stroke(
                                    width = currentStroke,
                                    cap = StrokeCap.Round
                                )
                            )
                        }
                        startAngle += (slice.percentage / 100f) * 360f
                    }
                }

                // Inner Center Display
                Box(
                    modifier = Modifier
                        .size(chartSize * 0.52f)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val activeSelection = selectedSliceIndex?.let { activeSlices.getOrNull(it) }

                        if (activeSelection != null) {
                            Text(
                                text = activeSelection.label.take(12),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeSelection.color,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (chartViewMode == 0) "VT ${activeSelection.value.toInt()}" else "${activeSelection.value.toInt()} Slots",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${activeSelection.percentage.toInt()}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            if (chartViewMode == 0) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Trophy",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "VT ${tournament.prizePool.toInt()}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "TOTAL POOL",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.5.sp
                                )
                            } else {
                                Text(
                                    text = "${tournament.filledSlots}/${tournament.maxSlots}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "PLAYERS",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Slice Interactive Legends
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                activeSlices.forEachIndexed { index, slice ->
                    val isSelected = selectedSliceIndex == index
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                selectedSliceIndex = if (selectedSliceIndex == index) null else index
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) slice.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) slice.color else Color.White.copy(alpha = 0.05f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(slice.color)
                                )
                                Column {
                                    Text(
                                        text = slice.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) slice.color else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = slice.subText,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = slice.color.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${slice.percentage.toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = slice.color,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = if (chartViewMode == 0) "VT ${slice.value.toInt()}" else "${slice.value.toInt()}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
