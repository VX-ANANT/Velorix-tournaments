package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Robust validation logic for Game ID / Player UID across the application.
 */
object GameIdValidator {
    private val REGEX_8_TO_12_DIGITS = Regex("^[0-9]{8,12}$")

    /**
     * Checks if the given Game ID matches the strict 8-12 digit constraint
     * and is not a dummy repetitive or sequential number.
     */
    fun isValid(id: String): Boolean {
        val trimmed = id.trim()
        if (!REGEX_8_TO_12_DIGITS.matches(trimmed)) return false
        if (trimmed.toSet().size <= 1) return false
        if (trimmed == "12345678" || trimmed == "123456789" || trimmed == "987654321") return false
        return true
    }

    /**
     * Returns a human-friendly error message if the ID is invalid, or null if valid/empty.
     */
    fun getErrorMessage(id: String): String? {
        val trimmed = id.trim()
        if (trimmed.isEmpty()) return null
        if (!trimmed.all { it.isDigit() }) return "Invalid ID Format: Game ID must contain numbers only."
        if (trimmed.length < 8) return "Invalid ID Format: Game ID must be at least 8 digits (current: ${trimmed.length})."
        if (trimmed.length > 12) return "Invalid ID Format: Game ID cannot exceed 12 digits (current: ${trimmed.length})."
        if (trimmed.toSet().size <= 1) return "Invalid ID Format: Repetitive dummy IDs are not allowed."
        if (trimmed == "12345678" || trimmed == "123456789" || trimmed == "987654321") return "Invalid ID Format: Sequential dummy UID is not permitted."
        return null
    }

    /**
     * Sanitizes input to digits only with max length of 12.
     */
    fun filterDigits(input: String, maxLength: Int = 12): String {
        return input.filter { it.isDigit() }.take(maxLength)
    }
}

/**
 * Reusable UI Component for Game ID / Free Fire UID input with real-time validation feedback.
 */
@Composable
fun GameIdInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Game ID / UID (8-12 Digits)",
    placeholder: String = "e.g. 5123984129",
    enabled: Boolean = true,
    testTag: String = "game_id_input",
    onValidationChange: ((Boolean) -> Unit)? = null
) {
    val trimmedValue = remember(value) { value.trim() }
    val isNotBlank = trimmedValue.isNotBlank()
    val isValid = remember(trimmedValue) { GameIdValidator.isValid(trimmedValue) }
    val errorMessage = remember(trimmedValue) { GameIdValidator.getErrorMessage(trimmedValue) }
    val isError = isNotBlank && errorMessage != null

    onValidationChange?.invoke(isValid)

    val errorRed = Color(0xFFEF4444)
    val successGreen = Color(0xFF10B981)
    val focusedBorderColor = if (isError) errorRed else if (isValid) successGreen else MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                val filtered = GameIdValidator.filterDigits(input, 12)
                onValueChange(filtered)
            },
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(placeholder, color = Color(0xFF64748B)) },
            leadingIcon = {
                Icon(
                    imageVector = when {
                        isError -> ImageVector.vectorResource(id = R.drawable.ic_untitledui_alert_triangle)
                        isValid -> ImageVector.vectorResource(id = R.drawable.ic_untitledui_check_circle)
                        else -> ImageVector.vectorResource(id = R.drawable.ic_untitledui_shield_tick)
                    },
                    contentDescription = "Game ID Icon",
                    tint = if (isError) errorRed else if (isValid) successGreen else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (isNotBlank) {
                    Text(
                        text = "${trimmedValue.length}/12",
                        color = if (isError) errorRed else if (isValid) successGreen else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            },
            isError = isError,
            supportingText = {
                if (isError) {
                    Text(
                        text = errorMessage ?: "Invalid ID Format: Must be 8-12 numeric digits (e.g. 5123984129)",
                        color = errorRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else if (isValid) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_untitledui_check_circle),
                            contentDescription = null,
                            tint = successGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Valid 8-12 digit Player UID format",
                            color = successGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                } else {
                    Text(
                        text = "Enter your official 8-12 digit Free Fire / BGMI UID",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = focusedBorderColor,
                unfocusedBorderColor = if (isError) errorRed else if (isValid) successGreen.copy(alpha = 0.5f) else Color(0xFF334155),
                errorBorderColor = errorRed,
                focusedContainerColor = Color(0xFF0F121C),
                unfocusedContainerColor = Color(0xFF0F121C),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = if (isError) errorRed else MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = Color(0xFF94A3B8)
            )
        )
    }
}
