package com.sipscoop.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sipscoop.app.data.AppState
import com.sipscoop.app.data.STEP_GOAL_MAX
import com.sipscoop.app.data.stepsKcal
import com.sipscoop.app.data.stepsKm
import com.sipscoop.app.steps.StepStatus
import java.time.LocalDate
import java.util.Locale

/** The first page: today's steps, counted by the phone on its own. */
@Composable
fun StepsScreen(
    state: AppState,
    update: ((AppState) -> AppState) -> Unit,
    status: StepStatus,
    onAskPermission: () -> Unit,
    compact: Boolean = false
) {
    when (status) {
        StepStatus.NO_SENSOR -> StepsMessage(
            sleepy = true, compact = compact,
            title = "No step sensor here",
            body = "This phone doesn't have the chip that counts steps, so Stride can't count. Water and protein work as usual, just swipe on."
        ) { SwipeHint("Swipe for water", Arrangement.Center) }
        StepStatus.NEED_PERMISSION -> StepsMessage(
            sleepy = false, compact = compact,
            title = "Meet Stride, your step buddy",
            body = "Your phone already counts steps. Allow \"Physical activity\" and Stride will show them here. Nothing leaves your phone."
        ) {
            Btn3D(
                onClick = onAskPermission,
                face = C.Gold, edge = C.GoldEdge, depth = 6.dp,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 13.dp)
            ) { Text("Start counting", style = T.display(19, Color.White)) }
            Text("Android will ask you once.", style = T.bold(13, C.Muted))
        }
        StepStatus.ON -> StepsToday(state, update, compact)
    }
}

@Composable
private fun StepsMessage(sleepy: Boolean, compact: Boolean, title: String, body: String, actions: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp, vertical = if (compact) 8.dp else 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp, Alignment.CenterVertically)
    ) {
        if (compact) StrideMascot(Modifier.size(70.dp, 54.dp), sleepy = sleepy)
        else StrideMascot(Modifier.size(130.dp, 100.dp), sleepy = sleepy)
        Text(title, style = T.display(if (compact) 21 else 26), textAlign = TextAlign.Center)
        Text(body, style = T.body(if (compact) 14 else 15, C.Muted), textAlign = TextAlign.Center)
        Spacer(Modifier.size(4.dp))
        actions()
    }
}

@Composable
private fun StepsToday(state: AppState, update: ((AppState) -> AppState) -> Unit, compact: Boolean) {
    val goal = maxOf(1, state.stepGoal)
    val steps = state.steps
    val fraction = steps.toFloat() / goal
    val left = maxOf(0, goal - steps)
    var showGoal by remember { mutableStateOf(false) }

    val msg = when {
        steps == 0 -> "Let's get moving! Every step counts."
        fraction >= 1f -> "Step goal smashed! Every extra step still counts."
        left <= 300 -> "So close! A quick lap and you're there."
        else -> "${fmt(left)} steps to go. A ${(left + 99) / 100}-minute walk does it!"
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val ring: Dp = (maxHeight * 0.32f).coerceIn(150.dp, 210.dp)
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.padding(top = 14.dp)) {
                    StrideMascot(Modifier.size(66.dp, 51.dp), celebrate = fraction >= 1f)
                }
                AnimatedContent(
                    targetState = msg,
                    transitionSpec = {
                        (fadeIn(tween(300)) + slideInHorizontally(tween(300)) { -it / 10 }) togetherWith fadeOut(tween(150))
                    },
                    label = "stepMsg",
                    modifier = Modifier.weight(1f)
                ) { m -> SpeechBubble(m, C.GoldLine, Modifier.fillMaxWidth()) }
            }

            if (!compact) Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StepRing(
                    steps = steps, goal = goal, fraction = fraction,
                    modifier = Modifier
                        .size(ring)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showGoal = true }
                )
                Pill("Goal ${fmt(goal)} · edit", StepAccent) { showGoal = true }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StepFact(
                    Modifier.weight(1f),
                    icon = { LineIcon(Ic.PIN, C.GoldText, 22.dp, 2.2f) },
                    value = String.format(Locale.US, "%.1f km", stepsKm(steps)),
                    label = "Distance"
                )
                StepFact(
                    Modifier.weight(1f),
                    icon = { LineIcon(Ic.FLAME, Color(0xFFFF7A45), 22.dp, 2.2f) },
                    value = "${fmt(stepsKcal(steps))} kcal",
                    label = "Burned, about"
                )
            }

            WeekBars(state, Track.STEPS, "This week", Modifier.fillMaxWidth().weight(1f).heightIn(min = 96.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(Color(0xFF34C759), CircleShape))
                Spacer(Modifier.size(6.dp))
                Text("Counting on its own", style = T.bold(13, C.Muted))
                if (compact) {
                    Spacer(Modifier.weight(1f))
                    Pill("Goal ${fmt(goal)} · edit", StepAccent) { showGoal = true }
                } else {
                    SwipeHint("Swipe for water", Arrangement.End)
                }
            }
        }
    }

    if (showGoal) {
        AmountDialog(
            title = "Daily step goal",
            initial = goal, unit = "steps", accent = StepAccent, confirmLabel = "Save",
            step = 500,
            max = STEP_GOAL_MAX,
            maxNote = "50,000 steps is the most you can set.",
            warning = { v -> if (v in 1 until 3000) "That's very low. Most people aim for 6,000 to 10,000 steps a day." else null },
            onDismiss = { showGoal = false },
            onConfirm = { v ->
                update { it.copy(stepGoal = v.coerceIn(500, STEP_GOAL_MAX)) }
                showGoal = false
            }
        )
    }
}

/** The big gold ring with today's steps in the middle. It fills up when the page opens. */
@Composable
private fun StepRing(steps: Int, goal: Int, fraction: Float, modifier: Modifier) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val sweep by animateFloatAsState(
        if (started) fraction.coerceIn(0f, 1f) else 0f,
        tween(1100, easing = FastOutSlowInEasing), label = "ring"
    )
    val shown by animateIntAsState(steps, tween(700, easing = FastOutSlowInEasing), label = "steps")
    val track = C.GoldTrack
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.09f
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(width = stroke))
            if (sweep > 0f) {
                drawArc(C.Gold, -90f, 360f * sweep, false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(fmt(shown), style = T.display(44, C.GoldText))
            Text("of ${fmt(goal)} steps", style = T.bold(14, C.Muted))
        }
    }
}

@Composable
private fun StepFact(modifier: Modifier, icon: @Composable () -> Unit, value: String, label: String) {
    Row(
        modifier
            .background(C.GoldCard, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        icon()
        Column {
            Text(value, style = T.display(20, C.GoldText), maxLines = 1)
            Text(label, style = T.body(12, C.Muted), maxLines = 1)
        }
    }
}
