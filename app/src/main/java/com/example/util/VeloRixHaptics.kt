package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * VeloRixHaptics
 *
 * Professional, battery-efficient tactile feedback engine.
 * Specifically tuned for esports actions:
 * - Tournament Join & Slot Booking (Dual affirmative pulse)
 * - Slot selection in Grid (Ultra-crisp micro tick)
 * - Room ID & Password copy (Positive snap click)
 * - Payment / Deposit / Token Convert success (Decisive heavy click)
 * - Elastic stretch & rubber-band boundary feedback (Subtle micro tick)
 *
 * Honors user preferences stored in SharedPreferences.
 */
object VeloRixHaptics {

    private const val PREFS_NAME = "velorix_haptics_prefs"
    private const val KEY_HAPTICS_ENABLED = "haptics_enabled"

    @Volatile
    private var isHapticsEnabledCached: Boolean? = null

    fun isHapticsEnabled(context: Context): Boolean {
        isHapticsEnabledCached?.let { return it }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
        isHapticsEnabledCached = enabled
        return enabled
    }

    fun setHapticsEnabled(context: Context, enabled: Boolean) {
        isHapticsEnabledCached = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, enabled).apply()
    }

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Subtle micro-tick for slot selection, avatar pick, and quick amount chip taps.
     */
    fun slotSelected(context: Context, composeHaptic: HapticFeedback? = null) {
        if (!isHapticsEnabled(context)) return
        try {
            composeHaptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(10L)
            }
        } catch (_: Throwable) {}
    }

    /**
     * High-priority decisive confirmation for Tournament Join & Slot Booking.
     */
    fun tournamentJoinedSuccess(context: Context, composeHaptic: HapticFeedback? = null) {
        if (!isHapticsEnabled(context)) return
        try {
            composeHaptic?.performHapticFeedback(HapticFeedbackType.LongPress)
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Double rhythmic confirmation pulse (50ms on, 60ms pause, 75ms on)
                val timings = longArrayOf(0, 50, 60, 75)
                val amplitudes = intArrayOf(0, 180, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 50, 60, 75), -1)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Crisp transaction confirmation haptic for Deposit, Token Convert & Claim Winnings.
     */
    fun paymentSuccess(context: Context, composeHaptic: HapticFeedback? = null) {
        if (!isHapticsEnabled(context)) return
        try {
            composeHaptic?.performHapticFeedback(HapticFeedbackType.LongPress)
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(35L)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Crisp positive tick for copying Room ID and Password credentials.
     */
    fun credentialCopied(context: Context, composeHaptic: HapticFeedback? = null) {
        if (!isHapticsEnabled(context)) return
        try {
            composeHaptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(18L)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Elastic stretch boundary threshold feedback (subtle tick when dragging past limit or bouncing back).
     */
    fun elasticBoundaryTick(context: Context) {
        if (!isHapticsEnabled(context)) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(8L)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Celebratory vibration pattern on winning match or reward claim.
     */
    fun celebrationPulse(context: Context) {
        if (!isHapticsEnabled(context)) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 40, 40, 60, 40, 90)
                val amplitudes = intArrayOf(0, 150, 0, 200, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 40, 40, 60, 40, 90), -1)
            }
        } catch (_: Throwable) {}
    }
}
