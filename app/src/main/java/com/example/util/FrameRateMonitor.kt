package com.example.util

import android.os.SystemClock
import android.util.Log
import android.view.Choreographer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Lightweight Non-Intrusive Frame Rate & UI Jank Monitor using Choreographer.FrameCallback.
 *
 * Designed specifically for high-throughput Compose feeds (e.g. Tournament Feed):
 * 1. Zero allocations on standard 60fps/120fps hot frames (uses primitive fields).
 * 2. Detects frame drops (>16.6ms threshold) and severe janks (>32ms, >64ms, >200ms).
 * 3. Logs diagnostic breadcrumbs and custom keys to Firebase Crashlytics with active component context.
 * 4. Captures non-fatal UIJankException for frozen frames (>200ms) with full component context.
 */
class UIJankException(message: String) : Exception(message)

object FrameRateMonitor : Choreographer.FrameCallback {
    private const val TAG = "FrameRateMonitor"
    private const val STANDARD_FRAME_TIME_MS = 16.66f // 60 FPS baseline (or 8.33f for 120Hz)
    private const val JANK_THRESHOLD_MS = 32.0f // Dropped at least 1 full frame
    private const val SEVERE_JANK_THRESHOLD_MS = 64.0f // Dropped 3+ frames
    private const val FROZEN_FRAME_THRESHOLD_MS = 200.0f // Extreme UI freeze

    @Volatile
    private var isMonitoring = false

    @Volatile
    var currentTag: String = "TournamentFeed"

    @Volatile
    var isScrollingState: Boolean = false

    private var lastFrameTimeNanos: Long = 0L
    private var lastLogTimeMs: Long = 0L
    private var lastStatsLogTimeMs: Long = 0L

    // Rolling window metrics (primitive counts, 0 object allocation)
    private var totalFramesInWindow = 0
    private var jankFramesInWindow = 0
    private var droppedFramesInWindow = 0
    private var maxFrameDurationInWindow = 0f

    fun start(tag: String = "TournamentFeed") {
        if (isMonitoring) {
            currentTag = tag
            return
        }
        currentTag = tag
        isMonitoring = true
        lastFrameTimeNanos = 0L
        lastLogTimeMs = SystemClock.uptimeMillis()
        lastStatsLogTimeMs = SystemClock.uptimeMillis()
        totalFramesInWindow = 0
        jankFramesInWindow = 0
        droppedFramesInWindow = 0
        maxFrameDurationInWindow = 0f

        try {
            Choreographer.getInstance().postFrameCallback(this)
            Log.d(TAG, "Started frame rate monitoring for: $tag")
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to start Choreographer frame callback: ${e.message}")
        }
    }

    fun stop() {
        if (!isMonitoring) return
        isMonitoring = false
        try {
            Choreographer.getInstance().removeFrameCallback(this)
            Log.d(TAG, "Stopped frame rate monitoring.")
        } catch (_: Throwable) {}
    }

    fun updateContext(tag: String, isScrolling: Boolean = false) {
        currentTag = tag
        isScrollingState = isScrolling
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isMonitoring) return

