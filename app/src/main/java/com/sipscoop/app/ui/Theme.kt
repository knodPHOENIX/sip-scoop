package com.sipscoop.app.ui

import android.content.Context
import com.sipscoop.app.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Whether the app is currently drawn in dark mode. Set by MainActivity. */
object ThemeState {
    var dark by mutableStateOf(false)
}

private fun pick(light: Long, dark: Long): Color = Color(if (ThemeState.dark) dark else light)

/**
 * Every colour in the app (professional look: calm neutrals, one clear colour per tracker).
 * Colours that change between light and dark mode are getters, so screens redraw by
 * themselves when the mode changes.
 */
object C {
    // tracker colours: a little deeper in light mode, brighter on the dark background
    val Blue: Color get() = pick(0xFF2F80ED, 0xFF4EA8FF)
    val BlueDeep: Color get() = pick(0xFF1F6FD6, 0xFF3B92F0)
    val Orange: Color get() = pick(0xFFF26B2C, 0xFFFF7A45)
    val Green: Color get() = pick(0xFF2BB04F, 0xFF30D158)
    val Yellow: Color get() = pick(0xFFE8A317, 0xFFF5B940)
    val YellowDark: Color get() = pick(0xFFC98A0C, 0xFFD9A030)
    val Navy = Color(0xFF17324D)
    val Pink = Color(0xFFFFB3C1)
    val RedFace = Color(0xFFE5483B)

    // neutrals
    val Bg: Color get() = pick(0xFFFFFFFF, 0xFF0B0D10)
    val Surface: Color get() = pick(0xFFFFFFFF, 0xFF15181D)
    val Ink: Color get() = pick(0xFF111418, 0xFFF2F4F7)
    val Muted: Color get() = pick(0xFF6B7280, 0xFF8B93A1)
    val Line: Color get() = pick(0xFFE3E6EA, 0xFF2A2F37)
    val NavLine: Color get() = pick(0xFFEEF0F3, 0xFF1C2027)
    val Faint: Color get() = pick(0xFFF4F5F7, 0xFF1A1D23)
    val Grey: Color get() = pick(0xFFE9EBEE, 0xFF23272E)
    val GreyText: Color get() = pick(0xFF7A818C, 0xFF6B7380)

    val BlueEdge: Color get() = pick(0xFF2A73D6, 0xFF3D95EC)
    val BlueDark: Color get() = pick(0xFF1F6FD6, 0xFF6CB6FF) // blue text
    val BlueSoft: Color get() = pick(0xFFEAF2FD, 0xFF0F1E2E)
    val BlueCard: Color get() = pick(0xFFF5F8FD, 0xFF111820)
    val BlueLight: Color get() = pick(0xFF8DBDF6, 0xFF2F6FA8)
    val BlueLine: Color get() = pick(0xFFD2E2F8, 0xFF1D3A57)
    val Glass: Color get() = pick(0xFFF5F8FC, 0xFF12171D)

    val OrangeDark: Color get() = pick(0xFFE05E22, 0xFFE5683A) // orange edge
    val OrangeText: Color get() = pick(0xFFC2501C, 0xFFFF9A6E)
    val OrangeSoft: Color get() = pick(0xFFFDEDE4, 0xFF2A1810)
    val OrangeCard: Color get() = pick(0xFFFEF7F2, 0xFF19130F)
    val OrangeLine: Color get() = pick(0xFFF3DCCD, 0xFF4A2A1C)
    val OrangeTrack: Color get() = pick(0xFFF6E3D8, 0xFF2E1D16)

    val GreenDark: Color get() = pick(0xFF23963F, 0xFF28B04A)
    val GreenText: Color get() = pick(0xFF1E7A35, 0xFF7EE09A)
    val GreenSoft: Color get() = pick(0xFFE4F6E9, 0xFF12251A)

    val YellowSoft: Color get() = pick(0xFFFDF4DF, 0xFF1E1A10)
    val YellowLine: Color get() = pick(0xFFF0DDB0, 0xFF3A3016)
    val YellowText: Color get() = pick(0xFF8A5D00, 0xFFF5B940)

