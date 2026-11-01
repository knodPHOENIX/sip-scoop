package com.sipscoop.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

private fun wave(base: Float, amp: Float, phase: Float, width: Float, bottom: Float): Path = Path().apply {
    moveTo(0f, bottom)
    var x = 0f
    while (x <= width) {
        val y = base + amp * sin(2.0 * PI * (x / 80.0 + phase)).toFloat()
        lineTo(x, y)
        x += 4f
    }
    lineTo(width, bottom)
    close()
}

/** A glass whose water rises smoothly and keeps gently waving. */
@Composable
fun WaterGlass(fraction: Float, modifier: Modifier = Modifier) {
    val level by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(1100, easing = FastOutSlowInEasing),
        label = "level"
    )
    val inf = rememberInfiniteTransition(label = "water")
    val phase by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "phase")
    val phase2 by inf.animateFloat(1f, 0f, infiniteRepeatable(tween(4600, easing = LinearEasing)), label = "phase2")
    val rise by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "rise")

    Canvas(modifier) {
        inViewBox(0f, 0f, 160f, 200f) {
            val outer = Path().apply {
                moveTo(14f, 10f); lineTo(146f, 10f); lineTo(131f, 192f); lineTo(29f, 192f); close()
            }
            val inner = Path().apply {
                moveTo(18f, 13f); lineTo(142f, 13f); lineTo(128f, 189f); lineTo(32f, 189f); close()
            }
            drawPath(outer, Color(0xFFF1F8FD))
            if (level > 0.003f) {
                clipPath(inner) {
                    val base = 14f + (1f - level) * 176f
                    drawPath(wave(base + 3f, 5f, phase2, 160f, 200f), C.BlueLight)
                    drawPath(wave(base, 6f, phase, 160f, 200f), C.Blue)
                    // bubbles drifting up through the water
                    listOf(Triple(56f, 5f, 0f), Triple(98f, 3.5f, 0.37f), Triple(80f, 2.5f, 0.68f)).forEach { (x, r, off) ->
                        val k = (rise + off) % 1f
                        val y = 192f - k * (192f - base)
                        if (y > base + 6f) drawCircle(C.BlueLight, r, Offset(x, y), alpha = 1f - k * 0.7f)
                    }
                }
            }
            drawPath(outer, C.BlueLine, style = Stroke(width = 6f, join = StrokeJoin.Round))
            drawLine(Color.White.copy(alpha = 0.75f), Offset(36f, 30f), Offset(46f, 160f), strokeWidth = 7f, cap = StrokeCap.Round)
        }
    }
}

/** Chunky protein bar that slides forward, with a shine running across it. */
@Composable
fun ProteinBar(fraction: Float, modifier: Modifier = Modifier) {
    val w by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "bar"
    )
    val inf = rememberInfiniteTransition(label = "shine")
    val shine by inf.animateFloat(-0.3f, 1.3f, infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing)), label = "shine")
    Box(
        modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(CircleShape)
            .background(C.OrangeTrack)
    ) {
        if (w > 0.005f) {
            Box(
                Modifier
                    .fillMaxWidth(w)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(C.Orange)
                    .drawBehind {
                        drawRect(Color(0xFFE46F22), topLeft = Offset(0f, size.height - 6.dp.toPx()), size = Size(size.width, 6.dp.toPx()))
                        val sw = 50.dp.toPx()
                        drawRoundRect(
                            Color(0xFFFFC08F),
                            topLeft = Offset(shine * size.width - sw / 2f, 4.dp.toPx()),
                            size = Size(sw, 5.dp.toPx()),
                            cornerRadius = CornerRadius(3.dp.toPx())
                        )
                    }
            )
        }
    }
}

/** Four milestone badges along the way to the protein goal. */
@Composable
fun Milestones(total: Int, goal: Int) {
    val marks = listOf(0.25f, 0.5f, 0.75f, 1f).map { Math.round(goal * it) }
    val nextIdx = marks.indexOfFirst { total < it }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        marks.forEachIndexed { i, m ->
            val done = total >= m
            val next = i == nextIdx
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MilestoneDot(done = done, next = next, trophy = i == 3)
                Text("$m g", style = T.bold(12, if (next) C.OrangeText else C.Muted))
            }
        }
    }
}

@Composable
private fun MilestoneDot(done: Boolean, next: Boolean, trophy: Boolean) {
    val bg by animateColorAsState(
        targetValue = when {
            done -> C.Green
            next -> Color.White
            else -> Color(0xFFECE4DC)
        },
        animationSpec = tween(400),
        label = "dot"
    )
    val scale by animateFloatAsState(
        targetValue = if (done) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow),
        label = "dotScale"
    )
    val inf = rememberInfiniteTransition(label = "ring")
    val ring by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Restart), label = "ringA")
    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
        if (next) {
            Canvas(Modifier.size(42.dp)) {
                drawCircle(C.Orange.copy(alpha = 0.35f * (1f - ring)), radius = size.minDimension / 2f * (0.75f + 0.25f * ring))
            }
        }
        Box(
            Modifier
                .size(34.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(bg)
                .then(if (next) Modifier.border(3.dp, C.Orange, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(18.dp)) {
                inViewBox(0f, 0f, 24f, 24f) {
                    when {
                        done -> {
                            val tick = Path().apply { moveTo(5f, 12.5f); lineTo(10f, 17f); lineTo(19f, 7f) }
                            drawPath(tick, Color.White, style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                        next -> drawCircle(C.Orange, 6f, Offset(12f, 12f))
                        trophy -> {
                            val cup = Path().apply {
                                moveTo(7f, 4f); lineTo(17f, 4f); lineTo(17f, 9f)
                                cubicTo(17f, 15.5f, 7f, 15.5f, 7f, 9f); close()
                                moveTo(12f, 14f); lineTo(12f, 17f)
                                moveTo(8f, 20f); lineTo(16f, 20f)
                            }
                            drawPath(cup, Color(0xFF8C7F73), style = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                        else -> drawCircle(Color(0xFFCBBFB4), 5f, Offset(12f, 12f))
                    }
                }
            }
        }
    }
}
