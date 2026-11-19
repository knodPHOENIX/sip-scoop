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
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
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
    val Bg: Color get() = pick(0xFFEEF0F3, 0xFF0E1014)
    val Surface: Color get() = pick(0xB3FFFFFF, 0x17FFFFFF)
    val Ink: Color get() = pick(0xFF111418, 0xFFF2F4F7)
    val Muted: Color get() = pick(0xFF6B7280, 0xFF8B93A1)
    val Line: Color get() = pick(0x1A0F1318, 0x1FFFFFFF)
    val NavLine: Color get() = pick(0x0F0F1318, 0x14FFFFFF)
    val Faint: Color get() = pick(0x0D0F1318, 0x0DFFFFFF)
    val Grey: Color get() = pick(0x140F1318, 0x1AFFFFFF)
    val GreyText: Color get() = pick(0xFF7A818C, 0xFF6B7380)

    val BlueEdge: Color get() = pick(0xFF2A73D6, 0xFF3D95EC)
    val BlueDark: Color get() = pick(0xFF1F6FD6, 0xFF6CB6FF) // blue text
    val BlueSoft: Color get() = pick(0x1A2F80ED, 0x244EA8FF)
    val BlueCard: Color get() = pick(0x0F2F80ED, 0x124EA8FF)
    val BlueLight: Color get() = pick(0xFF8DBDF6, 0xFF2F6FA8)
    val BlueLine: Color get() = pick(0x332F80ED, 0x404EA8FF)
    val Glass: Color get() = pick(0x99FFFFFF, 0x0FFFFFFF)

    val OrangeDark: Color get() = pick(0xFFE05E22, 0xFFE5683A) // orange edge
    val OrangeText: Color get() = pick(0xFFC2501C, 0xFFFF9A6E)
    val OrangeSoft: Color get() = pick(0x1AF26B2C, 0x24FF7A45)
    val OrangeCard: Color get() = pick(0x0FF26B2C, 0x12FF7A45)
    val OrangeLine: Color get() = pick(0x33F26B2C, 0x40FF7A45)
    val OrangeTrack: Color get() = pick(0x1FF26B2C, 0x29FF7A45)

    val GreenDark: Color get() = pick(0xFF23963F, 0xFF28B04A)
    val GreenText: Color get() = pick(0xFF1E7A35, 0xFF7EE09A)
    val GreenSoft: Color get() = pick(0x1A2BB04F, 0x2430D158)

    val YellowSoft: Color get() = pick(0x1FE8A317, 0x24F5B940)
    val YellowLine: Color get() = pick(0x40E8A317, 0x40F5B940)
    val YellowText: Color get() = pick(0xFF8A5D00, 0xFFF5B940)

    val Red: Color get() = pick(0xFFC93A30, 0xFFFF6B60)
    val RedSoft: Color get() = pick(0x1AC93A30, 0x24FF6B60)
    val RedLine: Color get() = pick(0x33C93A30, 0x40FF6B60)
    val RedEdge: Color get() = pick(0xFFB03228, 0xFFC9443A)
    val PinkSoft: Color get() = pick(0x1AB8327A, 0x24FF9BC8)
    val PinkText: Color get() = pick(0xFFB8327A, 0xFFFF9BC8)

    // Steps: gold
    val Gold: Color get() = pick(0xFFE8A317, 0xFFF5B940)
    val GoldEdge: Color get() = pick(0xFFD49412, 0xFFD9A030)
    val GoldText: Color get() = pick(0xFF8A5D00, 0xFFF5B940)
    val GoldSoft: Color get() = pick(0x1FE8A317, 0x24F5B940)
    val GoldCard: Color get() = pick(0x0FE8A317, 0x12F5B940)
    val GoldLine: Color get() = pick(0x40E8A317, 0x40F5B940)
    val GoldTrack: Color get() = pick(0x24E8A317, 0x29F5B940)
    val GoldIdle: Color get() = pick(0x2EE8A317, 0x33F5B940)
    val GoldHit: Color get() = pick(0xFFF0B83A, 0xFFF5C452)

    val BlueStreakLine: Color get() = pick(0x332F80ED, 0x404EA8FF)

    /** Opaque glass for pop-ups, sheets and toasts that sit on top of other content. */
    val Sheet: Color get() = pick(0xF2FFFFFF, 0xE61C1F25)

    /** The page background runs from a slightly lighter top to a deeper bottom. */
    val PageTop: Color get() = pick(0xFFF7F8FA, 0xFF1A1D23)
    val PageMid: Color get() = pick(0xFFEEF0F3, 0xFF0E1014)
    val PageBottom: Color get() = pick(0xFFE6E9EE, 0xFF08090B)

    /** Any light/dark colour pair. */
    fun pickColor(light: Long, dark: Long): Color = pick(light, dark)

    /** Page tint behind the rings view, one per tracker. */
    val TintSteps: Color get() = pick(0x00000000, 0x00000000)
    val TintWater: Color get() = pick(0x00000000, 0x00000000)
    val TintProtein: Color get() = pick(0x00000000, 0x00000000)
    val RingTrackWater: Color get() = pick(0x242F80ED, 0x294EA8FF)
    val RingTrackProtein: Color get() = pick(0x24F26B2C, 0x29FF7A45)
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

/** The calm neutral page background used behind every screen. */
fun Modifier.pageBackground(): Modifier = background(
    androidx.compose.ui.graphics.Brush.verticalGradient(0f to C.PageTop, 0.4f to C.PageMid, 1f to C.PageBottom)
)

@Composable
fun SipTheme(content: @Composable () -> Unit) {
    val scheme = if (ThemeState.dark) {
        darkColorScheme(
            primary = C.Blue, onPrimary = Color.White,
            secondary = C.Orange, onSecondary = Color.White,
            background = C.Bg, onBackground = C.Ink,
            surface = C.Sheet, onSurface = C.Ink,
            surfaceVariant = C.Faint, onSurfaceVariant = C.Muted,
            outline = C.Line
        )
    } else {
        lightColorScheme(
            primary = C.Blue, onPrimary = Color.White,
            secondary = C.Orange, onSecondary = Color.White,
            background = C.Bg, onBackground = C.Ink,
            surface = C.Sheet, onSurface = C.Ink,
            surfaceVariant = C.Faint, onSurfaceVariant = C.Muted,
            outline = C.Line
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
