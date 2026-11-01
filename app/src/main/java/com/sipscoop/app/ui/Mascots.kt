package com.sipscoop.app.ui

import androidx.compose.foundation.Canvas
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

fun DrawScope.drawDrip(body: Color = C.Blue, celebrate: Boolean = false) {
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
        drawCircle(C.Navy, 19f, Offset(192f, 290f))
        drawCircle(C.Navy, 19f, Offset(262f, 290f))
        drawCircle(Color.White, 6f, Offset(199f, 282f))
        drawCircle(Color.White, 6f, Offset(269f, 282f))
        drawPath(curve(206f, 326f, 248f, 350f), C.Navy, style = Stroke(width = 9f, cap = StrokeCap.Round))
    } else {
        drawCircle(Color.White, 20f, Offset(192f, 270f))
        drawCircle(Color.White, 20f, Offset(262f, 270f))
        drawCircle(C.Navy, 11f, Offset(196f, 274f))
        drawCircle(C.Navy, 11f, Offset(266f, 274f))
        drawPath(curve(200f, 316f, 254f, 344f), C.Navy, style = Stroke(width = 10f, cap = StrokeCap.Round))
    }
    drawOval(C.Pink, topLeft = Offset(150f, 308f), size = Size(28f, 16f))
    drawOval(C.Pink, topLeft = Offset(276f, 308f), size = Size(28f, 16f))
}

fun DrawScope.drawEgg(body: Color = C.Orange, outline: Color? = null, celebrate: Boolean = false) {
    val p = eggPath()
    drawPath(p, body)
    if (outline != null) drawPath(p, outline, style = Stroke(width = 10f))
    if (celebrate) {
        val st = Stroke(width = 5f, cap = StrokeCap.Round)
        drawPath(curve(362f, 350f, 378f, 340f), C.Navy, style = st)
        drawPath(curve(390f, 350f, 406f, 340f), C.Navy, style = st)
    } else {
        drawCircle(Color.White, 9f, Offset(370f, 346f))
        drawCircle(Color.White, 9f, Offset(398f, 346f))
        drawCircle(C.Navy, 5f, Offset(372f, 348f))
        drawCircle(C.Navy, 5f, Offset(400f, 348f))
    }
    drawPath(curve(372f, 370f, 396f, 382f), C.Navy, style = Stroke(width = 5f, cap = StrokeCap.Round))
}

/** Drip, the water drop. The box leaves room so nothing is ever cut off. */
@Composable
fun DripMascot(modifier: Modifier = Modifier, celebrate: Boolean = false) {
    Canvas(modifier) {
        inViewBox(104f, 58f, 244f, 356f) { drawDrip(celebrate = celebrate) }
    }
}

@Composable
fun EggMascot(modifier: Modifier = Modifier, celebrate: Boolean = false) {
    Canvas(modifier) {
        inViewBox(324f, 270f, 120f, 154f) { drawEgg(celebrate = celebrate) }
    }
}

/** Both mascots together, as on the app icon. */
@Composable
fun MascotPair(modifier: Modifier = Modifier, dripColor: Color = Color.White, celebrate: Boolean = false) {
    Canvas(modifier) {
        inViewBox(100f, 56f, 344f, 366f) {
            drawDrip(body = dripColor, celebrate = celebrate)
            drawEgg(outline = Color.White, celebrate = celebrate)
        }
    }
}