    val Red: Color get() = pick(0xFFC93A30, 0xFFFF6B60)
    val RedSoft: Color get() = pick(0xFFFCECEA, 0xFF2A1414)
    val RedLine: Color get() = pick(0xFFF1CFCB, 0xFF4A2420)
    val RedEdge: Color get() = pick(0xFFB03228, 0xFFC9443A)
    val PinkSoft: Color get() = pick(0xFFFCE8F1, 0xFF2A1620)
    val PinkText: Color get() = pick(0xFFB8327A, 0xFFFF9BC8)

    // Steps: gold
    val Gold: Color get() = pick(0xFFE8A317, 0xFFF5B940)
    val GoldEdge: Color get() = pick(0xFFD49412, 0xFFD9A030)
    val GoldText: Color get() = pick(0xFF8A5D00, 0xFFF5B940)
    val GoldSoft: Color get() = pick(0xFFFDF4DF, 0xFF1E1A10)
    val GoldCard: Color get() = pick(0xFFFEFAF0, 0xFF17150F)
    val GoldLine: Color get() = pick(0xFFF0DFB3, 0xFF3A3016)
    val GoldTrack: Color get() = pick(0xFFF7EED6, 0xFF2A2416)
    val GoldIdle: Color get() = pick(0xFFF1E6C8, 0xFF3A3220)
    val GoldHit: Color get() = pick(0xFFF0B83A, 0xFFF5C452)

    val BlueStreakLine: Color get() = pick(0xFFD2E2F8, 0xFF1D3A57)

    /** Any light/dark colour pair. */
    fun pickColor(light: Long, dark: Long): Color = pick(light, dark)

    /** Page tint behind the rings view, one per tracker. */
    val TintSteps: Color get() = pick(0xFFFFFCF5, 0xFF0B0D10)
    val TintWater: Color get() = pick(0xFFF7FAFE, 0xFF0B0D10)
    val TintProtein: Color get() = pick(0xFFFFF9F5, 0xFF0B0D10)
    val RingTrackWater: Color get() = pick(0xFFEAF2FD, 0xFF14273A)
    val RingTrackProtein: Color get() = pick(0xFFFBE7DC, 0xFF2E1D16)
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
 * Inter, a clean grotesk close to Apple's San Francisco. It is bundled in res/font
 * (free under the SIL Open Font License, see assets/licenses).
 */
object Fonts {
    val family: FontFamily = FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_semibold, FontWeight.SemiBold),
        Font(R.font.inter_bold, FontWeight.Bold)
    )
    val display: FontFamily get() = family
    val body: FontFamily get() = family
    val bodyBold: FontFamily get() = family

    /** Kept so MainActivity's start-up call still works; nothing to load any more. */
    @Suppress("UNUSED_PARAMETER")
    fun init(context: Context) {}
}

object T {
    /** Big numbers and titles: bold with slightly tight letters. */
    fun display(size: Int, color: Color = C.Ink) = TextStyle(
        fontFamily = Fonts.family, fontWeight = FontWeight.Bold, fontSize = size.sp, color = color,
        letterSpacing = (size * -0.025f).sp, lineHeight = (size * 1.2f).sp,
        fontFeatureSettings = "tnum"
    )

    fun body(size: Int, color: Color = C.Ink) = TextStyle(
        fontFamily = Fonts.family, fontWeight = FontWeight.Medium, fontSize = size.sp, color = color,
        lineHeight = (size * 1.4f).sp
    )

    fun bold(size: Int, color: Color = C.Ink) = TextStyle(
        fontFamily = Fonts.family, fontWeight = FontWeight.SemiBold, fontSize = size.sp, color = color,
        lineHeight = (size * 1.3f).sp, fontFeatureSettings = "tnum"
    )

    /** Small uppercase section labels. */
    fun label(color: Color = C.Muted) = TextStyle(
        fontFamily = Fonts.family, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = color,
        letterSpacing = 1.sp
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
