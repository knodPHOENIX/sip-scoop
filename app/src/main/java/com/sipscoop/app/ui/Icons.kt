package com.sipscoop.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Simple line icons, drawn on a 24×24 grid. */
object Ic {
    const val PENCIL = "M4 20 L5 15.5 L15.6 4.9 A2 2 0 0 1 18.4 4.9 L19.1 5.6 A2 2 0 0 1 19.1 8.4 L8.5 19 Z M13.8 6.7 L17.3 10.2"
    const val BACK = "M15 5 L8 12 L15 19"
    const val CHEVRON = "M9 6 L15 12 L9 18"
    const val TRASH = "M5 7 L19 7 M10 7 L10 4.5 L14 4.5 L14 7 M7 7 L8 20 L16 20 L17 7"
    const val BARS = "M5 20 L5 13 M12 20 L12 5 M19 20 L19 9"
    const val RESET = "M4.5 12 A7.5 7.5 0 1 0 7 6.4 M4 3.5 L4 8 L8.5 8"
    const val SLIDERS = "M4 7 L6.5 7 M11.5 7 L20 7 M4 17 L12.5 17 M17.5 17 L20 17 M9 4.5 A2.5 2.5 0 1 0 9 9.5 A2.5 2.5 0 1 0 9 4.5 Z M15 14.5 A2.5 2.5 0 1 0 15 19.5 A2.5 2.5 0 1 0 15 14.5 Z"
    const val GIFT = "M4 10 L20 10 L20 20 L4 20 Z M3 7 L21 7 L21 10 L3 10 Z M12 7 L12 20 M12 7 C10.5 3.5 6.5 4 8 7 M12 7 C13.5 3.5 17.5 4 16 7"
    const val LOGOUT = "M14 4 L19 4 L19 20 L14 20 M10 8 L6 12 L10 16 M6 12 L15 12"
    const val BUG = "M8 8 A4 4 0 0 1 16 8 L16 15 A4 4 0 0 1 8 15 Z M3 11 L8 11 M16 11 L21 11 M4 17 L8 15 M20 17 L16 15 M9 5 L7 3 M15 5 L17 3"
    const val CHECK = "M5 12.5 L10 17 L19 7"
    const val PHONE = "M9.5 3 L14.5 3 A2.5 2.5 0 0 1 17 5.5 L17 18.5 A2.5 2.5 0 0 1 14.5 21 L9.5 21 A2.5 2.5 0 0 1 7 18.5 L7 5.5 A2.5 2.5 0 0 1 9.5 3 Z"
    const val DROP = "M12 3 C12 3 5 11 5 15 A7 7 0 0 0 19 15 C19 11 12 3 12 3 Z"
    const val EGG = "M12 3 C8.5 3 6 9 6 13.5 A6 6 0 0 0 18 13.5 C18 9 15.5 3 12 3 Z"
    const val BOLT = "M13.5 2 L4.5 13.5 L11 13.5 L9.5 22 L19.5 9.5 L13 9.5 Z"
    const val PIN = "M12 21 C12 21 5 13.5 5 9 A7 7 0 0 1 19 9 C19 13.5 12 21 12 21 Z M12 6.5 A2.5 2.5 0 1 0 12 11.5 A2.5 2.5 0 1 0 12 6.5 Z"
    const val FLAME = "M12 22 C7.5 22 5 18.8 5 15.5 C5 11 9 8.5 10 3 C13 5 14 7.5 14 10 C15 9 15.6 8 15.8 6.5 C18 8.5 19 11.8 19 15.5 C19 18.8 16.5 22 12 22 Z"
    // Running shoe facing left (top bar streak, Steps tab).
    const val SHOE_BODY = "M22 15.5 V9.6 C22 8.6 21.1 7.9 20.1 8.2 L17.8 8.9 C17 9.1 16.2 8.8 15.7 8.2 L14 6 C13.5 5.4 12.6 5.4 12.1 5.9 C10.4 7.8 8.2 9.2 5.7 9.9 C3.5 10.5 2 11.9 2 13.7 V15.5 Z"
    const val SHOE_SOLE = "M3.3 15 H20.7 A1.7 1.7 0 0 1 20.7 18.4 H3.3 A1.7 1.7 0 0 1 3.3 15 Z"
    const val SHOE = SHOE_BODY + " " + SHOE_SOLE
    const val MOON = "M20 14.5 A8 8 0 1 1 9.5 4 A6.5 6.5 0 0 0 20 14.5 Z"
    const val SEARCH = "M10.5 4 A6.5 6.5 0 1 0 10.5 17 A6.5 6.5 0 1 0 10.5 4 Z M15.5 15.5 L20 20"
    const val CLOSE = "M6 6 L18 18 M18 6 L6 18"
    const val GLOBE = "M12 3 A9 9 0 1 0 12 21 A9 9 0 1 0 12 3 Z M3 12 L21 12 M12 3 C15 6 15 18 12 21 C9 18 9 6 12 3"
    const val CLOCK = "M12 3 A9 9 0 1 0 12 21 A9 9 0 1 0 12 3 Z M12 7 L12 12 L15.5 14"
    const val BOX = "M5 8 L12 4 L19 8 L19 16 L12 20 L5 16 Z M5 8 L12 12 L19 8 M12 12 L12 20"
    const val STAR = "M12 3 L14.6 8.8 L21 9.4 L16.2 13.6 L17.6 20 L12 16.7 L6.4 20 L7.8 13.6 L3 9.4 L9.4 8.8 Z"
    const val BELL = "M6 16 L6 11 A6 6 0 0 1 18 11 L18 16 L20 18 L4 18 Z M10 20.5 A2 2 0 0 0 14 20.5"
    const val GEM = "M7 3 L17 3 L22 9 L12 21 L2 9 Z"
    const val GEM_LINES = "M2 9 L22 9 M7 3 L12 9 L17 3 M12 9 L12 21"
}

@Composable
fun LineIcon(d: String, color: Color, size: Dp = 18.dp, width: Float = 2.2f) {
    Canvas(Modifier.size(size)) {
        inViewBox(0f, 0f, 24f, 24f) {
            drawPath(svg(d), color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun FillIcon(d: String, color: Color, size: Dp = 18.dp) {
    Canvas(Modifier.size(size)) {
        inViewBox(0f, 0f, 24f, 24f) { drawPath(svg(d), color) }
    }
}

/** The gold running shoe used for the step streak. */
@Composable
fun ShoeIcon(size: Dp = 20.dp, body: Color = C.Yellow, sole: Color = C.YellowDark, laces: Color = Color.White) {
    Canvas(Modifier.size(size)) {
        inViewBox(0f, 0f, 24f, 24f) {
            drawPath(svg(Ic.SHOE_BODY), body)
            drawPath(svg(Ic.SHOE_SOLE), sole)
            drawCircle(laces, 0.9f, androidx.compose.ui.geometry.Offset(11.4f, 8.4f))
            drawCircle(laces, 0.9f, androidx.compose.ui.geometry.Offset(9.4f, 9.7f))
        }
    }
}
