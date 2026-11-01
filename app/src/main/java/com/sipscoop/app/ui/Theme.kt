package com.sipscoop.app.ui

import android.content.Context
import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object C {
    val Blue = Color(0xFF1E9BF0)
    val BlueDark = Color(0xFF1477BE)
    val BlueSoft = Color(0xFFE3F4FE)
    val BlueCard = Color(0xFFEAF6FE)
    val BlueLight = Color(0xFF8ED3FA)
    val BlueLine = Color(0xFFB9D3E6)
    val BlueDeep = Color(0xFF1683D0)
    val Orange = Color(0xFFFF8A3D)
    val OrangeDark = Color(0xFFD9681F)
    val OrangeText = Color(0xFFC85E15)
    val OrangeSoft = Color(0xFFFFEEDF)
    val OrangeCard = Color(0xFFFFF6EE)
    val OrangeLine = Color(0xFFEBD9CA)
    val OrangeTrack = Color(0xFFF3E2D3)
    val Green = Color(0xFF58C443)
    val GreenDark = Color(0xFF46A332)
    val GreenText = Color(0xFF2F7A1E)
    val GreenSoft = Color(0xFFE5F7DF)
    val Yellow = Color(0xFFFFC800)
    val YellowDark = Color(0xFFE5A400)
    val YellowSoft = Color(0xFFFFF6D6)
    val YellowLine = Color(0xFFFFE48A)
    val YellowText = Color(0xFF7A5300)
    val Ink = Color(0xFF22303E)
    val Muted = Color(0xFF5B6B7B)
    val Line = Color(0xFFD6E2EC)
    val Faint = Color(0xFFF1F6FA)
    val Navy = Color(0xFF17324D)
    val Pink = Color(0xFFFFB3C1)
    val Red = Color(0xFFC2362B)
    val Grey = Color(0xFFE5EBF0)
    val GreyText = Color(0xFF6B7A88)
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

val WaterAccent = Accent(C.Blue, C.BlueDark, C.BlueDark, C.BlueSoft, C.BlueCard, C.Line)
val ProteinAccent = Accent(C.Orange, C.OrangeDark, C.OrangeText, C.OrangeSoft, C.OrangeCard, C.OrangeLine)

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
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.Blue,
            onPrimary = Color.White,
            secondary = C.Orange,
            onSecondary = Color.White,
            background = Color.White,
            onBackground = C.Ink,
            surface = Color.White,
            onSurface = C.Ink
        ),
        content = content
    )
}
