/*
 * Ported directly from BitChord (com.music.bitchord.ui.components.floatingtabbar.GlassSelectionPill).
 * Licensed under GPL-3.0 / MIT.
 *
 * Provides:
 * - GlassPillMotion: Physics springs for travel, held lift, acceleration squash, and glass-to-flat hand-off.
 * - PillSquashModel: Real-time acceleration differentiation and deformation.
 * - pillPlacement: Layout modifier placing pill centered on layout-time coordinates and live size.
 * - FlatSelectionPill: Plain background fill at rest size.
 * - GlassSelectionPill: Lifted glass lens refracts the bar glass & tab row, draws rainbow chromatic aberration rim,
 *   contact shadow, and inflates past the bar's edges.
 */
package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.ui.components.backdrop.Backdrop
import com.example.ui.components.backdrop.backdrops.LayerBackdrop
import com.example.ui.components.backdrop.drawBackdrop
import com.example.ui.components.backdrop.effects.lens
import com.example.ui.components.backdrop.highlight.Highlight
import com.example.ui.components.backdrop.shadow.Shadow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sign

/** How much taller than the bar the pill stands while it is lifted (from BitChord). */
internal val PILL_GROW_HEIGHT = 10.dp

/**
 * The selection pill's physics, owned by the tab bar and handed to whichever
 * pill is drawing it. Ported directly from BitChord.
 */
