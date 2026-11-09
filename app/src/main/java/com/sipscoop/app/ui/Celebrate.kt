package com.sipscoop.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sipscoop.app.data.AppState
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val CONFETTI_COLORS = listOf(
    Color(0xFF1E9BF0), Color(0xFFFF8A3D), Color(0xFFFFC800),
    Color(0xFF58C443), Color(0xFFE85D9A), Color(0xFF8ED3FA)
)

private data class Piece(val x: Float, val delay: Float, val color: Color, val w: Float, val h: Float, val spin: Float, val drift: Float)

/** Falling paper confetti. Draws nothing on touches, so the screen below still works. */
@Composable
fun Confetti(modifier: Modifier = Modifier, count: Int = 26) {
    val pieces = remember {
        val r = Random(7)
        List(count) {
            Piece(
                x = r.nextFloat(), delay = r.nextFloat(),
                color = CONFETTI_COLORS[it % CONFETTI_COLORS.size],
                w = 8f + r.nextFloat() * 6f, h = 12f + r.nextFloat() * 8f,
                spin = 180f + r.nextFloat() * 540f, drift = (r.nextFloat() - 0.5f) * 0.12f
            )
        }
    }
    val inf = rememberInfiniteTransition(label = "confetti")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "fall")
    Canvas(modifier) {
        pieces.forEach { p ->
            val k = (t + p.delay) % 1f
            val x = (p.x + p.drift * sin(k * 2f * PI.toFloat() * 2f)) * size.width
            val y = -40f + k * (size.height + 80f)
            val sx = p.w * density / 2f
            val sy = p.h * density / 2f
            rotate(p.spin * k, Offset(x, y)) {
                drawRoundRect(
                    p.color,
                    topLeft = Offset(x - sx / 2f, y - sy / 2f),
                    size = Size(sx, sy),
                    cornerRadius = CornerRadius(3f * density / 2f),
                    alpha = if (k > 0.9f) (1f - k) / 0.1f else 1f
                )
            }
        }
    }
}

/** Little cracker bursts: rings of dots that spread out and fade. */
@Composable
fun Fireworks(modifier: Modifier = Modifier) {
    val bursts = listOf(
        Triple(0.2f, 0.18f, Color(0xFFFFC800)),
        Triple(0.82f, 0.28f, Color(0xFFFF8A3D)),
        Triple(0.3f, 0.78f, Color(0xFF58C443)),
        Triple(0.75f, 0.7f, Color(0xFF1E9BF0))
    )
    val inf = rememberInfiniteTransition(label = "fireworks")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "burst")
    Canvas(modifier) {
        bursts.forEachIndexed { i, (fx, fy, color) ->
            val k = (t + i * 0.27f) % 1f
            val c = Offset(fx * size.width, fy * size.height)
            val r = 18f * density + k * 46f * density
            for (d in 0 until 12) {
                val a = d * (2.0 * PI / 12).toFloat()
                drawCircle(
                    color, radius = 3.2f * density * (1f - k * 0.5f),
                    center = Offset(c.x + cos(a) * r, c.y + sin(a) * r),
                    alpha = (1f - k).coerceIn(0f, 1f)
                )
            }
        }
    }
}

/**
 * Watches today's totals. Finishing one goal drops a small banner with confetti;
 * finishing both shows a full celebration.
 */
