package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import android.util.Log
import com.example.R
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.exp
import kotlin.math.sin

/**
 * SoundEffectManager.kt
 *
 * Professional, low-latency audio engine for crisp, subtle, and iconic sound effects.
 * Inspired by Michael Jackson's signature beats, vocal motifs, and funky grooves:
 * 1. Billie Jean Groove: Crisp 116 BPM kick + snare slap + hi-hat shuffle (Match Join / Registration Success)
 * 2. Smooth Criminal Brass: Staccato horn stab & punchy slap bass (Room Credentials / Reward Unlocked)
 * 3. Beat It Power Synth: Analog power synth swell & transient punch (Wallet Deposit / Payment Success)
 * 4. Bad Funky Snap: Tight acoustic finger snap & woody rimshot (Copy Room ID / Pass to clipboard, Quick Tap)
 * 5. Thriller Chime: Vintage analog funk chime & sparkling shimmer (In-App Notification / Important Alert)
 * 6. MJ "Hee-Hee!": Iconic staccato falsetto double-vocal accent with velvet harmonic finish
 * 7. MJ "Shamone!": Funky staccato groove vocal accent with snappy punch
 */
class SoundEffectManager private constructor(private val context: Context) {

    enum class SfxType(val label: String, val description: String) {
        BILLIE_GROOVE("Billie Jean Groove", "116 BPM punchy kick, snare & shaker (Match Join & Win)"),
        SMOOTH_STAB("Smooth Criminal Brass", "Staccato horn stab & slap bass lick (Room Unlocked & Rewards)"),
        BEATIT_POWER("Beat It Synth Punch", "Analog power synth swell & sub punch (Wallet & Payment Success)"),
        BAD_SNAP("Bad Funky Snap", "Tight acoustic finger snap & rimshot (Copy Credentials & Micro-Tap)"),
        THRILLER_CHIME("Thriller Funk Chime", "Vintage analog chime & shimmer reverb (Notification Alert)"),
        MJ_HEE_HEE("Michael Jackson Hee-Hee", "Iconic subtle falsetto staccato vocal cue (Tournament Joined & Special Victory)"),
        MJ_SHAMONE("Michael Jackson Shamone", "Funky punchy vocal accent (Level Up & High Roll Entry)")
    }

    private val prefs = context.getSharedPreferences("velorix_sound_prefs", Context.MODE_PRIVATE)
    private val isEnabled = AtomicBoolean(prefs.getBoolean("sfx_enabled", true))

    private val soundPool: SoundPool
    private val soundMap = ConcurrentHashMap<SfxType, Int>()
    private val loadedSounds = ConcurrentHashMap<Int, Boolean>()
    private val audioExecutor = Executors.newSingleThreadExecutor()

    // Synthesized Audio Buffers for instantaneous zero-latency playback
    private val synthesizedPcmMap = ConcurrentHashMap<SfxType, ByteArray>()

    companion object {
        private const val TAG = "SoundEffectManager"
        private const val SAMPLE_RATE = 44100

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
            .setMaxStreams(8)
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
        generateProceduralMotifs()
    }

    private fun preloadAllSounds() {
        try {
            soundMap[SfxType.BILLIE_GROOVE] = soundPool.load(context, R.raw.sfx_billie_groove, 1)
            soundMap[SfxType.SMOOTH_STAB] = soundPool.load(context, R.raw.sfx_smooth_stab, 1)
            soundMap[SfxType.BEATIT_POWER] = soundPool.load(context, R.raw.sfx_beatit_power, 1)
            soundMap[SfxType.BAD_SNAP] = soundPool.load(context, R.raw.sfx_bad_snap, 1)
            soundMap[SfxType.THRILLER_CHIME] = soundPool.load(context, R.raw.sfx_thriller_chime, 1)
            Log.i(TAG, "All signature SFX queued for preloading.")
        } catch (e: Throwable) {
            Log.e(TAG, "Error preloading sound effects: ${e.message}", e)
        }
    }

