package com.sipscoop.app.ui

import com.sipscoop.app.data.Walk
import com.sipscoop.app.data.StepLog
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
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

    val context = LocalContext.current
    val today = runCatching { LocalDate.parse(state.date) }.getOrElse { LocalDate.now() }
    // 0 = today, 1 = yesterday ... the day picker can look back 4 days.
    var back by remember { mutableIntStateOf(0) }
    val day = today.minusDays(back.toLong())
    val dayKey = day.toString()
    val daySteps = if (back == 0) steps else state.history[dayKey]?.steps ?: 0
    val hours = remember(dayKey, state.steps) { StepLog.hours(context, dayKey) }
    val walks = remember(dayKey, state.steps) { StepLog.walks(context, dayKey) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = if (compact) 0.dp else 6.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 14.dp)
    ) {
        Row(
            verticalAlignment = if (compact) Alignment.CenterVertically else Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.padding(top = if (compact) 0.dp else 14.dp)) {
                StrideMascot(Modifier.size(if (compact) 56.dp else 66.dp, if (compact) 43.dp else 51.dp), celebrate = fraction >= 1f)
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
                    .size(190.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showGoal = true }
            )
            Pill("Goal ${fmt(goal)} · edit", StepAccent) { showGoal = true }
        }

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

        // Distance, calories and time spent walking, for the picked day.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FactTile(Modifier.weight(1f), String.format(Locale.US, "%.1f km", stepsKm(daySteps)), "Distance")
            FactTile(Modifier.weight(1f), "${fmt(stepsKcal(daySteps))} kcal", "Burned, about")
            FactTile(Modifier.weight(1f), "${walks.sumOf { it.minutes }} min", "Active walking")
        }

        DayPicker(today, back) { back = it }

        ActivityChart(state, hours, back, day)

        WalksCard(walks, if (back == 0) "Walks today" else "Walks on ${shortDay(day)}")
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

private fun shortDay(d: LocalDate): String =
    d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.US) + " " + d.dayOfMonth

private fun clock(min: Int): String {
    val h = (min / 60) % 24
    val m = min % 60
    val h12 = if (h % 12 == 0) 12 else h % 12
    return String.format(Locale.US, "%d:%02d %s", h12, m, if (h < 12) "AM" else "PM")
}

private fun hourLabel(h: Int): String {
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "$h12 ${if (h % 24 < 12) "AM" else "PM"}"
}

@Composable
private fun FactTile(modifier: Modifier, value: String, label: String) {
    Column(
        modifier
            .background(C.GoldCard, RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = T.display(18, C.GoldText), maxLines = 1)
        Text(label, style = T.body(11, C.Muted), maxLines = 1)
    }
}

/** Today and the 4 days before it; pick one to see its hours and walks. */
@Composable
private fun DayPicker(today: LocalDate, back: Int, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Pick a day")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (4 downTo 0).forEach { b ->
                val d = today.minusDays(b.toLong())
                val on = b == back
                val bg by animateColorAsState(if (on) C.Gold else C.Surface, tween(220), label = "dayBg")
                val fg by animateColorAsState(if (on) Color(0xFF4A3600) else C.Ink, tween(220), label = "dayFg")
                Column(
                    Modifier
                        .weight(1f)
                        .background(bg, RoundedCornerShape(14.dp))
                        .border(2.dp, if (on) C.GoldEdge else C.Line, RoundedCornerShape(14.dp))
                        .clickable { onPick(b) }
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (b == 0) "Today" else d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.US),
                        style = T.bold(11, fg), maxLines = 1
                    )
                    Text("${d.dayOfMonth}", style = T.display(17, fg))
                }
            }
        }
    }
}

