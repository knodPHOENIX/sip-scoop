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
    const val MOON = "M20 14.5 A8 8 0 1 1 9.5 4 A6.5 6.5 0 0 0 20 14.5 Z"
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
