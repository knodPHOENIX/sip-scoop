package com.sipscoop.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sipscoop.app.R
import com.sipscoop.app.data.AppState
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * The opening animation: the DNA logo pops up in the middle, spins and melts into the three
 * rings, the rings glide up to their place and the rest of the page builds in around them.
 */
object Intro {
    /** True from app start until the opening animation is over. */
    val active = mutableStateOf(true)
    /** 0..1 along the animation; page parts use it to build in (see [introIn]). */
    val build = mutableFloatStateOf(0f)
    /** The real rings take over from the animation's rings once they are in place. */
    val handedOver = mutableStateOf(false)
    /** Where the real rings sit on screen (null in Classic view or before they are laid out). */
    val ringBounds = mutableStateOf<Rect?>(null)

    fun reset() {
        active.value = true; build.floatValue = 0f; handedOver.value = false
    }

    fun finish() {
        active.value = false; build.floatValue = 1f; handedOver.value = true
    }

    const val TOTAL_MS = 2000f
}

private fun seg(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
private fun outCubic(x: Float) = 1f - (1f - x).pow(3)
private fun inOut(x: Float) = if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f).pow(3) / 2f
private fun outBack(x: Float): Float {
    val c1 = 1.5f; val c3 = c1 + 1f
    return 1f + c3 * (x - 1f).pow(3) + c1 * (x - 1f).pow(2)
}

/**
 * Fades and slides a part of the page in while the opening animation runs.
 * [fromMs]..[toMs] is when it builds in; [dy] is where it slides from.
 */
fun Modifier.introIn(fromMs: Float, toMs: Float, dy: Dp): Modifier = composed {
    val px = with(LocalDensity.current) { dy.toPx() }
    graphicsLayer {
        val t = Intro.build.floatValue * Intro.TOTAL_MS
        val k = outCubic(seg(t, fromMs, toMs))
        alpha = k
        translationY = px * (1f - k)
    }
}

/** Drawn over everything at app start. Calls [onDone] when the page is fully built. */
@Composable
fun IntroOverlay(state: AppState, stepsOn: Boolean, onDone: () -> Unit) {
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        clock.animateTo(Intro.TOTAL_MS, tween(Intro.TOTAL_MS.toInt(), easing = LinearEasing)) {
            Intro.build.floatValue = value / Intro.TOTAL_MS
            if (value >= 1760f && !Intro.handedOver.value) Intro.handedOver.value = true
        }
        Intro.finish()
        onDone()
    }
    val t = clock.value
    var size by remember { mutableStateOf(IntSize.Zero) }
    val bounds = Intro.ringBounds.value
    val showRings = state.ringsView && bounds != null

    val colors = listOf(C.Gold, C.Blue, C.Orange)
    val tracks = listOf(C.GoldTrack, C.RingTrackWater, C.RingTrackProtein)
    val radius = listOf(0.441f, 0.339f, 0.237f)
    val fills = (0..2).map { state.fraction(it, stepsOn).coerceIn(0f, 1f) }

    // Timeline (ms): 0-450 logo pops in · 450-1150 logo spins and melts while the rings unwind ·
    // 1150-1750 rings glide to their place · 1300-1950 the page builds in around them.
    val pop = outBack(seg(t, 0f, 450f))
    val morph = inOut(seg(t, 450f, 1150f))
    val grow = outCubic(seg(t, 500f, 1150f))
    val lift = inOut(seg(t, 1150f, 1750f))
    val centre = Offset(size.width / 2f, size.height / 2f)
    val target = bounds?.center ?: centre
    val ringCentre = Offset(centre.x + (target.x - centre.x) * lift, centre.y + (target.y - centre.y) * lift)

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
    ) {
        // Dark start, fading to reveal the real page background.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 1f - seg(t, 1100f, 1700f) }
                .background(Color(0xFF0B0D10))
        )
        if (showRings && !Intro.handedOver.value) {
            val b = bounds!!
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = seg(t, 500f, 750f) }) {
                val side = b.width * (0.45f + 0.55f * grow)
                rotate(-120f * (1f - grow), ringCentre) {
                    for (i in 0..2) {
                        val near = if (i == 0) 1f else 0f
                        val w = side * (0.064f + 0.021f * near)
                        val settle = seg(t, 1250f, 1750f)
                        val a = 1f - (1f - (0.28f + 0.72f * near)) * settle
                        val sweep = outCubic(seg(t, 560f + i * 90f, 1150f + i * 90f))
                        val r = radius[i] * side
                        val tl = Offset(ringCentre.x - r, ringCentre.y - r)
                        val sz = Size(r * 2, r * 2)
                        drawArc(tracks[i], -90f, 360f * sweep, false, tl, sz, alpha = a, style = Stroke(w))
                        val f = fills[i] * sweep
                        if (f > 0.001f) drawArc(colors[i], -90f, 360f * f, false, tl, sz, alpha = a, style = Stroke(w, cap = StrokeCap.Round))
                    }
                }
            }
            // The step count in the middle counts up.
            val shown = if (stepsOn) (state.steps * outCubic(seg(t, 1000f, 1700f))).roundToInt() else state.steps
            Column(
                Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        alpha = seg(t, 1000f, 1300f)
                        translationX = ringCentre.x - centre.x
                        translationY = ringCentre.y - centre.y
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(fmt(shown), style = T.display(23, C.GoldText), maxLines = 1, textAlign = TextAlign.Center)
                Text("of ${fmt(state.stepGoal)}", style = T.bold(11, C.Muted), maxLines = 1, textAlign = TextAlign.Center)
            }
        }
        // The logo.
        val logoAlpha = minOf(seg(t, 0f, 200f), 1f - seg(t, 650f, 1100f))
        if (logoAlpha > 0f) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        val s = (0.55f + 0.45f * pop) * (1f + 0.35f * morph)
                        scaleX = s; scaleY = s
                        rotationZ = -200f * morph
                        alpha = logoAlpha
                    }
                    .blur((8f * seg(t, 700f, 1100f)).dp)
                    .shadow(18.dp, RoundedCornerShape(36.dp))
            ) {
                Image(
                    painterResource(R.drawable.logo_full),
                    contentDescription = "Sip & Scoop",
                    modifier = Modifier.size(156.dp).clip(RoundedCornerShape(36.dp))
                )
            }
        }
    }
}
