package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LiquidGlassConfig
import com.example.ui.components.stretchOverscroll
import com.example.ui.theme.GffDevanagariFontFamily
import com.example.ui.viewmodel.PlatformViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidGlassSettingsScreen(
    viewModel: PlatformViewModel,
    onNavigateBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val config by viewModel.liquidGlassConfig.collectAsState()

    var activeDialog by remember { mutableStateOf<String?>(null) }

    val velorixRed = Color(0xFFFF0060)

    Scaffold(
        containerColor = Color(0xFF000000),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Liquid Glass (Beta)",
                        fontFamily = GffDevanagariFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF000000)
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ---------------------------------------------------------
            // 0. REAL-TIME INTERACTIVE MATERIAL PREVIEW
            // ---------------------------------------------------------
            item {
                SectionHeader(title = "REAL-TIME MATERIAL PREVIEW")
                Spacer(modifier = Modifier.height(8.dp))
                LiveGlassPreviewCard(config = config)
            }

            // ---------------------------------------------------------
            // 1. NAVIGATION BAR STYLE
            // ---------------------------------------------------------
            item {
                SectionHeader(title = "NAVIGATION BAR STYLE")
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCardContainer {
                    SettingsToggleRow(
                        icon = Icons.Outlined.SpaceDashboard,
                        title = "Floating navigation bar",
                        subtitle = "iOS-style floating tab bar that shrinks while scrolling",
                        checked = config.floatingNavBar,
                        onCheckedChange = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.setFloatingNavBar(it)
                        }
                    )
                }
            }

            // ---------------------------------------------------------
            // 2. LIQUID GLASS (BETA)
            // ---------------------------------------------------------
            item {
                SectionHeader(title = "LIQUID GLASS (BETA)")
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCardContainer {
                    SettingsToggleRow(
                        icon = Icons.Default.Check,
                        title = "Enable Liquid Glass",
                        subtitle = "Rendering Liquid Glass is resource-intensive and may reduce smoothness and battery life on some devices",
                        checked = config.enableLiquidGlass,
                        onCheckedChange = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.setEnableLiquidGlass(it)
                        }
                    )
                }
            }

            // ---------------------------------------------------------
            // 3. EFFECTS
            // ---------------------------------------------------------
            item {
                SectionHeader(title = "EFFECTS")
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCardContainer {
                    Column {
                        SettingsClickableRow(
                            icon = Icons.Outlined.Tune,
                            title = "Vibrancy",
                            subtitle = "Increase the saturation of the glass backdrop",
                            badgeText = "${(config.vibrancy * 100).roundToInt()}%",
                            onClick = { activeDialog = "vibrancy" }
                        )
                        SettingsDivider()

                        SettingsClickableRow(
                            icon = Icons.Outlined.Tune,
                            title = "Blur Radius",
                            subtitle = "Amount of blur on the glass surface",
                            badgeText = "${config.blurRadius.roundToInt()} dp",
                            onClick = { activeDialog = "blur_radius" }
                        )
                        SettingsDivider()

                        SettingsClickableRow(
                            icon = Icons.Outlined.Tune,
                            title = "Lens Refraction Height",
                            subtitle = "Manage Lens Refraction Height settings",
                            badgeText = "${(config.lensRefractionHeight * 100).roundToInt()}%",
                            onClick = { activeDialog = "lens_height" }
                        )
                        SettingsDivider()

                        SettingsClickableRow(
                            icon = Icons.Outlined.Tune,
                            title = "Lens Refraction Amount",
                            subtitle = "Manage Lens Refraction Amount settings",
                            badgeText = "${(config.lensRefractionAmount * 100).roundToInt()}%",
                            onClick = { activeDialog = "lens_amount" }
                        )
                        SettingsDivider()

                        SettingsToggleRow(
                            icon = Icons.Outlined.Tune,
                            title = "Chromatic Aberration",
                            subtitle = "Manage Chromatic Aberration settings",
                            checked = config.chromaticAberration,
                            onCheckedChange = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                viewModel.setChromaticAberration(it)
                            }
                        )
                        SettingsDivider()

                        SettingsToggleRow(
                            icon = Icons.Outlined.Tune,
                            title = "Depth Effect",
                            subtitle = "Manage Depth Effect settings",
                            checked = config.depthEffect,
                            onCheckedChange = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                viewModel.setDepthEffect(it)
                            }
                        )
                    }
                }
            }

            // ---------------------------------------------------------
            // 4. APPEARANCE
            // ---------------------------------------------------------
            item {
                SectionHeader(title = "APPEARANCE")
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCardContainer {
                    Column {
                        SettingsClickableRow(
                            icon = Icons.Outlined.Contrast,
                            title = "Surface Tint",
                            subtitle = "Tint color applied to the glass surface",
                            badgeText = config.surfaceTint.replaceFirstChar { it.uppercase() },
                            onClick = { activeDialog = "surface_tint" }
                        )
                        SettingsDivider()

                        SettingsClickableRow(
                            icon = Icons.Outlined.Tune,
                            title = "Surface Opacity",
                            subtitle = "Opacity of the glass tint overlay for readability",
                            badgeText = "${(config.surfaceOpacity * 100).roundToInt()}%",
                            onClick = { activeDialog = "surface_opacity" }
                        )
                        SettingsDivider()

                        SettingsClickableRow(
                            icon = Icons.Outlined.TextFields,
                            title = "Glass Text Color",
                            subtitle = "Text color used on glass surfaces",
                            badgeText = config.glassTextColor.replaceFirstChar { it.uppercase() },
                            onClick = { activeDialog = "text_color" }
                        )
                    }
                }
            }

            // ---------------------------------------------------------
            // 5. PER COMPONENT
            // ---------------------------------------------------------
            item {
                SectionHeader(title = "PER COMPONENT")
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCardContainer {
                    Column {
                        SettingsToggleRow(
                            icon = Icons.Outlined.ViewStream,
                            title = "Glass Header & Top Bar",
                            subtitle = "Apply frosted glass refraction and specular rim to top app bars",
                            checked = config.glassTopBar,
                            onCheckedChange = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                viewModel.setGlassTopBar(it)
                            }
                        )
                        SettingsDivider()

                        SettingsToggleRow(
                            icon = Icons.Outlined.Layers,
                            title = "Glass Dialogs & Modals",
                            subtitle = "Hardware-accelerated glass backdrop blur on popups and modals",
                            checked = config.glassDialogs,
                            onCheckedChange = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                viewModel.setGlassDialogs(it)
                            }
                        )
                        SettingsDivider()

                        SettingsToggleRow(
                            icon = Icons.Outlined.SpaceDashboard,
                            title = "Glass Navigation Bar",
                            subtitle = "Manage Glass Navigation Bar settings",
                            checked = config.glassNavBar,
                            onCheckedChange = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                viewModel.setGlassNavBar(it)
                            }
                        )
                        SettingsDivider()

                        SettingsToggleRow(
                            icon = Icons.Outlined.Layers,
                            title = "Glass Cards",
                            subtitle = "Apply frosted glass and glowing border to tournament cards",
                            checked = config.glassCards,
                            onCheckedChange = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                viewModel.setGlassCards(it)
                            }
                        )
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // INTERACTIVE REAL-TIME ADJUSTMENT DIALOGS
    // -------------------------------------------------------------
    when (activeDialog) {
        "vibrancy" -> {
            SliderSettingDialog(
                title = "Vibrancy",
                description = "Increase the saturation of the glass backdrop",
                currentValue = config.vibrancy,
                range = 0.0f..2.0f,
                steps = 20,
                displayFormat = { "${(it * 100).roundToInt()}%" },
                onValueChange = { viewModel.setVibrancy(it) },
                onDismiss = { activeDialog = null }
            )
        }
        "blur_radius" -> {
            SliderSettingDialog(
                title = "Blur Radius",
                description = "Amount of blur on the glass surface",
                currentValue = config.blurRadius,
                range = 0f..30f,
                steps = 30,
                displayFormat = { "${it.roundToInt()} dp" },
                onValueChange = { viewModel.setBlurRadius(it) },
                onDismiss = { activeDialog = null }
            )
        }
        "lens_height" -> {
            SliderSettingDialog(
                title = "Lens Refraction Height",
                description = "Manage upper curvature refraction height",
                currentValue = config.lensRefractionHeight,
                range = 0.1f..1.0f,
                steps = 18,
                displayFormat = { "${(it * 100).roundToInt()}%" },
                onValueChange = { viewModel.setLensRefractionHeight(it) },
                onDismiss = { activeDialog = null }
            )
        }
        "lens_amount" -> {
            SliderSettingDialog(
                title = "Lens Refraction Amount",
                description = "Manage specular highlight brightness and glare reflection intensity",
                currentValue = config.lensRefractionAmount,
                range = 0.0f..0.40f,
                steps = 20,
                displayFormat = { "${(it * 100).roundToInt()}%" },
                onValueChange = { viewModel.setLensRefractionAmount(it) },
                onDismiss = { activeDialog = null }
            )
        }
        "surface_opacity" -> {
            SliderSettingDialog(
                title = "Surface Opacity",
                description = "Opacity of the glass tint overlay for transparency & readability",
                currentValue = config.surfaceOpacity,
                range = 0.05f..0.85f,
                steps = 16,
                displayFormat = { "${(it * 100).roundToInt()}%" },
                onValueChange = { viewModel.setSurfaceOpacity(it) },
                onDismiss = { activeDialog = null }
            )
        }
        "surface_tint" -> {
            TintSelectionDialog(
                currentTint = config.surfaceTint,
                onSelect = {
                    viewModel.setSurfaceTint(it)
                    activeDialog = null
                },
                onDismiss = { activeDialog = null }
            )
        }
        "text_color" -> {
            TextColorSelectionDialog(
                currentColor = config.glassTextColor,
                onSelect = {
                    viewModel.setGlassTextColor(it)
                    activeDialog = null
                },
                onDismiss = { activeDialog = null }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontFamily = GffDevanagariFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF94A3B8),
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SettingsCardContainer(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF1E212B),
        border = BorderStroke(1.dp, Color(0xFF2B3140).copy(alpha = 0.40f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = Color(0xFF2A2E3D).copy(alpha = 0.5f),
        thickness = 0.8.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontFamily = GffDevanagariFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF5B8DEF),
                uncheckedThumbColor = Color(0xFF64748B),
                uncheckedTrackColor = Color(0xFF131620),
                uncheckedBorderColor = Color(0xFF334155)
            )
        )
    }
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontFamily = GffDevanagariFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF2A2F3D),
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Text(
                text = badgeText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF5B8DEF),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SliderSettingDialog(
    title: String,
    description: String,
    currentValue: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    displayFormat: (Float) -> String,
    onValueChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var tempVal by remember { mutableFloatStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E212B),
        titleContentColor = Color.White,
        textContentColor = Color(0xFF94A3B8),
        title = {
            Text(
                text = title,
                fontFamily = GffDevanagariFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Value",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        text = displayFormat(tempVal),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5B8DEF)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = tempVal,
                    onValueChange = {
                        tempVal = it
                        onValueChange(it)
                    },
                    valueRange = range,
                    steps = steps,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFF5B8DEF),
                        inactiveTrackColor = Color(0xFF2B3140)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Done", color = Color(0xFF5B8DEF), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun TintSelectionDialog(
    currentTint: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val tints = listOf(
        "obsidian" to "Dark Obsidian (Sleek Apple Pro)",
        "crimson" to "Velorix Crimson (Esports Championship)",
        "midnight" to "Midnight Navy (Deep Sapphire)",
        "emerald" to "Toxic Emerald (Cyber Gamer)",
        "gold" to "Imperial Gold (Champion Tier)",
        "clear" to "Smoky Crystal (Ultra Transparent)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E212B),
        title = {
            Text(
                text = "Surface Tint",
                fontFamily = GffDevanagariFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tints.forEach { (key, label) ->
                    val isSelected = currentTint.equals(key, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF2B3448) else Color(0xFF161922),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF5B8DEF) else Color(0xFF2B3140).copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(key) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelect(key) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF5B8DEF),
                                    unselectedColor = Color(0xFF64748B)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close", color = Color(0xFF5B8DEF))
            }
        }
    )
}