        if (lastFrameTimeNanos != 0L) {
            val frameDurationNs = frameTimeNanos - lastFrameTimeNanos
            val frameDurationMs = frameDurationNs / 1_000_000f

            totalFramesInWindow++
            if (frameDurationMs > maxFrameDurationInWindow) {
                maxFrameDurationInWindow = frameDurationMs
            }

            if (frameDurationMs >= JANK_THRESHOLD_MS) {
                val droppedFrames = ((frameDurationMs - STANDARD_FRAME_TIME_MS) / STANDARD_FRAME_TIME_MS).toInt().coerceAtLeast(1)
                jankFramesInWindow++
                droppedFramesInWindow += droppedFrames

                val now = SystemClock.uptimeMillis()
                // Rate-limit breadcrumb logging to Crashlytics to max 1 per 750ms to respect rate limits
                if (now - lastLogTimeMs >= 750L || frameDurationMs >= SEVERE_JANK_THRESHOLD_MS) {
                    lastLogTimeMs = now
                    recordJankToCrashlytics(frameDurationMs, droppedFrames, currentTag, isScrollingState)
                }
            }

            // Periodic 5-second aggregate window stats summary
            val now = SystemClock.uptimeMillis()
            if (now - lastStatsLogTimeMs >= 5000L) {
                val windowDurationSec = (now - lastStatsLogTimeMs) / 1000f
                val avgFps = if (windowDurationSec > 0f) (totalFramesInWindow / windowDurationSec).coerceIn(0f, 120f) else 0f

                recordRollingStatsToCrashlytics(avgFps, droppedFramesInWindow, jankFramesInWindow, maxFrameDurationInWindow, currentTag)

                // Reset window
                lastStatsLogTimeMs = now
                totalFramesInWindow = 0
                jankFramesInWindow = 0
                droppedFramesInWindow = 0
                maxFrameDurationInWindow = 0f
            }
        }

        lastFrameTimeNanos = frameTimeNanos

        if (isMonitoring) {
            try {
                Choreographer.getInstance().postFrameCallback(this)
            } catch (_: Throwable) {}
        }
    }

    private fun recordJankToCrashlytics(
        durationMs: Float,
        droppedFrames: Int,
        tag: String,
        isScrolling: Boolean
    ) {
        try {
            val scrollContext = if (isScrolling) "SCROLLING" else "IDLE/INTERACTION"
            val logMessage = "[FEED_JANK] Component: $tag | State: $scrollContext | Duration: ${String.format(java.util.Locale.US, "%.1f", durationMs)}ms | Dropped: $droppedFrames frames"
            Log.w(TAG, logMessage)

            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.log(logMessage)
            crashlytics.setCustomKey("feed_jank_component", tag)
            crashlytics.setCustomKey("feed_jank_state", scrollContext)
            crashlytics.setCustomKey("feed_jank_duration_ms", durationMs.toDouble())
            crashlytics.setCustomKey("feed_dropped_frames", droppedFrames)

            if (durationMs >= FROZEN_FRAME_THRESHOLD_MS) {
                // Non-fatal record for significant UI freezes
                crashlytics.recordException(
                    UIJankException("Tournament Feed UI Freeze: ${durationMs.toInt()}ms at $tag ($scrollContext)")
                )
            }
        } catch (_: Throwable) {}
    }

    private fun recordRollingStatsToCrashlytics(
        avgFps: Float,
        droppedFrames: Int,
        jankFrames: Int,
        maxDurationMs: Float,
        tag: String
    ) {
        try {
            val logMessage = "[FEED_STATS] 5s Summary for $tag -> Avg FPS: ${avgFps.toInt()}, Dropped Frames: $droppedFrames, Jank Occurrences: $jankFrames, Max Frame: ${maxDurationMs.toInt()}ms"
            Log.i(TAG, logMessage)

            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.log(logMessage)
            crashlytics.setCustomKey("feed_avg_fps", avgFps.toDouble())
            crashlytics.setCustomKey("feed_window_dropped_frames", droppedFrames)
        } catch (_: Throwable) {}
    }
}

/**
 * Composable lifecycle tracker that hooks into the Tournament Feed
 * and informs FrameRateMonitor of active feed tags, item counts, and scroll state.
 */
@Composable
fun TrackTournamentFeedJank(
    feedTag: String = "TournamentFeed",
    itemCount: Int = 0,
    isScrolling: Boolean = false
) {
    DisposableEffect(feedTag) {
        FrameRateMonitor.start(tag = "$feedTag [items=$itemCount]")
        onDispose {
            FrameRateMonitor.stop()
        }
    }

    LaunchedEffect(feedTag, itemCount, isScrolling) {
        FrameRateMonitor.updateContext(
            tag = "$feedTag [items=$itemCount]",
            isScrolling = isScrolling
        )
    }
}
