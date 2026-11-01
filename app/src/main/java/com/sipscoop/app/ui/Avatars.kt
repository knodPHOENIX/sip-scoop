package com.sipscoop.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.PathParser

private class AvatarLook(
    val bg: Long, val skin: Long, val hair: Long, val shirt: Long, val shirtDark: Long,
    val cap: Long = 0, val capDark: Long = 0
)

private val LOOKS = mapOf(
    "m1" to AvatarLook(0xFFBFE6FD, 0xFFF1C08F, 0xFF2B2A33, 0xFF1E9BF0, 0xFF1477BE),
    "m2" to AvatarLook(0xFFC9F0D6, 0xFFB97A4E, 0xFF2E1C14, 0xFF58C443, 0xFF3E8F2C),
    "m3" to AvatarLook(0xFFFFE0C7, 0xFFE3A877, 0xFF3A2418, 0xFF2E4A63, 0xFF1C3247, 0xFFFF8A3D, 0xFFD9681F),
    "f1" to AvatarLook(0xFFFFD6E5, 0xFFF1C08F, 0xFF2B2233, 0xFFE85D9A, 0xFFB83C74),
    "f2" to AvatarLook(0xFFE1DAFF, 0xFFB97A4E, 0xFF5A3320, 0xFF7B6CF0, 0xFF5646C9),
    "f3" to AvatarLook(0xFFFFF0B8, 0xFFE8B48A, 0xFF8A4B2A, 0xFF1E9BF0, 0xFF1477BE)
)

val AVATAR_LABELS = mapOf(
    "m1" to "Spiky-hair boy",
    "m2" to "Curly-hair boy with glasses",
    "m3" to "Boy in a cap",
    "f1" to "Girl with long hair",
    "f2" to "Girl with a bun",
    "f3" to "Girl with a bob"
)

private val pathCache = HashMap<String, Path>()

/** Turns SVG path text into a Compose path (cached, the shapes never change). */
internal fun svg(d: String): Path = pathCache.getOrPut(d) { PathParser().parsePathString(d).toPath() }

private const val SHIRT = "M14 122 C18 98 38 87 60 87 C82 87 102 98 106 122 Z"
private const val COLLAR = "M48 87 Q60 97 72 87"
private const val SMILE = "M53 67 Q60 74 67 67"
private const val HAIR_M1 = "M35 55 C33 38 44 28 58 28 L52 22 L62 26 L66 20 L70 28 C80 31 87 40 85 55 C82 47 76 43 68 44 C62 40 52 41 46 45 C41 47 37 50 35 55 Z"
private const val SIDEBURNS_M3 = "M36 58 C36 52 38 49 40 48 L40 60 Z M84 58 C84 52 82 49 80 48 L80 60 Z"
private const val CAP_M3 = "M35 49 C35 33 46 26 60 26 C74 26 85 33 85 49 Z"
private const val BRIM_M3 = "M60 49 L97 49 C96 55 76 56 60 53 Z"
private const val STUBBLE_M3 = "M50 75 Q60 81 70 75"
private const val BACK_F1 = "M34 58 C30 34 46 26 60 26 C76 26 90 36 86 60 L88 98 C80 102 70 100 66 92 L54 92 C50 100 38 102 32 98 Z"
private const val FRONT_F1 = "M36 53 C37 37 49 30 61 31 C74 32 84 40 84 53 C77 45 67 40 57 44 C50 46 42 48 36 53 Z"
private const val FRONT_F2 = "M36 54 C36 38 46 30 60 30 C74 30 84 38 84 54 C78 44 68 40 60 41 C52 40 42 44 36 54 Z"
private const val BACK_F3 = "M32 66 C28 40 43 27 60 27 C77 27 92 40 88 66 C86 74 78 76 76 70 L44 70 C42 76 34 74 32 66 Z"
private const val FRONT_F3 = "M36 52 C38 38 48 31 60 31 C72 31 82 38 84 52 C76 47 70 41 62 40 C56 45 46 48 36 52 Z"
private const val LASHES = "M46.5 55 L44.5 53.5 M73.5 55 L75.5 53.5"

