package com.sipscoop.app.ui

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.sin
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/** Draws [block] in a fixed coordinate box, scaled to fit and centred in the canvas. */
fun DrawScope.inViewBox(minX: Float, minY: Float, w: Float, h: Float, block: DrawScope.() -> Unit) {
    val s = minOf(size.width / w, size.height / h)
    val dx = (size.width - w * s) / 2f - minX * s
    val dy = (size.height - h * s) / 2f - minY * s
    withTransform({
        translate(dx, dy)
        scale(s, s, Offset.Zero)
    }) {
        block()
    }
}

fun dropPath() = Path().apply {
    moveTo(226f, 66f)
    cubicTo(226f, 66f, 112f, 200f, 112f, 292f)
    arcTo(Rect(112f, 178f, 340f, 406f), 180f, -180f, false)
    cubicTo(340f, 200f, 226f, 66f, 226f, 66f)
    close()
}

fun eggPath() = Path().apply {
    moveTo(384f, 282f)
    cubicTo(356f, 282f, 336f, 332f, 336f, 364f)
    arcTo(Rect(336f, 316f, 432f, 412f), 180f, -180f, false)
    cubicTo(432f, 332f, 412f, 282f, 384f, 282f)
    close()
}

/** A curve from (x0,y) to (x1,y) bending down to [dip] — used for smiles and happy eyes. */
fun curve(x0: Float, y: Float, x1: Float, dip: Float) = Path().apply {
    val cx = (x0 + x1) / 2f
    moveTo(x0, y)
    cubicTo(x0 + (cx - x0) * 2f / 3f, y + (dip - y) * 2f / 3f, x1 + (cx - x1) * 2f / 3f, y + (dip - y) * 2f / 3f, x1, y)
}

fun DrawScope.drawDrip(body: Color = C.Blue, celebrate: Boolean = false, blink: Float = 1f) {
    drawPath(dropPath(), body)
    if (celebrate) {
        val st = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(curve(178f, 282f, 206f, 258f), C.Navy, style = st)
        drawPath(curve(248f, 282f, 276f, 258f), C.Navy, style = st)
        val mouth = Path().apply {
            moveTo(198f, 310f)
            cubicTo(206f, 340f, 248f, 340f, 256f, 310f)
            close()
        }
        drawPath(mouth, C.Navy)
    } else if (body == Color.White) {
        scale(1f, blink, Offset(227f, 288f)) {
            drawCircle(C.Navy, 19f, Offset(192f, 290f))
            drawCircle(C.Navy, 19f, Offset(262f, 290f))
            drawCircle(Color.White, 6f, Offset(199f, 282f))
            drawCircle(Color.White, 6f, Offset(269f, 282f))
        }
        drawPath(curve(206f, 326f, 248f, 350f), C.Navy, style = Stroke(width = 9f, cap = StrokeCap.Round))
    } else {
        scale(1f, blink, Offset(227f, 270f)) {
            drawCircle(Color.White, 20f, Offset(192f, 270f))
            drawCircle(Color.White, 20f, Offset(262f, 270f))
            drawCircle(C.Navy, 11f, Offset(196f, 274f))
            drawCircle(C.Navy, 11f, Offset(266f, 274f))
        }
        drawPath(curve(200f, 316f, 254f, 344f), C.Navy, style = Stroke(width = 10f, cap = StrokeCap.Round))
    }
    drawOval(C.Pink, topLeft = Offset(150f, 308f), size = Size(28f, 16f))
    drawOval(C.Pink, topLeft = Offset(276f, 308f), size = Size(28f, 16f))
}

fun DrawScope.drawEgg(body: Color = C.Orange, outline: Color? = null, celebrate: Boolean = false, blink: Float = 1f) {
    val p = eggPath()
    drawPath(p, body)
    if (outline != null) drawPath(p, outline, style = Stroke(width = 10f))
    if (celebrate) {
        val st = Stroke(width = 5f, cap = StrokeCap.Round)
        drawPath(curve(362f, 350f, 378f, 340f), C.Navy, style = st)
        drawPath(curve(390f, 350f, 406f, 340f), C.Navy, style = st)
    } else {
        scale(1f, blink, Offset(384f, 347f)) {
            drawCircle(Color.White, 9f, Offset(370f, 346f))
            drawCircle(Color.White, 9f, Offset(398f, 346f))
            drawCircle(C.Navy, 5f, Offset(372f, 348f))
            drawCircle(C.Navy, 5f, Offset(400f, 348f))
        }
    }
    drawPath(curve(372f, 370f, 396f, 382f), C.Navy, style = Stroke(width = 5f, cap = StrokeCap.Round))
}