@Composable
private fun TextColorSelectionDialog(
    currentColor: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        "white" to "Pure Crisp White (#FFFFFF)",
        "adaptive" to "Adaptive M3 Dynamic Tone",
        "platinum" to "Platinum Silver (#E2E8F0)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E212B),
        title = {
            Text(
                text = "Glass Text Color",
                fontFamily = GffDevanagariFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (key, label) ->
                    val isSelected = currentColor.equals(key, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF2B3448) else Color(0xFF161922),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF5B8DEF) else Color(0xFF2B3140).copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(key) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelect(key) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF5B8DEF),
                                    unselectedColor = Color(0xFF64748B)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close", color = Color(0xFF5B8DEF))
            }
        }
    )
}

@Composable
private fun LiveGlassPreviewCard(config: LiquidGlassConfig) {
    val specularIntensity = config.lensRefractionAmount.coerceIn(0.0f, 0.45f)
    val glareHeightFraction = config.lensRefractionHeight.coerceIn(0.15f, 0.95f)
    val isGlassEnabled = config.enableLiquidGlass

    val baseTint = when (config.surfaceTint.lowercase()) {
        "crimson" -> Color(0xFF1E030B)
        "midnight" -> Color(0xFF091122)
        "clear" -> Color(0xFF06080E)
        "emerald" -> Color(0xFF031A0F)
        "gold" -> Color(0xFF1E1704)
        else -> Color(0xFF0D111A)
    }

    val textColor = when (config.glassTextColor.lowercase()) {
        "platinum" -> Color(0xFFE2E8F0)
        "adaptive" -> Color(0xFFF1F5F9)
        else -> Color.White
    }

    val specularRimGradient = if (config.chromaticAberration) {
        Brush.verticalGradient(
            0.0f to Color(0xFF38BDF8).copy(alpha = (specularIntensity * 1.3f).coerceIn(0.1f, 0.6f)),
            0.20f to Color.White.copy(alpha = specularIntensity * 0.40f),
            0.75f to Color(0xFFF43F5E).copy(alpha = (specularIntensity * 0.25f).coerceIn(0.04f, 0.25f)),
            1.0f to Color.White.copy(alpha = specularIntensity * 0.45f)
        )
    } else {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = specularIntensity),
            0.20f to Color.White.copy(alpha = specularIntensity * 0.35f),
            0.75f to Color.White.copy(alpha = specularIntensity * 0.10f),
            1.0f to Color.White.copy(alpha = specularIntensity * 0.40f)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0B0E14))
    ) {
        // Vibrant background lights underneath the glass
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF3B82F6).copy(alpha = (0.85f * config.vibrancy).coerceIn(0.1f, 1f)), Color.Transparent),
                    radius = size.width * 0.55f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.3f)
                ),
                radius = size.width * 0.55f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.3f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFEC4899).copy(alpha = (0.80f * config.vibrancy).coerceIn(0.1f, 1f)), Color.Transparent),
                    radius = size.width * 0.5f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.7f)
                ),
                radius = size.width * 0.5f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.7f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF10B981).copy(alpha = (0.65f * config.vibrancy).coerceIn(0.1f, 1f)), Color.Transparent),
                    radius = size.width * 0.35f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.9f)
                ),
                radius = size.width * 0.35f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.9f)
            )
        }

        // Glass Surface Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .shadow(
                    elevation = if (config.depthEffect) 12.dp else 0.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color.Black.copy(alpha = 0.4f),
                    spotColor = Color.Black.copy(alpha = 0.6f)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (isGlassEnabled) {
                        baseTint.copy(alpha = (config.surfaceOpacity * 0.85f * config.vibrancy).coerceIn(0.12f, 0.88f))
                    } else {
                        Color(0xFF1E212B)
                    }
                )
                .drawBehind {
                    if (isGlassEnabled) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.White.copy(alpha = (specularIntensity * 0.45f).coerceIn(0.04f, 0.22f)),
                                0.7f to Color.White.copy(alpha = (specularIntensity * 0.10f).coerceIn(0.01f, 0.06f)),
                                1.0f to Color.Transparent
                            ),
                            size = androidx.compose.ui.geometry.Size(size.width, size.height * glareHeightFraction),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(40f, 40f)
                        )
                    }
                }
                .border(
                    width = 1.dp,
                    brush = if (isGlassEnabled) specularRimGradient else androidx.compose.ui.graphics.SolidColor(Color(0xFF334155)),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                .background(if (isGlassEnabled) Color(0xFF10B981) else Color(0xFFF59E0B))
                        )
                        Text(
                            text = if (isGlassEnabled) "ACTIVE LIQUID GLASS" else "PERFORMANCE MODE (SOLID)",
                            fontFamily = GffDevanagariFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = config.surfaceTint.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isGlassEnabled) "Real-time Material Reflection" else "Hardware Optimized Rendering",
                        fontFamily = GffDevanagariFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = if (isGlassEnabled)
                            "Opacity: ${(config.surfaceOpacity * 100).roundToInt()}% • Blur: ${config.blurRadius.roundToInt()}dp • Glare: ${(config.lensRefractionAmount * 100).roundToInt()}%"
                        else
                            "Glass blur disabled. UI operates at 120 FPS maximum smoothness.",
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