/** One of the six profile characters, drawn as a circle. */
@Composable
fun AvatarImage(id: String, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        inViewBox(0f, 0f, 120f, 120f) { drawAvatar(id) }
    }
}

fun DrawScope.drawAvatar(id: String) {
    val look = LOOKS[id] ?: LOOKS.getValue("m1")
    val skin = Color(look.skin)
    val hair = Color(look.hair)
    val ink = Color(0xFF17324D)
    val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(0f, 0f, 120f, 120f)) }
    clipPath(circle) {
        drawRect(Color(look.bg), size = Size(120f, 120f))
        if (id == "f1") drawPath(svg(BACK_F1), hair)
        if (id == "f3") drawPath(svg(BACK_F3), hair)
        drawPath(svg(SHIRT), Color(look.shirt))
        drawPath(svg(COLLAR), Color(look.shirtDark), style = Stroke(width = 3f, cap = StrokeCap.Round))
        drawRoundRect(skin, topLeft = Offset(52f, 72f), size = Size(16f, 17f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f))
        drawCircle(skin, 5.5f, Offset(36f, 59f))
        drawCircle(skin, 5.5f, Offset(84f, 59f))
        drawCircle(skin, 24f, Offset(60f, 56f))

        when (id) {
            "m1" -> drawPath(svg(HAIR_M1), hair)
            "m2" -> {
                drawCircle(hair, 8f, Offset(41f, 46f))
                drawCircle(hair, 9.5f, Offset(48f, 37f))
                drawCircle(hair, 10f, Offset(60f, 33f))
                drawCircle(hair, 9.5f, Offset(72f, 37f))
                drawCircle(hair, 8f, Offset(79f, 46f))
            }
            "m3" -> {
                drawPath(svg(SIDEBURNS_M3), hair)
                drawPath(svg(CAP_M3), Color(look.cap))
                drawPath(svg(BRIM_M3), Color(look.capDark))
                drawCircle(Color(look.capDark), 3f, Offset(60f, 27f))
                drawPath(svg(STUBBLE_M3), hair.copy(alpha = 0.5f), style = Stroke(width = 3f, cap = StrokeCap.Round))
            }
            "f1" -> drawPath(svg(FRONT_F1), hair)
            "f2" -> {
                drawCircle(hair, 11f, Offset(60f, 25f))
                drawPath(svg(FRONT_F2), hair)
                drawCircle(Color(0xFFFFC800), 2.6f, Offset(36f, 67f))
                drawCircle(Color(0xFFFFC800), 2.6f, Offset(84f, 67f))
            }
            "f3" -> {
                drawPath(svg(FRONT_F3), hair)
                rotate(25f, Offset(74f, 40f)) {
                    drawRoundRect(
                        Color(0xFFFFC800), topLeft = Offset(68f, 37f), size = Size(12f, 6f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
                    )
                }
            }
        }

        drawCircle(ink, 3.3f, Offset(51f, 58f))
        drawCircle(ink, 3.3f, Offset(69f, 58f))
        drawCircle(Color.White, 1f, Offset(52.2f, 56.8f))
        drawCircle(Color.White, 1f, Offset(70.2f, 56.8f))
        if (id.startsWith("f")) drawPath(svg(LASHES), ink, style = Stroke(width = 1.8f, cap = StrokeCap.Round))
        drawOval(Color(0xCCFF9DB0), topLeft = Offset(41.3f, 62.4f), size = Size(8.4f, 5.2f))
        drawOval(Color(0xCCFF9DB0), topLeft = Offset(70.3f, 62.4f), size = Size(8.4f, 5.2f))
        drawPath(svg(SMILE), ink, style = Stroke(width = 2.6f, cap = StrokeCap.Round))
        if (id == "m2") {
            val glass = Color(0xFF22303E)
            drawCircle(glass, 7.5f, Offset(51f, 58f), style = Stroke(width = 2.4f))
            drawCircle(glass, 7.5f, Offset(69f, 58f), style = Stroke(width = 2.4f))
            drawLine(glass, Offset(58.5f, 58f), Offset(61.5f, 58f), strokeWidth = 2.4f, cap = StrokeCap.Round)
        }
    }
}