@Stable
internal class GlassPillMotion(
    private val scope: CoroutineScope,
    initialIndex: Int,
) {
    /** Where the pill is drawn, in tab steps. */
    var position by mutableFloatStateOf(initialIndex.toFloat())
        private set

    /** The width lift, `0` at rest and `1` at the raised size; overshoots both. */
    var liftX by mutableFloatStateOf(0f)
        private set

    /** The height lift; see [liftX]. */
    var liftY by mutableFloatStateOf(0f)
        private set

    /** How much of the material is on, `0`..`1`. */
    var material by mutableFloatStateOf(0f)
        private set

    /** Acceleration squash: width scales by `1 + this`, height by `1 - this`. */
    var deviation by mutableFloatStateOf(0f)
        private set

    /** How much of the pill still reads as glass, `1` lifted to `0` flat. */
    var glassPresence by mutableFloatStateOf(0f)
        private set

    /**
     * True when there is nothing left in the pill but a flat fill at rest size.
     */
    var isFlat by mutableStateOf(true)
        private set

    /** The tab the pill is headed for. */
    var index: Int = initialIndex
        private set

    /** One tab step in dp, which the squash model samples the pill's travel in. */
    var stepDp: Float = 0f

    /** "Reduce animation": every move lands at once, with no lift. */
    var reduceMotion: Boolean = false

    private var travelPos = initialIndex.toDouble()
    private var travelVel = 0.0
    private var travelTarget = travelPos
    private var travelFrom = travelPos
    private var travelActive = false
    private var lifted = false

    private var liftXPos = 0.0
    private var liftXVel = 0.0
    private var liftYPos = 0.0
    private var liftYVel = 0.0
    private var liftPos = 0.0
    private var liftVel = 0.0

    private var dragging = false
    private var dragTarget = 0.0
    private var dragFollow = 0.0

    private var travelSign = 0.0
    private var travelSignEased = 0.0
    private var handover = 1.0
    private var squash = 0.0
    private val squashModel = PillSquashModel()
    private var running = false

    /** A tab was chosen: travel there, lifted. */
    fun animateTo(next: Int) {
        if (next == index) return
        if (reduceMotion) {
            snapTo(next)
            return
        }
        index = next
        travelActive = true
        lifted = true
        travelFrom = travelPos
        travelTarget = next.toDouble()
        travelSign = signOfTravel(travelTarget - travelFrom)
        isFlat = false
        start()
    }

    /** Place the pill on [target] at rest, with no travel and no lift. */
    fun snapTo(target: Int) {
        index = target
        travelPos = target.toDouble()
        travelTarget = travelPos
        travelFrom = travelPos
        travelVel = 0.0
        travelActive = false
        dragging = false
        lifted = false
        liftXPos = 0.0; liftXVel = 0.0
        liftYPos = 0.0; liftYVel = 0.0
        liftPos = 0.0; liftVel = 0.0
        handover = 1.0
        squash = 0.0
        squashModel.stop()
        travelSign = 0.0
        travelSignEased = 0.0
        publish()
        isFlat = true
    }

    /** A finger has picked the pill up where it stands or holds it. */
    fun startDrag() {
        dragging = true
        travelActive = false
        lifted = !reduceMotion
        travelSign = 0.0
        dragFollow = travelPos
        dragTarget = travelPos
        travelVel = 0.0
        isFlat = false
        start()
    }

    /** The finger is over [position] tab steps. */
    fun dragTo(position: Float) {
        if (dragging) dragTarget = position.toDouble()
    }

    /**
     * The finger let go and the pill is to settle on [next].
     */
    fun release(next: Int) {
        if (!dragging) return
        val from = dragFollow
        dragging = false
        index = next
        if (reduceMotion) {
            snapTo(next)
            return
        }
        travelActive = true
        travelPos = from
        travelVel = 0.0
        travelFrom = from
        travelTarget = next.toDouble()
        travelSign = signOfTravel(travelTarget - travelFrom)
        start()
    }

    private fun start() {
        if (running) return
        running = true
        scope.launch {
            var last = -1L
            try {
                while (true) {
                    val now = withFrameNanos { it }
                    val dt = if (last < 0) 0.0 else (now - last) / 1e9
                    last = now
                    if (!tick(dt, now / 1e9)) break
                }
            } finally {
                running = false
            }
        }
    }

    private fun tick(dt: Double, now: Double): Boolean {
        // 1) Travel.
        var travelSettled = true
        if (travelActive) {
            val r = springStep(travelPos, travelVel, travelTarget, dt, TRAVEL_STIFFNESS, TRAVEL_DAMPING)
            travelPos = r.first
            travelVel = r.second
            travelSettled = abs(travelPos - travelTarget) < 0.003 && abs(travelVel) < 0.05
            if (travelSettled) {
                travelPos = travelTarget
                travelVel = 0.0
            }
        }

        // 2) Under a finger, chase smoothly.
        if (dragging) {
            dragFollow += if (reduceMotion) {
                dragTarget - dragFollow
            } else {
                (dragTarget - dragFollow) * (1 - exp(-dt / FOLLOW_TAU))
            }
        }

        // 3) The lift, held for the journey and released on landing.
        if (!dragging && (travelSettled || travelProgress() >= HANDOVER_START)) {
            lifted = false
        }
        val liftTarget = if (lifted) 1.0 else 0.0
        stepLift(liftXPos, liftXVel, liftTarget, dt, LIFT_STIFFNESS, LIFT_DAMPING_X).let {
            liftXPos = it.first; liftXVel = it.second
        }
        stepLift(liftYPos, liftYVel, liftTarget, dt, LIFT_STIFFNESS, LIFT_DAMPING_Y).let {
            liftYPos = it.first; liftYVel = it.second
        }
        stepLift(liftPos, liftVel, liftTarget, dt, MATERIAL_STIFFNESS, MATERIAL_DAMPING).let {
            liftPos = it.first; liftVel = it.second
        }
        val liftSettled = !lifted && liftXPos == 0.0 && liftYPos == 0.0 && liftPos == 0.0

        if (travelActive && travelSettled && liftSettled && !dragging) {
            travelActive = false
        }

        // 5) Sample deformation.
        val frac = if (dragging) dragFollow else travelPos
        if (stepDp > 0f) {
            if (!squashModel.isTracking) squashModel.start()
            squash = squashModel.track(frac * stepDp, now, dt)
            if (travelSignEased == 0.0) {
                travelSignEased = travelSign
            } else if (travelSignEased != travelSign) {
                travelSignEased += (travelSign - travelSignEased) * (1 - exp(-dt / SIGN_TAU))
                if (abs(travelSign - travelSignEased) < 0.01) travelSignEased = travelSign
            }
            val key = travelSignEased
            if (key != 0.0) squash = squash * (1 - abs(key)) - key * abs(squash)
        }

        // 6) Hand-off to flat.
        val handoverTarget = if (lifted) 0.0 else 1.0
        val tau = if (handoverTarget > handover) HANDOVER_TAU else GLASS_RETURN_TAU
        handover += (handoverTarget - handover) * (1 - exp(-dt / tau))
        if (abs(handoverTarget - handover) < 0.002) handover = handoverTarget

        val settled = !travelActive && !dragging && liftSettled &&
            abs(squash) < 0.0005 && handover >= 1.0
        if (settled) {
            squashModel.stop()
            squash = 0.0
            travelSign = 0.0
            travelSignEased = 0.0
        }
        publish()
        isFlat = settled
        return !settled
    }

    private fun publish() {
        position = (if (dragging) dragFollow else travelPos).toFloat()
        liftX = liftXPos.toFloat()
        liftY = liftYPos.toFloat()
        material = liftPos.coerceIn(0.0, 1.0).toFloat()
        deviation = squash.toFloat()
        glassPresence = (1 - handover).coerceIn(0.0, 1.0).toFloat()
    }

    private fun travelProgress(): Double {
        val span = abs(travelTarget - travelFrom)
        if (span < 1e-6) return 1.0
        return (1 - abs(travelTarget - travelPos) / span).coerceIn(0.0, 1.0)
    }

    private fun signOfTravel(span: Double): Double = if (abs(span) < 1e-6) 0.0 else sign(span)

    /** The size the pill is drawn at this frame, between [rest] and [lifted]. */
    fun liveSize(rest: Size, lifted: Size): Size {
        val w = rest.width + (lifted.width - rest.width) * liftX
        val h = rest.height + (lifted.height - rest.height) * liftY
        return Size(w * (1 + deviation), h * (1 - deviation))
    }

    private companion object {
        const val TRAVEL_STIFFNESS = 280.0
        const val TRAVEL_DAMPING = 31.4
        const val LIFT_STIFFNESS = 250.0
        const val LIFT_DAMPING_X = 19.0
        const val LIFT_DAMPING_Y = 22.1
        const val MATERIAL_STIFFNESS = 1000.0
        const val MATERIAL_DAMPING = 63.3
        const val HANDOVER_START = 0.92
        const val HANDOVER_TAU = 0.09
        const val GLASS_RETURN_TAU = 0.05
        const val FOLLOW_TAU = 0.05
        const val SIGN_TAU = 0.25

        fun springStep(
            x: Double,
            vel: Double,
            target: Double,
            dt: Double,
            stiffness: Double,
            damping: Double,
        ): Pair<Double, Double> {
            var t = dt
            var px = x
            var pv = vel
            while (t > 0) {
                val step = if (t > 1 / 240.0) 1 / 240.0 else t
                val accel = -stiffness * (px - target) - damping * pv
                pv += accel * step
                px += pv * step
                t -= step
            }
            return px to pv
        }

        fun stepLift(
            x: Double,
            vel: Double,
            target: Double,
            dt: Double,
            stiffness: Double,
            damping: Double,
        ): Pair<Double, Double> {
            val r = springStep(x, vel, target, dt, stiffness, damping)
            return if (abs(r.first - target) < 0.0008 && abs(r.second) < 0.01) target to 0.0 else r
        }
    }
}

