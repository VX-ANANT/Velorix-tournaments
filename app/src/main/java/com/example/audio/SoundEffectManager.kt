package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.example.R
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SoundEffectManager.kt
 *
 * Professional, low-latency audio engine for crisp, subtle, and iconic sound effects:
 * Inspired by Michael Jackson's signature beats and musical motifs:
 * 1. Billie Jean Groove: Crisp 116 BPM kick + snare slap + hi-hat shuffle (Match Join / Registration Success)
 * 2. Smooth Criminal Brass & Slap: Staccato horn stab & punchy slap bass (Room Credentials / Reward Unlocked)
 * 3. Beat It Power Synth: Analog power synth swell & transient punch (Wallet Deposit / Payment Success)
 * 4. Bad Funky Snap: Tight acoustic finger snap & woody rimshot (Copy Room ID / Pass to clipboard, Quick Tap)
 * 5. Thriller Chime: Vintage analog funk chime & sparkling shimmer (In-App Notification / Important Alert)
 */
class SoundEffectManager private constructor(private val context: Context) {

    enum class SfxType(val label: String, val description: String) {
        BILLIE_GROOVE("Billie Jean Groove", "116 BPM punchy kick, snare & shaker (Match Join & Win)"),
        SMOOTH_STAB("Smooth Criminal Brass", "Staccato horn stab & slap bass lick (Room Unlocked & Rewards)"),
        BEATIT_POWER("Beat It Synth Punch", "Analog power synth swell & sub punch (Wallet & Payment Success)"),
        BAD_SNAP("Bad Funky Snap", "Tight acoustic finger snap & rimshot (Copy Credentials & Micro-Tap)"),
        THRILLER_CHIME("Thriller Funk Chime", "Vintage analog chime & shimmer reverb (Notification Alert)")
    }

    private val prefs = context.getSharedPreferences("velorix_sound_prefs", Context.MODE_PRIVATE)
    private val isEnabled = AtomicBoolean(prefs.getBoolean("sfx_enabled", true))

    private val soundPool: SoundPool
    private val soundMap = ConcurrentHashMap<SfxType, Int>()
    private val loadedSounds = ConcurrentHashMap<Int, Boolean>()

    companion object {
        private const val TAG = "SoundEffectManager"

        @Volatile
        private var INSTANCE: SoundEffectManager? = null

        fun getInstance(context: Context): SoundEffectManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SoundEffectManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSounds[sampleId] = true
                Log.d(TAG, "Sample $sampleId loaded successfully.")
            } else {
                Log.w(TAG, "Failed to load sample $sampleId with status $status")
            }
        }

        preloadAllSounds()
    }

    private fun preloadAllSounds() {
        try {
            soundMap[SfxType.BILLIE_GROOVE] = soundPool.load(context, R.raw.sfx_billie_groove, 1)
            soundMap[SfxType.SMOOTH_STAB] = soundPool.load(context, R.raw.sfx_smooth_stab, 1)
            soundMap[SfxType.BEATIT_POWER] = soundPool.load(context, R.raw.sfx_beatit_power, 1)
            soundMap[SfxType.BAD_SNAP] = soundPool.load(context, R.raw.sfx_bad_snap, 1)
            soundMap[SfxType.THRILLER_CHIME] = soundPool.load(context, R.raw.sfx_thriller_chime, 1)
            Log.i(TAG, "All 5 signature SFX queued for preloading.")
        } catch (e: Throwable) {
            Log.e(TAG, "Error preloading sound effects: ${e.message}", e)
        }
    }

    /**
     * Plays the requested sound effect with optional volume scaling (0.0 to 1.0).
     */
    fun play(type: SfxType, volume: Float = 0.85f): Int {
        if (!isEnabled.get()) {
            return 0
        }

        val soundId = soundMap[type] ?: return 0
        val clampedVol = volume.coerceIn(0.0f, 1.0f)

        return try {
            val streamId = soundPool.play(soundId, clampedVol, clampedVol, 1, 0, 1.0f)
            if (streamId == 0) {
                // If not ready on first shot, fallback retry with minor delay or note
                Log.d(TAG, "Sound $type played or queued (streamId=$streamId)")
            }
            streamId
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to play $type: ${e.message}")
            0
        }
    }

    // Convenience Methods
    fun playBillieGroove(volume: Float = 0.90f): Int = play(SfxType.BILLIE_GROOVE, volume)
    fun playSmoothStab(volume: Float = 0.85f): Int = play(SfxType.SMOOTH_STAB, volume)
    fun playBeatItPower(volume: Float = 0.85f): Int = play(SfxType.BEATIT_POWER, volume)
    fun playBadSnap(volume: Float = 0.80f): Int = play(SfxType.BAD_SNAP, volume)
    fun playThrillerChime(volume: Float = 0.75f): Int = play(SfxType.THRILLER_CHIME, volume)

    fun isSoundEnabled(): Boolean = isEnabled.get()

    fun setSoundEnabled(enabled: Boolean) {
        isEnabled.set(enabled)
        prefs.edit().putBoolean("sfx_enabled", enabled).apply()
        Log.i(TAG, "Sound effects enabled status changed to: $enabled")
    }

    fun release() {
        try {
            soundPool.release()
        } catch (e: Throwable) {
            Log.w(TAG, "Error releasing SoundPool: ${e.message}")
        }
    }
}