@Composable
fun CelebrationLayer(state: AppState) {
    val waterDone = state.waterTotal >= state.waterGoal
    val proteinDone = state.proteinTotal >= state.proteinGoal
    val stepsDone = state.stepsMet
    var prevSteps by remember { mutableStateOf<Boolean?>(null) }
    var prevWater by remember { mutableStateOf<Boolean?>(null) }
    var prevProtein by remember { mutableStateOf<Boolean?>(null) }
    var banner by remember { mutableStateOf<String?>(null) }
    var bannerKey by remember { mutableIntStateOf(0) }
    var showBoth by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(waterDone, proteinDone) {
        val justWater = prevWater == false && waterDone
        val justProtein = prevProtein == false && proteinDone
        if (justWater || justProtein) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (waterDone && proteinDone) {
                banner = null
                showBoth = true
            } else {
                banner = if (justWater) "water" else "protein"
                bannerKey++
            }
        }
        prevWater = waterDone
        prevProtein = proteinDone
    }
    LaunchedEffect(stepsDone) {
        if (prevSteps == false && stepsDone) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            banner = "steps"
            bannerKey++
        }
        prevSteps = stepsDone
    }
    LaunchedEffect(bannerKey) {
        if (banner != null) {
            delay(3400)
            banner = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = banner != null, enter = fadeIn(tween(250)), exit = fadeOut(tween(400))) {
            Confetti(Modifier.fillMaxSize(), count = 16)
        }
        AnimatedVisibility(
            visible = banner != null,
            enter = slideInVertically(spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)) { -it * 2 } + fadeIn(),
            exit = slideOutVertically(tween(300)) { -it * 2 } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 16.dp, end = 16.dp, top = 96.dp)
        ) {
            val kind = banner ?: "water"
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(C.Surface, RoundedCornerShape(20.dp))
                    .border(2.dp, C.Green.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (kind) {
                    "steps" -> StrideMascot(Modifier.size(62.dp, 48.dp), celebrate = true)
                    "protein" -> EggMascot(Modifier.size(46.dp, 58.dp), celebrate = true)
                    else -> DripMascot(Modifier.size(42.dp, 60.dp), celebrate = true)
                }
                Column {
                    Text(
                        when (kind) {
                            "steps" -> "Step goal smashed!"
                            "protein" -> "Protein goal done!"
                            else -> "Water goal done!"
                        },
                        style = T.display(20)
                    )
                    Text(
                        when (kind) {
                            "steps" -> "${fmt(state.stepGoal)} steps today. Step streak: ${state.stepStreak} ${if (state.stepStreak == 1) "day" else "days"}."
                            "protein" -> "${state.proteinGoal} g in. Now finish your water."
                            else -> "${liters(state.waterGoal)} down. Now finish your protein."
                        },
                        style = T.body(14, C.Muted)
                    )
                }
            }
        }

        AnimatedVisibility(visible = showBoth, enter = fadeIn(tween(250)), exit = fadeOut(tween(250))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (ThemeState.dark) 0.6f else 0.45f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            ) {
                Fireworks(Modifier.fillMaxSize())
                Confetti(Modifier.fillMaxSize(), count = 32)
            }
        }
        AnimatedVisibility(
            visible = showBoth,
            enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.6f) + fadeIn(),
            exit = scaleOut(tween(200), targetScale = 0.8f) + fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 22.dp)
        ) {
            val streak = state.streak
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(C.Surface, RoundedCornerShape(28.dp))
                    .padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MascotPair(Modifier.size(170.dp, 150.dp), dripColor = C.Blue, celebrate = true)
                Text("Both goals done!", style = T.display(30), textAlign = TextAlign.Center)
                Text(
                    "${liters(state.waterGoal)} of water and ${state.proteinGoal} g of protein. " +
                        "Your diet streak is now $streak ${if (streak == 1) "day" else "days"}, ${state.firstName}.",
                    style = T.body(15, C.Muted),
                    textAlign = TextAlign.Center
                )
                Row(
                    Modifier
                        .background(C.BlueSoft, CircleShape)
                        .border(2.dp, C.BlueStreakLine, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FillIcon(Ic.BOLT, C.Blue, 18.dp)
                    Text("$streak-day diet streak", style = T.bold(15, C.BlueDark))
                }
                Btn3D(
                    onClick = { showBoth = false },
                    face = C.Green, edge = C.GreenDark, depth = 6.dp,
                    contentPadding = PaddingValues(vertical = 13.dp)
                ) { Text("Yay!", style = T.display(20, Color.White)) }
            }
        }
    }
}
