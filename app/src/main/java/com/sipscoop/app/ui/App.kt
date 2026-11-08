package com.sipscoop.app.ui

import kotlinx.coroutines.delay
import androidx.compose.foundation.border
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

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
private const val FOOD = 4

@Composable
fun MainScreen(
    state: AppState,
    update: ((AppState) -> AppState) -> Unit,
    onNeedNotificationPermission: () -> Unit
) {
    var page by rememberSaveable { mutableIntStateOf(HOME) }
    val pager = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    var toast by remember { mutableStateOf<String?>(null) }
    var toastKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(toastKey) {
        if (toast != null) {
            delay(3000)
            toast = null
        }
    }

    BackHandler(enabled = page != HOME) {
        page = if (page == PROFILE || page == FOOD) HOME else PROFILE
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
                FOOD -> FoodSearchScreen(
                    state, update,
                    onBack = { page = HOME },
                    onAdded = { msg ->
                        toast = msg
                        toastKey++
                    }
                )
                else -> Column(Modifier.fillMaxSize().background(C.Bg)) {
                    // Fixed top: greeting, streak and profile stay put while the pages slide.
                    Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp)) {
                        GreetingHeader(state.firstName, state.streak, state.avatar) { page = PROFILE }
                    }
                    PageDots(pager.currentPage + pager.currentPageOffsetFraction)
                    Box(Modifier.weight(1f)) {
                        HorizontalPager(state = pager, beyondViewportPageCount = 1) { t ->
                            TrackerScreen(if (t == 0) Kind.WATER else Kind.PROTEIN, state, update, onSearchFood = { page = FOOD })
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(2.dp).background(C.NavLine))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NavTab("Water", pager.currentPage == 0, WaterAccent, water = true, modifier = Modifier.weight(1f)) {
                            scope.launch { pager.animateScrollToPage(0) }
                        }
                        NavTab("Protein", pager.currentPage == 1, ProteinAccent, water = false, modifier = Modifier.weight(1f)) {
                            scope.launch { pager.animateScrollToPage(1) }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = toast != null && page == HOME,
            enter = slideInVertically(spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)) { -it * 2 } + fadeIn(),
            exit = slideOutVertically(tween(260)) { -it * 2 } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 16.dp, end = 16.dp, top = 96.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(C.Surface, RoundedCornerShape(16.dp))
                    .border(2.dp, C.Green.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.size(28.dp).background(C.Green, CircleShape), contentAlignment = Alignment.Center) {
                    LineIcon(Ic.CHECK, Color.White, 14.dp, 4f)
                }
                Text(toast ?: "", style = T.bold(15), modifier = Modifier.weight(1f))
            }
        }
        CelebrationLayer(state)
    }
}

/** Two little pills under the header; the wide one follows your finger while swiping. */
@Composable
private fun PageDots(position: Float) {
    val p = position.coerceIn(0f, 1f)
    Row(
        Modifier.fillMaxWidth().padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        Box(Modifier.size(width = (6 + 12 * (1 - p)).dp, height = 6.dp).background(lerpColor(C.Line, C.Blue, 1 - p), CircleShape))
        Box(Modifier.size(width = (6 + 12 * p).dp, height = 6.dp).background(lerpColor(C.Line, C.Orange, p), CircleShape))
    }
}

private fun lerpColor(a: Color, b: Color, t: Float): Color = androidx.compose.ui.graphics.lerp(a, b, t.coerceIn(0f, 1f))

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
