package com.sipscoop.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.sipscoop.app.data.AppState

@Composable
fun AppRoot(state: AppState, update: ((AppState) -> AppState) -> Unit) {
    var splashDone by rememberSaveable { mutableStateOf(false) }
    val screen = when {
        !splashDone -> 0
        !state.onboarded -> 1
        else -> 2
    }
    AnimatedContent(
        targetState = screen,
        transitionSpec = { fadeIn(tween(450)) togetherWith fadeOut(tween(300)) },
        label = "root"
    ) { s ->
        when (s) {
            0 -> Splash { splashDone = true }
            1 -> Box(Modifier.fillMaxSize().background(Color.White).windowInsetsPadding(WindowInsets.safeDrawing)) {
                Onboarding(state) { name, age, water, protein ->
                    update { it.copy(onboarded = true, name = name, age = age, waterGoal = water, proteinGoal = protein) }
                }
            }
            else -> MainScreen(state, update)
        }
    }
}

@Composable
fun MainScreen(state: AppState, update: ((AppState) -> AppState) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Box(Modifier.weight(1f)) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally(tween(380)) { w -> if (forward) w / 4 else -w / 4 } + fadeIn(tween(380))) togetherWith
                        (slideOutHorizontally(tween(280)) { w -> if (forward) -w / 4 else w / 4 } + fadeOut(tween(200)))
                },
                label = "tabs"
            ) { t ->
                TrackerScreen(if (t == 0) Kind.WATER else Kind.PROTEIN, state, update)
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFE6EDF3)))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NavTab("Water", tab == 0, WaterAccent, water = true, modifier = Modifier.weight(1f)) { tab = 0 }
            NavTab("Protein", tab == 1, ProteinAccent, water = false, modifier = Modifier.weight(1f)) { tab = 1 }
        }
    }
}

@Composable
private fun NavTab(label: String, selected: Boolean, accent: Accent, water: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) accent.soft else Color.White, tween(250), label = "tabBg")
    val border by animateColorAsState(if (selected) accent.face.copy(alpha = 0.5f) else Color.White, tween(250), label = "tabBorder")
    val textColor by animateColorAsState(if (selected) accent.text else C.Muted, tween(250), label = "tabText")
    val iconColor by animateColorAsState(if (selected) accent.face else Color(0xFFAFC0CE), tween(250), label = "tabIcon")
    SquishButton(
        onClick = onClick,
        background = bg,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        borderColor = border,
        contentPadding = PaddingValues(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Canvas(Modifier.size(22.dp)) {
                inViewBox(0f, 0f, 24f, 24f) {
                    val p = Path().apply {
                        if (water) {
                            moveTo(12f, 3f)
                            cubicTo(12f, 3f, 5f, 11f, 5f, 15f)
                            arcTo(Rect(5f, 8f, 19f, 22f), 180f, -180f, false)
                            cubicTo(19f, 11f, 12f, 3f, 12f, 3f)
                        } else {
                            moveTo(12f, 3f)
                            cubicTo(8.5f, 3f, 6f, 9f, 6f, 13.5f)
                            arcTo(Rect(6f, 7.5f, 18f, 19.5f), 180f, -180f, false)
                            cubicTo(18f, 9f, 15.5f, 3f, 12f, 3f)
                        }
                        close()
                    }
                    drawPath(p, iconColor)
                }
            }
            Text(label, style = T.bold(15, textColor))
        }
    }
}

/** The blue loading screen with bouncing mascots and a filling bar. */
@Composable
fun Splash(onDone: () -> Unit) {
    val load = remember { Animatable(0.06f) }
    LaunchedEffect(Unit) {
        load.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
        onDone()
    }
    val inf = rememberInfiniteTransition(label = "splash")
    val bob by inf.animateFloat(0f, -14f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    Box(
        Modifier
            .fillMaxSize()
            .background(C.Blue)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(Color(0xFF5CBDF8), 14.dp.toPx(), Offset(w * 0.15f, h * 0.17f))
            drawCircle(Color(0xFF5CBDF8), 7.dp.toPx(), Offset(w * 0.25f, h * 0.23f))
            drawCircle(Color(0xFF5CBDF8), 18.dp.toPx(), Offset(w * 0.85f, h * 0.3f))
            drawCircle(Color(0xFF5CBDF8), 8.dp.toPx(), Offset(w * 0.77f, h * 0.14f))
            drawCircle(Color(0xFF5CBDF8), 10.dp.toPx(), Offset(w * 0.11f, h * 0.66f))
            val top = h * 0.82f
            val waveH = 13.dp.toPx()
            val wave = Path().apply {
                moveTo(0f, top)
                cubicTo(w * 0.18f, top - waveH, w * 0.33f, top + waveH, w * 0.5f, top)
                cubicTo(w * 0.67f, top - waveH, w * 0.82f, top + waveH, w, top)
                lineTo(w, h); lineTo(0f, h); close()
            }
            drawPath(wave, C.BlueDeep)
        }
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 40.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))
            MascotPair(
                Modifier
                    .size(210.dp)
                    .graphicsLayer { translationY = bob * density }
            )
            Spacer(Modifier.height(18.dp))
            Text("Sip & Scoop", style = T.display(46, Color.White))
            Text("Hydrate. Fuel up. Repeat.", style = T.bold(18, Color(0xFFE6F5FE)))
            Spacer(Modifier.weight(1.2f))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F6CAD))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(load.value)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text("Filling your glass…", style = T.bold(15, Color.White))
        }
    }
}
