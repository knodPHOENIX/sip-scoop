package com.sipscoop.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sipscoop.app.data.AppState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Calendar behind a streak pill. [steps] = the gold step-streak calendar,
 * otherwise the blue diet-streak calendar (water and protein both hit).
 */
@Composable
fun StreakCalendarDialog(state: AppState, steps: Boolean, onDismiss: () -> Unit) {
    val today = runCatching { LocalDate.parse(state.date) }.getOrElse { LocalDate.now() }
    var offset by remember { mutableIntStateOf(0) }
    val month = YearMonth.from(today).plusMonths(offset.toLong())

    fun hit(d: LocalDate): Boolean = when {
        d.isAfter(today) -> false
        d == today -> if (steps) state.stepsMet else state.todayMet
        else -> state.history[d.toString()]?.let { if (steps) it.stepMet else it.met } ?: false
    }

    val best = remember(state.history, state.steps, state.water, state.protein, steps) {
        val dates = state.history.keys.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() } + today
        var longest = 0
        var run = 0
        var prev: LocalDate? = null
        for (d in dates.distinct().sorted()) {
            run = if (hit(d)) (if (prev != null && prev.plusDays(1) == d && run > 0) run + 1 else 1) else 0
            longest = maxOf(longest, run)
            prev = d
        }
        longest
    }
    val current = if (steps) state.stepStreak else state.streak

    val color = if (steps) C.Gold else C.Blue
    val onColor = if (steps) Color(0xFF4A3600) else Color.White
    val soft = if (steps) C.GoldSoft else C.BlueSoft
    val text = if (steps) C.GoldText else C.BlueDark
    val missBg = C.pickColor(0xFFEDF1F5, 0xFF22313F)
    val futureBg = C.pickColor(0xFFF6F8FA, 0xFF1A2733)
    val futureText = C.pickColor(0xFFB7C3CE, 0xFF4C5F70)

    Popup(onDismiss = onDismiss, centered = false) {
        Row(
            Modifier.padding(end = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(48.dp).background(soft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                if (steps) ShoeIcon(30.dp) else FillIcon(Ic.BOLT, C.Blue, 28.dp)
            }
            Column {
                Text(
                    "$current-day ${if (steps) "step" else "diet"} streak",
                    style = T.display(22)
                )
                Text(
                    if (steps) "Days you hit your step goal" else "Days you hit both water and protein",
                    style = T.body(13, C.Muted)
                )
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MonthArrow(back = true, enabled = offset > -24) { offset-- }
            Text(
                "${month.month.getDisplayName(TextStyle.FULL, Locale.US)} ${month.year}",
                style = T.bold(16),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            MonthArrow(back = false, enabled = offset < 12) { offset++ }
        }

        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, style = T.bold(11, C.Muted), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }

        var drag by remember { mutableIntStateOf(0) }
        AnimatedContent(
            targetState = month,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(260)) { w -> if (forward) w / 6 else -w / 6 } + fadeIn(tween(260))) togetherWith fadeOut(tween(120))
            },
            label = "month",
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { drag = 0 },
                        onDragEnd = {
                            if (drag < -60 && offset < 12) offset++
                            else if (drag > 60 && offset > -24) offset--
                            drag = 0
                        }
                    ) { _, dx -> drag += dx.toInt() }
                }
        ) { m ->
            val lead = m.atDay(1).dayOfWeek.value - 1
            val cells: List<LocalDate?> = List(lead) { null } + (1..m.lengthOfMonth()).map { m.atDay(it) }
            Column(Modifier.height(6 * 40.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { d ->
                            Box(Modifier.weight(1f).height(34.dp), contentAlignment = Alignment.Center) {
                                if (d != null) {
                                    val h = hit(d)
                                    val future = d.isAfter(today)
                                    val isToday = d == today
                                    var box = Modifier
                                        .size(36.dp, 34.dp)
                                        .background(
                                            when {
                                                h -> color
                                                future -> futureBg
                                                else -> missBg
                                            },
                                            RoundedCornerShape(10.dp)
                                        )
                                    if (isToday) box = box.border(2.5.dp, if (h) C.Ink else color, RoundedCornerShape(10.dp))
                                    Box(box, contentAlignment = Alignment.Center) {
                                        Text(
                                            "${d.dayOfMonth}",
                                            style = T.bold(13, if (h) onColor else if (future) futureText else C.Muted)
                                        )
                                    }
                                }
                            }
                        }
                        repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        val monthHits = (1..month.lengthOfMonth()).count { hit(month.atDay(it)) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalTile(Modifier.weight(1f), soft, "$monthHits", text, "Days hit this month")
            CalTile(Modifier.weight(1f), soft, "$best ${if (best == 1) "day" else "days"}", text, "Best streak")
        }
    }
}

@Composable
private fun MonthArrow(back: Boolean, enabled: Boolean, onClick: () -> Unit) {
    SquishButton(
        onClick = { if (enabled) onClick() },
        background = C.Surface,
        borderColor = C.Line,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        LineIcon(if (back) Ic.BACK else Ic.CHEVRON, if (enabled) C.Muted else C.Line, 16.dp, 3f)
    }
}

@Composable
private fun CalTile(modifier: Modifier, bg: Color, value: String, valueColor: Color, label: String) {
    Column(
        modifier
            .background(bg, RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = T.display(19, valueColor), maxLines = 1)
        Text(label, style = T.body(12, C.Muted), maxLines = 1)
    }
}
