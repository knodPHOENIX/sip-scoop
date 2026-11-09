package com.sipscoop.app.ui

import android.content.Context
import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Whether the app is currently drawn in dark mode. Set by MainActivity. */
object ThemeState {
    var dark by mutableStateOf(false)
}

private fun pick(light: Long, dark: Long): Color = Color(if (ThemeState.dark) dark else light)

/**
 * Every colour in the app. Colours that change between light and dark mode are
 * getters, so screens redraw by themselves when the mode changes.
 */
object C {
    // same in both modes
    val Blue = Color(0xFF1E9BF0)
    val BlueDeep = Color(0xFF1683D0)
    val Orange = Color(0xFFFF8A3D)
    val Green = Color(0xFF58C443)
    val Yellow = Color(0xFFFFC800)
    val YellowDark = Color(0xFFE5A400)
    val Navy = Color(0xFF17324D)
    val Pink = Color(0xFFFFB3C1)
    val RedFace = Color(0xFFE5483B)

    // change with the mode
    val Bg: Color get() = pick(0xFFFFFFFF, 0xFF0F1923)
    val Surface: Color get() = pick(0xFFFFFFFF, 0xFF17232F)
    val Ink: Color get() = pick(0xFF22303E, 0xFFEAF2F8)
    val Muted: Color get() = pick(0xFF5B6B7B, 0xFF9DB0C1)
    val Line: Color get() = pick(0xFFD6E2EC, 0xFF2C3D4D)
    val NavLine: Color get() = pick(0xFFE6EDF3, 0xFF22313F)
    val Faint: Color get() = pick(0xFFF1F6FA, 0xFF1C2A37)
    val Grey: Color get() = pick(0xFFE5EBF0, 0xFF26333F)
    val GreyText: Color get() = pick(0xFF6B7A88, 0xFF8596A6)

    val BlueEdge: Color get() = pick(0xFF1477BE, 0xFF0F6CAD)
    val BlueDark: Color get() = pick(0xFF1477BE, 0xFF7CCBFA) // blue text
    val BlueSoft: Color get() = pick(0xFFE3F4FE, 0xFF13324A)
    val BlueCard: Color get() = pick(0xFFEAF6FE, 0xFF142B3E)
    val BlueLight: Color get() = pick(0xFF8ED3FA, 0xFF3F8FC4)
    val BlueLine: Color get() = pick(0xFFB9D3E6, 0xFF35546B)
    val Glass: Color get() = pick(0xFFF1F8FD, 0xFF152536)

    val OrangeDark: Color get() = pick(0xFFD9681F, 0xFFB85512) // orange edge
    val OrangeText: Color get() = pick(0xFFC85E15, 0xFFFFB27A)
    val OrangeSoft: Color get() = pick(0xFFFFEEDF, 0xFF3A2618)
    val OrangeCard: Color get() = pick(0xFFFFF6EE, 0xFF261A12)
    val OrangeLine: Color get() = pick(0xFFEBD9CA, 0xFF4A3526)
    val OrangeTrack: Color get() = pick(0xFFF3E2D3, 0xFF3A2A1E)

    val GreenDark: Color get() = pick(0xFF46A332, 0xFF3E8F2C)
    val GreenText: Color get() = pick(0xFF2F7A1E, 0xFF9BE58A)
    val GreenSoft: Color get() = pick(0xFFE5F7DF, 0xFF1C361C)

    val YellowSoft: Color get() = pick(0xFFFFF6D6, 0xFF30270C)
    val YellowLine: Color get() = pick(0xFFFFE48A, 0xFF5E4A12)
    val YellowText: Color get() = pick(0xFF7A5300, 0xFFFFD95A)

    val Red: Color get() = pick(0xFFC2362B, 0xFFFF7A6E)
    val RedSoft: Color get() = pick(0xFFFDECEA, 0xFF3A1C1A)
    val RedLine: Color get() = pick(0xFFF0CFCB, 0xFF5A2A26)
    val RedEdge: Color get() = pick(0xFFB0301F, 0xFF9E2A1D)
    val PinkSoft: Color get() = pick(0xFFFCE8F1, 0xFF3A1E2D)
    val PinkText: Color get() = pick(0xFFB8327A, 0xFFFF9BC8)

