package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
                .stretchOverscroll()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
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
                            title = "Glass Player",
                            subtitle = "Manage Glass Player settings",
                            checked = config.glassPlayer,
                            onCheckedChange = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                viewModel.setGlassPlayer(it)
                            }
                        )
                        SettingsDivider()

                        SettingsToggleRow(
                            icon = Icons.Outlined.ViewStream,
                            title = "Glass Mini Player",
                            subtitle = "Manage Glass Mini Player settings",
                            checked = config.glassMiniPlayer,
                            onCheckedChange = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                viewModel.setGlassMiniPlayer(it)
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
        "crimson" to "Velorix Crimson (Esports Arena)",
        "midnight" to "Midnight Navy (Deep Sapphire)",
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