private class PillSquashModel {
    private val history = ArrayList<Pair<Double, Double>>()
    var isTracking = false
        private set
    private var value = 0.0

    fun start() {
        history.clear()
        value = 0.0
        isTracking = true
    }

    fun stop() {
        isTracking = false
        history.clear()
        value = 0.0
    }

    fun track(x: Double, now: Double, dt: Double): Double {
        if (!isTracking) return value
        history.add(x to now)
        val cutoff = now - SAMPLE_WINDOW
        history.removeAll { it.second < cutoff }
        val raw = (averageAcceleration() * SENSITIVITY).coerceIn(-MAX_DEFORMATION, MAX_DEFORMATION)
        val ease = (dt / RESPONSE_TIME).coerceIn(0.0, 1.0)
        value += (raw - value) * ease
        return value
    }

    private fun averageAcceleration(): Double {
        if (history.size < 3) return 0.0
        val velocities = ArrayList<Pair<Double, Double>>(history.size)
        for (i in 1 until history.size) {
            val dt = history[i].second - history[i - 1].second
            if (dt <= 0) continue
            velocities.add(
                (history[i].first - history[i - 1].first) / dt to
                    (history[i].second + history[i - 1].second) / 2,
            )
        }
        if (velocities.size < 2) return 0.0
        var total = 0.0
        var count = 0
        for (i in 1 until velocities.size) {
            val dt = velocities[i].second - velocities[i - 1].second
            if (dt <= 0) continue
            total += (velocities[i].first - velocities[i - 1].first) / dt
            count++
        }
        return if (count == 0) 0.0 else total / count
    }

