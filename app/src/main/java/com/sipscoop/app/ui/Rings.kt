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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
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
 * All three rings are always shown in full colour; the one for the page you're on is a
 * little broader, and the widths glide as you swipe. Tap a ring to jump to its page.
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
                // Subtle but clear: about 14 vs 9 on a 220 wide set of rings.
                val w = side * (0.041f + 0.023f * near)
                val alpha = 1f
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

/** Today's numbers under the rings, each in its own colour: 6,240 · 1,750 ml · 84 g. Tap to jump. */
@Composable
fun RingChips(
    state: AppState,
    position: Float,
    stepsOn: Boolean,
    onHold: (Int, androidx.compose.ui.geometry.Rect) -> Unit = { _, _ -> },
    onPick: (Int) -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val current = kotlin.math.round(position.coerceIn(0f, 2f)).toInt()
    val names = listOf("Steps", "Water", "Protein")
    val dots = listOf(C.Gold, C.Blue, C.Orange)
    val accents = listOf(StepAccent, WaterAccent, ProteinAccent)
    val values = listOf(
        if (stepsOn) fmt(state.steps) else "–",
        "${fmt(state.waterTotal)} ml",
        "${state.proteinTotal} g"
    )
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        names.forEachIndexed { i, name ->
            val on = i == current
            // The chip of the page you're on is a little brighter.
            val strength by animateFloatAsState(if (on) 1f else 0f, tween(300), label = "chipOn")
            var bounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
            Row(
                Modifier
                    .weight(1f)
                    .onGloballyPositioned { bounds = it.boundsInRoot() }
                    .semantics { contentDescription = if (i == 0) "$name ${values[i]}" else "$name ${values[i]}. Hold to edit." }
                    .glass(CircleShape, tint = accents[i].face)
                    .border(1.dp, dots[i].copy(alpha = 0.28f + 0.42f * strength), CircleShape)
                    .background(accents[i].face.copy(alpha = 0.10f * strength), CircleShape)
                    .clip(CircleShape)
                    .pointerInput(i) {
                        detectTapGestures(
                            onTap = { onPick(i) },
                            // Water and protein: hold to open their edits (the chip grows open).
                            onLongPress = {
                                if (i > 0) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onHold(i, bounds)
                                }
                            }
                        )
                    }
                    .padding(horizontal = 6.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                Box(Modifier.size(7.dp).background(dots[i], CircleShape))
                Text(values[i], style = T.bold(13, accents[i].text), maxLines = 1, softWrap = false)
                if (i > 0) LineIcon(Ic.PENCIL, accents[i].text.copy(alpha = 0.75f), 11.dp, 2f)
            }
        }
    }
}