/** Steps by hour (when you walked) or the last 7 days. Tap an hour to see its number. */
@Composable
private fun ActivityChart(state: AppState, hours: IntArray, back: Int, day: LocalDate) {
    var byHour by remember { mutableStateOf(true) }
    // Show from 6 AM, or earlier if there were steps earlier.
    val first = minOf(6, hours.indexOfFirst { it > 0 }.takeIf { it >= 0 } ?: 6)
    val range = first..23
    val best = range.maxByOrNull { hours[it] } ?: 8
    var picked by remember(back) { mutableIntStateOf(-1) }
    val sel = if (picked in range) picked else best

    Column(
        Modifier
            .fillMaxWidth()
            .background(C.Surface, RoundedCornerShape(18.dp))
            .border(2.dp, C.Line, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (!byHour) "This week" else if (back == 0) "Today by hour" else "${shortDay(day)} by hour",
                style = T.bold(15), modifier = Modifier.weight(1f)
            )
            Row(Modifier.background(C.Faint, CircleShape).padding(3.dp)) {
                listOf(true to "By hour", false to "Week").forEach { (v, label) ->
                    val on = byHour == v
                    val bg by animateColorAsState(if (on) C.Gold else Color.Transparent, tween(220), label = "segBg")
                    Text(
                        label,
                        style = T.bold(12, if (on) Color(0xFF4A3600) else C.Muted),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(bg)
                            .clickable { byHour = v }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }
        if (byHour) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(C.GoldCard, RoundedCornerShape(12.dp))
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (hours[sel] == 0 && hours.sum() == 0) "No steps logged by hour yet"
                    else "${hourLabel(sel)} – ${hourLabel(sel + 1)} · ${fmt(hours[sel])} steps",
                    style = T.bold(13, C.GoldText)
                )
            }
            val top = maxOf(hours.maxOrNull() ?: 0, 1)
            var started by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { started = true }
            val grow by animateFloatAsState(if (started) 1f else 0f, tween(800, easing = FastOutSlowInEasing), label = "hourBars")
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                range.forEach { h ->
                    val v = hours[h]
                    val color = if (v >= StepLog.WALK_MIN_STEPS) C.Gold else C.GoldIdle
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { picked = h },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight((v.toFloat() / top).coerceIn(0.03f, 1f) * grow)
                                .background(color, RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                .then(if (h == sel) Modifier.border(2.dp, C.GoldText, RoundedCornerShape(5.dp)) else Modifier)
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(first, 9, 12, 15, 18, 21).distinct().filter { it in range }.forEach {
                    Text(hourLabel(it), style = T.bold(11, C.Muted))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Legend(C.Gold, "Hours with a walk (500+)")
                Legend(C.GoldIdle, "Light moving")
            }
        } else {
            WeekBars(state, Track.STEPS, "Last 7 days", Modifier.fillMaxWidth().height(170.dp))
        }
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
        Text(text, style = T.body(12, C.Muted))
    }
}

/** A timeline of the day's walks: 500+ steps in a row, with start and end time. */
@Composable
private fun WalksCard(walks: List<Walk>, title: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(C.Surface, RoundedCornerShape(18.dp))
            .border(2.dp, C.Line, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = T.bold(15), modifier = Modifier.weight(1f))
            Text("500+ steps in a row", style = T.body(12, C.Muted))
        }
        if (walks.isEmpty()) {
            Text(
                "No walks yet. Walk 500 steps in a row and it shows up here with the time.",
                style = T.body(13, C.Muted),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        walks.forEachIndexed { i, w ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.width(20.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        Modifier
                            .size(14.dp)
                            .background(C.Gold, CircleShape)
                            .border(3.dp, C.GoldSoft, CircleShape)
                    )
                    if (i < walks.lastIndex) {
                        Box(Modifier.width(3.dp).weight(1f).background(C.GoldLine, RoundedCornerShape(2.dp)))
                    }
                }
                Row(
                    Modifier
                        .weight(1f)
                        .padding(vertical = 5.dp)
                        .background(C.GoldCard, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("${clock(w.startMin)} – ${clock(w.endMin)}", style = T.bold(14))
                        Text("${walkName(w.startMin)} · ${w.minutes} min", style = T.body(12, C.Muted))
                    }
                    Text(fmt(w.steps), style = T.display(18, C.GoldText))
                }
            }
        }
        if (walks.isNotEmpty()) {
            Text(
                "${walks.size} ${if (walks.size == 1) "walk" else "walks"} · ${walks.sumOf { it.minutes }} min on your feet",
                style = T.body(12, C.Muted),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun walkName(startMin: Int): String = when (startMin / 60) {
    in 4..10 -> "Morning walk"
    in 11..13 -> "Lunch walk"
    in 14..16 -> "Afternoon walk"
    in 17..20 -> "Evening walk"
    else -> "Night walk"
}