    private companion object {
        const val SAMPLE_WINDOW = 0.3
        const val SENSITIVITY = 0.00007
        const val MAX_DEFORMATION = 0.12
        const val RESPONSE_TIME = 0.18
    }
}

/**
 * Places a pill-sized child centered on [center] at [size], inside a parent it
 * matches the size of. Both read on layout, not composition.
 */
internal fun Modifier.pillPlacement(center: () -> Offset, size: () -> Size): Modifier =
    layout { measurable, constraints ->
        val s = size()
        val w = if (s.width.isNaN() || s.width <= 0f) 1 else s.width.roundToInt().coerceAtLeast(1)
        val h = if (s.height.isNaN() || s.height <= 0f) 1 else s.height.roundToInt().coerceAtLeast(1)
        val placeable = measurable.measure(Constraints.fixed(w, h))
        layout(constraints.minWidth, constraints.minHeight) {
            val c = center()
            val cx = if (c.x.isNaN()) 0f else c.x
            val cy = if (c.y.isNaN()) 0f else c.y
            placeable.place((cx - w / 2f).roundToInt(), (cy - h / 2f).roundToInt())
        }
    }

/** The flat pill: a fill and nothing else. */
@Composable
internal fun FlatSelectionPill(
    color: Color,
    modifier: Modifier,
) {
    val shape = remember { RoundedCornerShape(percent = 50) }
    Box(modifier.background(color, shape))
}

/**
 * The lifted pill: a lens over the tab bar that refracts the bar's own glass
 * and its tab row, drawn above both and free to stand past the bar's edges.
 *
 * Sourced directly from BitChord (GlassSelectionPill).
 */
@Composable
internal fun GlassSelectionPill(
    motion: GlassPillMotion,
    fillColor: Color,
    barGlass: LayerBackdrop,
    tabRow: LayerBackdrop,
    modifier: Modifier,
) {
    val page = LocalAppBackdrop.current
    val shape = remember { RoundedCornerShape(percent = 50) }
    val backdrop = remember(page, barGlass, tabRow, fillColor) {
        SelectionPillBackdrop(page, barGlass, tabRow) {
            fillColor.copy(alpha = fillColor.alpha * (1f - motion.material))
        }
    }

    Box(
        modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val rHeight = LENS_HEIGHT.toPx() * motion.glassPresence
                    val rAmount = LENS_AMOUNT.toPx() * motion.material
                    if (rHeight > 0.05f && rAmount > 0.05f) {
                        lens(
                            refractionHeight = rHeight,
                            refractionAmount = rAmount,
                            chromaticAberration = true,
                        )
                    }
                }
            },
            highlight = {
                val alpha = motion.material * rimOf(motion.glassPresence)
                if (alpha > 0f) Highlight.Default.copy(alpha = alpha) else null
            },
            shadow = {
                val alpha = motion.material * rimOf(motion.glassPresence)
                if (alpha > 0f) PILL_SHADOW.copy(alpha = alpha) else null
            },
        ),
    )
}

private fun rimOf(presence: Float): Float = ((presence - 0.45f) / 0.55f).coerceIn(0f, 1f)
private val LENS_HEIGHT = 12.dp
private val LENS_AMOUNT = 14.dp
private val PILL_SHADOW = Shadow(
    radius = 12.dp,
    offset = DpOffset(0.dp, 2.dp),
    color = Color.Black.copy(alpha = 0.3f),
)

private class SelectionPillBackdrop(
    private val page: Backdrop?,
    private val bar: Backdrop?,
    private val tabs: Backdrop?,
    private val fill: () -> Color,
) : Backdrop {
    override val isCoordinatesDependent: Boolean = true

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) {
        try {
            if (page != null) {
                with(page) { drawBackdrop(density, coordinates, layerBlock) }
            }
        } catch (_: Throwable) {}
        try {
            if (bar != null) {
                with(bar) { drawBackdrop(density, coordinates, layerBlock) }
            }
        } catch (_: Throwable) {}
        try {
            val color = fill()
            if (color.alpha > 0f) {
                drawRect(
                    color = color,
                    topLeft = Offset(-size.width, -size.height),
                    size = Size(size.width * 3, size.height * 3),
                )
            }
        } catch (_: Throwable) {}
        try {
            if (tabs != null) {
                with(tabs) { drawBackdrop(density, coordinates, layerBlock) }
            }
        } catch (_: Throwable) {}
    }
}
