package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Tournament
import com.example.data.model.TournamentParticipant
import com.example.data.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotPickerModal(
    tournament: Tournament,
    currentUser: User?,
    occupiedParticipants: List<TournamentParticipant>,
    cooldownSeconds: Int = 0,
    onDismiss: () -> Unit,
    onConfirmSlot: (slotNumber: Int, ign: String, characterId: String, teamName: String, onFinished: (Boolean) -> Unit) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    val totalSlots = when {
        tournament.maxSlots <= 25 -> tournament.maxSlots
        tournament.maxSlots <= 50 -> 25
        else -> 25 // 25 standard esports slots (e.g. 25 squads/duos/solos)
    }

    val occupiedSlotMap = remember(occupiedParticipants) {
        occupiedParticipants.associateBy { it.slotNumber }
    }

    // Default to first unoccupied slot
    var selectedSlot by remember {
        mutableIntStateOf(
            (1..totalSlots).firstOrNull { it !in occupiedSlotMap.keys } ?: 1
        )
    }

    var ign by remember {
        mutableStateOf(currentUser?.inGameName?.ifBlank { currentUser.username } ?: "")
    }
    var characterId by remember {
        mutableStateOf(currentUser?.freeFireId ?: "")
    }
    var teamName by remember {
        mutableStateOf("")
    }
    var agreeToRules by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isSubmitting,
            dismissOnClickOutside = !isSubmitting,
            usePlatformDefaultWidth = false
        )
    ) {
        ApplyDialogWindowBlur()
        var isVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            isVisible = true
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x2E000000))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                        scaleIn(
                            initialScale = 0.88f,
                            animationSpec = spring(
                                dampingRatio = 0.72f,
                                stiffness = 380f
                            )
                        ),
                exit = fadeOut(animationSpec = tween(150)) +
                        scaleOut(targetScale = 0.92f, animationSpec = tween(150))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                        .heightIn(max = 680.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .testTag("slot_picker_dialog"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xF20B0E14)),
                    shape = RoundedCornerShape(28.dp),
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
                        .verticalScroll(rememberScrollState())
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

                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x18FFFFFF))
                                    .border(BorderStroke(1.dp, Color(0x20FFFFFF)), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_iconsax_timer),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Select Team / Slot",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = (-0.3).sp
                                )
                                Text(
                                    text = "${tournament.game} • Entry: VT ${tournament.entryFee.toInt()}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            enabled = !isSubmitting,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Slot Selection Grid Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ROOM SLOTS (1 - $totalSlots)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF60A5FA),
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Free", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Booked", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 5-column slot grid
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F121C))
                            .padding(8.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items((1..totalSlots).toList()) { slotNum ->
                                val isOccupied = slotNum in occupiedSlotMap.keys
                                val isSelected = slotNum == selectedSlot
                                val occupant = occupiedSlotMap[slotNum]

                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1.2f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            when {
                                                isSelected -> Color(0xFF3B82F6)
                                                isOccupied -> Color(0xFF2D1515)
                                                else -> Color(0xFF1E293B)
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = when {
                                                isSelected -> Color(0xFF93C5FD)
                                                isOccupied -> Color(0xFFEF4444).copy(alpha = 0.4f)
                                                else -> Color(0xFF334155)
                                            },
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable(enabled = !isOccupied && !isSubmitting) {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            selectedSlot = slotNum
                                        }
                                        .testTag("slot_item_$slotNum"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "#$slotNum",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isSelected -> Color.White
                                                isOccupied -> Color(0xFFF87171)
                                                else -> Color(0xFFE2E8F0)
                                            }
                                        )
                                        Text(
                                            text = if (isOccupied) (occupant?.inGameName?.take(4) ?: "Full") else "Free",
                                            fontSize = 8.sp,
                                            color = when {
                                                isSelected -> Color.White.copy(alpha = 0.9f)
                                                isOccupied -> Color(0xFFEF4444)
                                                else -> Color(0xFF10B981)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Player In-Game Name & Character ID inputs
                    Text(
                        text = "WARRIOR CREDENTIALS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF60A5FA),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = ign,
                        onValueChange = { 
                            ign = it
                            errorMessage = null 
                        },
                        label = { Text("In-Game Name (IGN)") },
                        placeholder = { Text("e.g. Mortal_07, Dynamo") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = Color(0xFF3B82F6))
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slot_ign_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F121C),
                            unfocusedContainerColor = Color(0xFF0F121C),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    GameIdInputField(
                        value = characterId,
                        onValueChange = { 
                            characterId = it
                            errorMessage = null 
                        },
                        label = "Character ID / Player UID",
                        testTag = "slot_uid_input",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        label = { Text("Team / Clan Name (Optional)") },
                        placeholder = { Text("e.g. Team Soul, GodLike") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Groups, contentDescription = null, tint = Color(0xFFA855F7))
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slot_team_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFA855F7),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F121C),
                            unfocusedContainerColor = Color(0xFF0F121C),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Anti-Cheat & Rules Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { agreeToRules = !agreeToRules },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = agreeToRules,
                            onCheckedChange = { agreeToRules = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF10B981),
                                uncheckedColor = Color(0xFF64748B)
                            )
                        )
                        Text(
                            text = "I agree to fair play rules, no hacks/emulators, and room schedule.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Join Action Button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            if (ign.isBlank()) {
                                errorMessage = "Please enter your In-Game Name (IGN)"
                                return@Button
                            }
                            if (characterId.isBlank()) {
                                errorMessage = "Please enter your Character ID / UID"
                                return@Button
                            }
                            if (!GameIdValidator.isValid(characterId)) {
                                errorMessage = GameIdValidator.getErrorMessage(characterId) ?: "Invalid ID Format: Game ID must be an authentic 8-12 digit numeric player UID (e.g. 5123984129)."
                                return@Button
                            }
                            if (!agreeToRules) {
                                errorMessage = "You must accept the tournament fair-play rules"
                                return@Button
                            }
                            isSubmitting = true
                            onConfirmSlot(selectedSlot, ign.trim(), characterId.trim(), teamName.trim()) { success ->
                                if (!success) {
                                    isSubmitting = false
                                }
                            }
                        },
                        enabled = !isSubmitting && cooldownSeconds == 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("confirm_slot_join_button"),
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Booking Slot #$selectedSlot...", fontWeight = FontWeight.Bold, color = Color.Black)
                        } else if (cooldownSeconds > 0) {
                            Text("Wait ${cooldownSeconds}s", fontWeight = FontWeight.Bold, color = Color.Black)
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Confirm Slot #$selectedSlot • Pay VT ${tournament.entryFee.toInt()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
            }
        }
    }
}
