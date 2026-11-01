package com.sipscoop.app.ui

import androidx.activity.compose.BackHandler
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
fun AppRoot(
    state: AppState,
    update: ((AppState) -> AppState) -> Unit,
    onNeedNotificationPermission: () -> Unit
) {
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
            1 -> Box(Modifier.fillMaxSize().background(C.Bg).windowInsetsPadding(WindowInsets.safeDrawing)) {
                Onboarding(state) { name, age, avatar, water, protein ->
                    update {
                        it.copy(
                            onboarded = true, name = name, age = age, avatar = avatar,
                            waterGoal = water, proteinGoal = protein, since = it.date
                        )
                    }
                }
            }
            else -> MainScreen(state, update, onNeedNotificationPermission)
        }
    }
}

private const val HOME = 0
private const val PROFILE = 1
private const val SETTINGS = 2
private const val HISTORY = 3

@Composable
fun MainScreen(
    state: AppState,
    update: ((AppState) -> AppState) -> Unit,
    onNeedNotificationPermission: () -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var page by rememberSaveable { mutableIntStateOf(HOME) }

    BackHandler(enabled = page != HOME) {
        page = if (page == PROFILE) HOME else PROFILE
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(C.Bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(380)) { w -> if (forward) w / 3 else -w / 3 } + fadeIn(tween(380))) togetherWith
                    (slideOutHorizontally(tween(280)) { w -> if (forward) -w / 3 else w / 3 } + fadeOut(tween(200)))
            },
            label = "pages"
        ) { p ->
            when (p) {
                PROFILE -> ProfileScreen(
                    state, update,
                    onBack = { page = HOME },
                    onSettings = { page = SETTINGS },
                    onHistory = { page = HISTORY }
                )
                SETTINGS -> SettingsScreen(state, update, onBack = { page = PROFILE }, onNeedNotificationPermission = onNeedNotificationPermission)
                HISTORY -> HistoryScreen(state, onBack = { page = PROFILE })
                else -> Column(Modifier.fillMaxSize().background(C.Bg)) {
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
                            TrackerScreen(if (t == 0) Kind.WATER else Kind.PROTEIN, state, update, onProfile = { page = PROFILE })
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(2.dp).background(C.NavLine))
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
        }
    }
}

/** Both tabs always wear their own colour; the open one is filled in solid. */
@Composable
private fun NavTab(label: String, selected: Boolean, accent: Accent, water: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val face by animateColorAsState(if (selected) accent.face else accent.soft, tween(250), label = "tabFace")
    val edge by animateColorAsState(if (selected) accent.edge else accent.line, tween(250), label = "tabEdge")
    val textColor by animateColorAsState(if (selected) Color.White else accent.text, tween(250), label = "tabText")
    val iconColor by animateColorAsState(if (selected) Color.White else accent.face, tween(250), label = "tabIcon")
    Btn3D(
        onClick = onClick,
        face = face,
        edge = edge,
        modifier = modifier,
        depth = 5.dp,
        contentPadding = PaddingValues(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FillIcon(if (water) Ic.DROP else Ic.EGG, iconColor, 22.dp)
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
