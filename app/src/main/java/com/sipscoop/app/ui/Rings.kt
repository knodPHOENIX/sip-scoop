package com.sipscoop.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sipscoop.app.data.AppState
import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.delay

private val Glide = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/** Soft background tint for the rings view, following the swipe. */
fun ringsTint(position: Float): Color {
    val p = position.coerceIn(0f, 2f)
    return if (p <= 1f) androidx.compose.ui.graphics.lerp(C.TintSteps, C.TintWater, p)
    else androidx.compose.ui.graphics.lerp(C.TintWater, C.TintProtein, p - 1f)
}

private fun AppState.fraction(i: Int, stepsOn: Boolean): Float {
    val track = Track.entries[i]
    if (track == Track.STEPS && !stepsOn) return 0f
    return todayOf(track).toFloat() / maxOf(1, goalOf(track))
}

/**
 * One set of three rings for steps (outside), water and protein (inside).
 * The ring for the page you're on is thick and bright; tap a ring to jump to its page.
 * The rings fill up from empty every time this view appears.
 */
@Composable
fun SharedRings(state: AppState, position: Float, stepsOn: Boolean, onPick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = listOf(C.Gold, C.Blue, C.Orange)
    val tracks = listOf(C.GoldTrack, C.RingTrackWater, C.RingTrackProtein)
    val radius = listOf(0.441f, 0.339f, 0.237f)

    val fills = (0..2).map { i ->
        var started by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(180L * i)
            started = true
        }
        val target = state.fraction(i, stepsOn).coerceIn(0f, 1f)
        animateFloatAsState(if (started) target else 0f, tween(1600, easing = Glide), label = "ring$i").value
    }

    val current = position.coerceIn(0f, 2f).let { kotlin.math.round(it).toInt() }

    Box(
        modifier.semantics { contentDescription = "Progress rings. Tap a ring to open steps, water or protein." },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tap ->
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val dist = hypot(tap.x - c.x, tap.y - c.y)
                        val side = minOf(size.width, size.height).toFloat()
                        // A touch area a bit wider than the visible ring, so it's easy to hit.
                        val reach = side * 0.06f + 8.dp.toPx()
                        val hit = radius.indices.minByOrNull { abs(dist - radius[it] * side) }
                        if (hit != null && abs(dist - radius[hit] * side) <= reach) onPick(hit)
                    }
                }
        ) {
            val side = size.minDimension
            val c = Offset(size.width / 2f, size.height / 2f)
            for (i in 0..2) {
                val near = (1f - abs(position - i)).coerceIn(0f, 1f)
                val w = side * (0.064f + 0.021f * near)
                val alpha = 0.28f + 0.72f * near
                val r = radius[i] * side
                val tl = Offset(c.x - r, c.y - r)
                val sz = Size(r * 2, r * 2)
                drawArc(tracks[i], 0f, 360f, false, tl, sz, alpha = alpha, style = Stroke(w))
                if (fills[i] > 0.001f) {
                    drawArc(colors[i], -90f, 360f * fills[i], false, tl, sz, alpha = alpha, style = Stroke(w, cap = StrokeCap.Round))
                }
            }
        }
        // Centre text lives inside the innermost ring only.
        Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = current,
                transitionSpec = { (fadeIn(tween(250)) + scaleIn(tween(250), 0.92f)) togetherWith fadeOut(tween(150)) },
                label = "ringCentre"
            ) { k ->
                val (value, of, color) = when (k) {
                    0 -> Triple(fmt(state.steps), "of ${fmt(state.stepGoal)}", C.GoldText)
                    1 -> Triple(fmt(state.waterTotal), "of ${fmt(state.waterGoal)} ml", C.BlueDark)
                    else -> Triple("${state.proteinTotal} g", "of ${state.proteinGoal} g", C.OrangeText)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, style = T.display(23, color), maxLines = 1, textAlign = TextAlign.Center)
                    Text(of, style = T.bold(11, C.Muted), maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** Steps 78% · Water 58% · Protein 70%. Tap to jump. */
@Composable
fun RingChips(state: AppState, position: Float, stepsOn: Boolean, onPick: (Int) -> Unit) {
    val current = kotlin.math.round(position.coerceIn(0f, 2f)).toInt()
    val names = listOf("Steps", "Water", "Protein")
    val dots = listOf(C.Gold, C.Blue, C.Orange)
    val accents = listOf(StepAccent, WaterAccent, ProteinAccent)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        names.forEachIndexed { i, name ->
            val on = i == current
            val bg by animateColorAsState(if (on) accents[i].soft else C.Surface, tween(300), label = "chipBg")
            val edge by animateColorAsState(if (on) accents[i].line else C.Line, tween(300), label = "chipEdge")
            val text by animateColorAsState(if (on) accents[i].text else C.Muted, tween(300), label = "chipText")
            val pct = (state.fraction(i, stepsOn) * 100).toInt()
            Row(
                Modifier
                    .background(bg, CircleShape)
                    .border(2.dp, edge, CircleShape)
                    .clickable { onPick(i) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(9.dp).background(dots[i], CircleShape))
                Text("$name $pct%", style = T.bold(13, text), maxLines = 1)
            }
        }
    }
}