/** Eyes close briefly every 4 seconds; [offsetMs] keeps two mascots from blinking in sync. */
@Composable
fun rememberBlink(offsetMs: Int = 0): Float {
    val inf = rememberInfiniteTransition(label = "blink")
    val v by inf.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 4000
                1f at 0
                1f at 3700
                0.08f at 3790
                1f at 3900
            },
            initialStartOffset = StartOffset(offsetMs)
        ),
        label = "blinkV"
    )
    return v
}

/** A slow, gentle wobble so the mascots always look alive. */
@Composable
private fun Modifier.jiggle(enabled: Boolean): Modifier {
    if (!enabled) return this
    val inf = rememberInfiniteTransition(label = "jiggle")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "jig")
    return this.graphicsLayer {
        val a = (t * 2.0 * Math.PI).toFloat()
        rotationZ = 3f * sin(a)
        scaleX = 1f + 0.02f * sin(a * 2f)
        scaleY = 1f - 0.02f * sin(a * 2f)
        transformOrigin = TransformOrigin(0.5f, 1f)
    }
}

/**
 * Makes a mascot playful: a quick tap gives a happy jiggle, and pressing and holding
 * swells it up like jelly, which wobbles back into shape on release.
 */
@Composable
private fun Modifier.mascotTouch(enabled: Boolean): Modifier {
    if (!enabled) return this
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val sx = remember { Animatable(1f) }
    val sy = remember { Animatable(1f) }
    val rot = remember { Animatable(0f) }
    return this
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    var held = false
                    val grow = scope.launch {
                        delay(220)
                        held = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        launch { rot.animateTo(0f, tween(150)) }
                        launch { sx.animateTo(1.18f, tween(520, easing = FastOutSlowInEasing)) }
                        sy.animateTo(1.12f, tween(520, easing = FastOutSlowInEasing))
                    }
                    tryAwaitRelease()
                    grow.cancel()
                    if (held) {
                        // Let go: snap back with a wobbly, jelly-like bounce.
                        scope.launch { sx.animateTo(1f, spring(dampingRatio = 0.22f, stiffness = 260f)) }
                        scope.launch { sy.animateTo(1f, spring(dampingRatio = 0.22f, stiffness = 420f)) }
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch {
                            rot.animateTo(0f, keyframes {
                                durationMillis = 620
                                -11f at 80
                                10f at 190
                                -7f at 300
                                5f at 410
                                -2f at 520
                            })
                        }
                        scope.launch {
                            sy.animateTo(1f, keyframes {
                                durationMillis = 520
                                0.86f at 90
                                1.12f at 220
                                0.95f at 340
                                1.03f at 440
                            })
                        }
                        scope.launch {
                            sx.animateTo(1f, keyframes {
                                durationMillis = 520
                                1.12f at 90
                                0.92f at 220
                                1.04f at 340
                                0.99f at 440
                            })
                        }
                    }
                }
            )
        }
        .graphicsLayer {
            scaleX = sx.value
            scaleY = sy.value
            rotationZ = rot.value
            transformOrigin = TransformOrigin(0.5f, 1f)
        }
}

/** Drip, the water drop. The box leaves room so nothing is ever cut off. */
@Composable
fun DripMascot(modifier: Modifier = Modifier, celebrate: Boolean = false, alive: Boolean = true) {
    val blink = if (alive) rememberBlink() else 1f
    Canvas(modifier.jiggle(alive).mascotTouch(alive)) {
        inViewBox(104f, 58f, 244f, 356f) { drawDrip(celebrate = celebrate, blink = blink) }
    }
}

@Composable
fun EggMascot(modifier: Modifier = Modifier, celebrate: Boolean = false, alive: Boolean = true) {
    val blink = if (alive) rememberBlink(900) else 1f
    Canvas(modifier.jiggle(alive).mascotTouch(alive)) {
        inViewBox(324f, 270f, 120f, 154f) { drawEgg(celebrate = celebrate, blink = blink) }
    }
}

/** Both mascots together, as on the app icon. */
@Composable
fun MascotPair(modifier: Modifier = Modifier, dripColor: Color = Color.White, celebrate: Boolean = false, alive: Boolean = true) {
    val b1 = if (alive) rememberBlink() else 1f
    val b2 = if (alive) rememberBlink(900) else 1f
    Canvas(modifier.jiggle(alive)) {
        inViewBox(100f, 56f, 344f, 366f) {
            drawDrip(body = dripColor, celebrate = celebrate, blink = b1)
            drawEgg(outline = Color.White, celebrate = celebrate, blink = b2)
        }
    }
}