    /**
     * Synthesizes smooth, subtle, and iconic MJ motifs procedurally into PCM buffers.
     * Guarantees instantaneous low-latency playback with velvet harmonic mixing.
     */
    private fun generateProceduralMotifs() {
        audioExecutor.execute {
            try {
                synthesizedPcmMap[SfxType.MJ_HEE_HEE] = synthesizeMjHeeHee()
                synthesizedPcmMap[SfxType.MJ_SHAMONE] = synthesizeMjShamone()
                synthesizedPcmMap[SfxType.BILLIE_GROOVE] = synthesizeMjBillieGroove()
                synthesizedPcmMap[SfxType.BAD_SNAP] = synthesizeMjBadSnap()
                synthesizedPcmMap[SfxType.SMOOTH_STAB] = synthesizeMjSmoothStab()
                synthesizedPcmMap[SfxType.BEATIT_POWER] = synthesizeMjBeatItPower()
                synthesizedPcmMap[SfxType.THRILLER_CHIME] = synthesizeMjThrillerChime()
                Log.d(TAG, "Procedural MJ audio motifs synthesized.")
            } catch (e: Throwable) {
                Log.w(TAG, "Error in procedural sound synthesis: ${e.message}")
            }
        }
    }

    /**
     * Synthesizes iconic high-falsetto "Hee-Hee!" double-accent with vocal formant resonance.
     */
    private fun synthesizeMjHeeHee(): ByteArray {
        val durationSec = 0.38
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ByteArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // 2 bursts: 1st staccato "Hee", 2nd accented "Hee" with pitch glide
            val env1 = if (t < 0.14) {
                val pt = t / 0.14
                sin(pt * Math.PI) * exp(-pt * 2.5)
            } else 0.0

            val env2 = if (t in 0.15..0.36) {
                val pt = (t - 0.15) / 0.21
                sin(pt * Math.PI) * exp(-pt * 3.0)
            } else 0.0

            // Falsetto base pitch ~ 587Hz (D5) to 659Hz (E5) + vocal harmonics (formants F1 350Hz, F2 2300Hz, F3 3100Hz)
            val pitch1 = 587.0 + 80.0 * sin(t * 40.0)
            val pitch2 = 659.0 + 120.0 * (1.0 - (t - 0.15).coerceAtLeast(0.0) * 3.0)

            val v1 = sin(2 * Math.PI * pitch1 * t) * 0.45 +
                     sin(2 * Math.PI * (pitch1 * 2.0) * t) * 0.25 +
                     sin(2 * Math.PI * 2350.0 * t) * 0.15 +
                     sin(2 * Math.PI * 3150.0 * t) * 0.08

            val v2 = sin(2 * Math.PI * pitch2 * t) * 0.50 +
                     sin(2 * Math.PI * (pitch2 * 2.0) * t) * 0.28 +
                     sin(2 * Math.PI * 2400.0 * t) * 0.18 +
                     sin(2 * Math.PI * 3200.0 * t) * 0.10

            val sample = (v1 * env1 + v2 * env2) * 0.75
            val sampleClamped = sample.coerceIn(-1.0, 1.0)
            val shortVal = (sampleClamped * 32767.0).toInt().toShort()

            buffer[i * 2] = (shortVal.toInt() and 0xFF).toByte()
            buffer[i * 2 + 1] = ((shortVal.toInt() shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    /**
     * Synthesizes iconic funky "Shamone / Aow!" accent.
     */
    private fun synthesizeMjShamone(): ByteArray {
        val durationSec = 0.32
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ByteArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = sin((t / durationSec).coerceIn(0.0, 1.0) * Math.PI) * exp(-t * 9.0)
            val pitch = 440.0 * exp(-t * 3.5) + 220.0

            val v = sin(2 * Math.PI * pitch * t) * 0.5 +
                    sin(2 * Math.PI * (pitch * 1.5) * t) * 0.3 +
                    sin(2 * Math.PI * 1800.0 * t) * 0.2

            val sample = (v * env * 0.8).coerceIn(-1.0, 1.0)
            val shortVal = (sample * 32767.0).toInt().toShort()

            buffer[i * 2] = (shortVal.toInt() and 0xFF).toByte()
            buffer[i * 2 + 1] = ((shortVal.toInt() shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    /**
     * Synthesizes 116 BPM Billie Jean tight kick-snare groove motif.
     */
    private fun synthesizeMjBillieGroove(): ByteArray {
        val durationSec = 0.45
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ByteArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // Kick transient (65Hz sub drop)
            val kickEnv = exp(-t * 24.0)
            val kickFreq = 120.0 * exp(-t * 35.0) + 55.0
            val kick = sin(2 * Math.PI * kickFreq * t) * kickEnv * 0.65

            // Snare / Shaker slap at t=0.18s
            val snareT = (t - 0.18).coerceAtLeast(0.0)
            val snareEnv = if (t >= 0.18) exp(-snareT * 22.0) else 0.0
            val whiteNoise = (Math.random() * 2.0 - 1.0) * 0.35
            val tone = sin(2 * Math.PI * 220.0 * snareT) * 0.25
            val snare = (whiteNoise + tone) * snareEnv

            val sample = (kick + snare).coerceIn(-1.0, 1.0)
            val shortVal = (sample * 32767.0).toInt().toShort()

            buffer[i * 2] = (shortVal.toInt() and 0xFF).toByte()
            buffer[i * 2 + 1] = ((shortVal.toInt() shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    /**
     * Synthesizes tight Bad-era acoustic finger snap & micro-pop.
     */
    private fun synthesizeMjBadSnap(): ByteArray {
        val durationSec = 0.18
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ByteArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 42.0)
            val noise = (Math.random() * 2.0 - 1.0) * 0.5
            val snapTone = sin(2 * Math.PI * 3400.0 * t) * 0.3 + sin(2 * Math.PI * 1800.0 * t) * 0.2
            val sample = ((noise + snapTone) * env * 0.75).coerceIn(-1.0, 1.0)
            val shortVal = (sample * 32767.0).toInt().toShort()

            buffer[i * 2] = (shortVal.toInt() and 0xFF).toByte()
            buffer[i * 2 + 1] = ((shortVal.toInt() shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    /**
     * Synthesizes staccato Smooth Criminal horn stab.
     */
    private fun synthesizeMjSmoothStab(): ByteArray {
        val durationSec = 0.35
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ByteArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 14.0) * (1.0 - exp(-t * 120.0))
            // Fm7 stab: F (349Hz), Ab (415Hz), C (523Hz), Eb (622Hz)
            val chord = sin(2 * Math.PI * 349.23 * t) * 0.25 +
                        sin(2 * Math.PI * 415.30 * t) * 0.22 +
                        sin(2 * Math.PI * 523.25 * t) * 0.20 +
                        sin(2 * Math.PI * 622.25 * t) * 0.18

            val sample = (chord * env * 0.8).coerceIn(-1.0, 1.0)
            val shortVal = (sample * 32767.0).toInt().toShort()

            buffer[i * 2] = (shortVal.toInt() and 0xFF).toByte()
            buffer[i * 2 + 1] = ((shortVal.toInt() shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    /**
     * Synthesizes Beat It analog power synth surge.
     */
    private fun synthesizeMjBeatItPower(): ByteArray {
        val durationSec = 0.36
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ByteArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 11.0) * (1.0 - exp(-t * 90.0))
            // Power fifth: E3 (164Hz) + B3 (246Hz) + E4 (329Hz) + analog drive
            val base = sin(2 * Math.PI * 164.81 * t) * 0.35 +
                       sin(2 * Math.PI * 246.94 * t) * 0.30 +
                       sin(2 * Math.PI * 329.63 * t) * 0.25
            val sample = (base * env * 0.85).coerceIn(-1.0, 1.0)
            val shortVal = (sample * 32767.0).toInt().toShort()

            buffer[i * 2] = (shortVal.toInt() and 0xFF).toByte()
            buffer[i * 2 + 1] = ((shortVal.toInt() shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    /**
     * Synthesizes Thriller funk analog shimmer chime.
     */
    private fun synthesizeMjThrillerChime(): ByteArray {
        val durationSec = 0.48
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ByteArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 7.5)
            // Bell chime harmonics
            val chime = sin(2 * Math.PI * 1046.50 * t) * 0.30 +
                        sin(2 * Math.PI * 2093.00 * t) * 0.25 +
                        sin(2 * Math.PI * 3135.96 * t) * 0.18 +
                        sin(2 * Math.PI * 4186.01 * t) * 0.10
            val sample = (chime * env * 0.75).coerceIn(-1.0, 1.0)
            val shortVal = (sample * 32767.0).toInt().toShort()

            buffer[i * 2] = (shortVal.toInt() and 0xFF).toByte()
            buffer[i * 2 + 1] = ((shortVal.toInt() shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    /**
     * Plays the requested sound effect with optional volume scaling (0.0 to 1.0).
     */
    fun play(type: SfxType, volume: Float = 0.85f): Int {
        if (!isEnabled.get()) {
            return 0
        }

        val clampedVol = volume.coerceIn(0.0f, 1.0f)
        val soundId = soundMap[type]

        if (soundId != null && loadedSounds[soundId] == true) {
            try {
                val streamId = soundPool.play(soundId, clampedVol, clampedVol, 1, 0, 1.0f)
                if (streamId != 0) return streamId
            } catch (e: Throwable) {
                Log.w(TAG, "SoundPool play failed for $type, falling back to PCM: ${e.message}")
            }
        }

        // Fast fallback to high-fidelity synthesized PCM audio track
        playProceduralTrack(type, clampedVol)
        return 1
    }

    private fun playProceduralTrack(type: SfxType, volume: Float) {
        val pcm = synthesizedPcmMap[type] ?: return
        audioExecutor.execute {
            var track: AudioTrack? = null
            try {
                val bufferSize = pcm.size
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.setVolume(volume)
                track.write(pcm, 0, pcm.size)
                track.play()
                
                // Allow completion then release
                Thread.sleep(400)
            } catch (e: Throwable) {
                Log.w(TAG, "Error playing procedural track for $type: ${e.message}")
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Throwable) {}
            }
        }
    }

    // Convenience Methods
    fun playTournamentJoinSuccess(): Int {
        // Billie Jean groove + signature MJ accent combo on match join
        val res = playBillieGroove(0.88f)
        audioExecutor.execute {
            try {
                Thread.sleep(180)
                playHeeHee(0.78f)
            } catch (_: Throwable) {}
        }
        return res
    }

    fun playHeeHee(volume: Float = 0.80f): Int = play(SfxType.MJ_HEE_HEE, volume)
    fun playShamone(volume: Float = 0.80f): Int = play(SfxType.MJ_SHAMONE, volume)
    fun playBillieGroove(volume: Float = 0.85f): Int = play(SfxType.BILLIE_GROOVE, volume)
    fun playSmoothStab(volume: Float = 0.80f): Int = play(SfxType.SMOOTH_STAB, volume)
    fun playBeatItPower(volume: Float = 0.80f): Int = play(SfxType.BEATIT_POWER, volume)
    fun playBadSnap(volume: Float = 0.75f): Int = play(SfxType.BAD_SNAP, volume)
    fun playThrillerChime(volume: Float = 0.70f): Int = play(SfxType.THRILLER_CHIME, volume)

    fun isSoundEnabled(): Boolean = isEnabled.get()

    fun setSoundEnabled(enabled: Boolean) {
        isEnabled.set(enabled)
        prefs.edit().putBoolean("sfx_enabled", enabled).apply()
        Log.i(TAG, "Sound effects enabled status changed to: $enabled")
    }

    fun release() {
        try {
            soundPool.release()
            audioExecutor.shutdown()
        } catch (e: Throwable) {
            Log.w(TAG, "Error releasing SoundPool: ${e.message}")
        }
    }
}