    // Steps: light golden yellow
    val Gold = Color(0xFFF5B700)
    val GoldEdge: Color get() = pick(0xFFC99300, 0xFFA37700)
    val GoldText: Color get() = pick(0xFF8A6100, 0xFFFFD666)
    val GoldSoft: Color get() = pick(0xFFFFF4CC, 0xFF33290A)
    val GoldCard: Color get() = pick(0xFFFFF9E6, 0xFF241E0C)
    val GoldLine: Color get() = pick(0xFFF0DFA8, 0xFF55451A)
    val GoldTrack: Color get() = pick(0xFFFBEFC6, 0xFF2E2710)
    val GoldIdle: Color get() = pick(0xFFF3E6BE, 0xFF3A3218)
    val GoldHit = Color(0xFFF5C83A)

    val BlueStreakLine: Color get() = pick(0xFFB5DDF8, 0xFF1F4F73)
}

/** Colours for one tracker (water or protein). */
data class Accent(
    val face: Color,
    val edge: Color,
    val text: Color,
    val soft: Color,
    val card: Color,
    val line: Color
)

val WaterAccent: Accent get() = Accent(C.Blue, C.BlueEdge, C.BlueDark, C.BlueSoft, C.BlueCard, C.Line)
val ProteinAccent: Accent get() = Accent(C.Orange, C.OrangeDark, C.OrangeText, C.OrangeSoft, C.OrangeCard, C.OrangeLine)
val StepAccent: Accent get() = Accent(C.Gold, C.GoldEdge, C.GoldText, C.GoldSoft, C.GoldCard, C.GoldLine)

/**
 * Rounded fonts (Baloo 2 for headings, Nunito for text) are downloaded into
 * assets/fonts during the GitHub build. If they are missing, the phone's own font is used.
 */
object Fonts {
    var display: FontFamily = FontFamily.SansSerif
        private set
    var body: FontFamily = FontFamily.SansSerif
        private set
    var bodyBold: FontFamily = FontFamily.SansSerif
        private set
    private var loaded = false

    fun init(context: Context) {
        if (loaded) return
        loaded = true
        val files = runCatching { context.assets.list("fonts")?.toList() }.getOrNull().orEmpty()
        fun make(file: String, weight: Int): FontFamily? {
            if (file !in files) return null
            return runCatching {
                val tf: Typeface = Typeface.Builder(context.assets, "fonts/$file")
                    .setFontVariationSettings("'wght' $weight")
                    .build()
                FontFamily(tf)
            }.getOrNull()
        }
        make("baloo2.ttf", 800)?.let { display = it }
        make("nunito.ttf", 700)?.let { body = it }
        make("nunito.ttf", 800)?.let { bodyBold = it }
    }
}

object T {
    fun display(size: Int, color: Color = C.Ink) = TextStyle(
        fontFamily = Fonts.display, fontWeight = FontWeight.ExtraBold, fontSize = size.sp, color = color,
        lineHeight = (size * 1.15f).sp
    )

    fun body(size: Int, color: Color = C.Ink) = TextStyle(
        fontFamily = Fonts.body, fontWeight = FontWeight.Bold, fontSize = size.sp, color = color,
        lineHeight = (size * 1.35f).sp
    )

    fun bold(size: Int, color: Color = C.Ink) = TextStyle(
        fontFamily = Fonts.bodyBold, fontWeight = FontWeight.ExtraBold, fontSize = size.sp, color = color,
        lineHeight = (size * 1.3f).sp
    )

    fun label(color: Color = C.Muted) = TextStyle(
        fontFamily = Fonts.bodyBold, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = color,
        letterSpacing = 1.2.sp
    )
}

@Composable
fun SipTheme(content: @Composable () -> Unit) {
    val scheme = if (ThemeState.dark) {
        darkColorScheme(
            primary = C.Blue, onPrimary = Color.White,
            secondary = C.Orange, onSecondary = Color.White,
            background = C.Bg, onBackground = C.Ink,
            surface = C.Surface, onSurface = C.Ink,
            surfaceVariant = C.Faint, onSurfaceVariant = C.Muted,
            outline = C.Line
        )
    } else {
        lightColorScheme(
            primary = C.Blue, onPrimary = Color.White,
            secondary = C.Orange, onSecondary = Color.White,
            background = C.Bg, onBackground = C.Ink,
            surface = C.Surface, onSurface = C.Ink,
            surfaceVariant = C.Faint, onSurfaceVariant = C.Muted,
            outline = C.Line
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
