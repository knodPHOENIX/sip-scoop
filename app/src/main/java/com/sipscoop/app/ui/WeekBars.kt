package com.sipscoop.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sipscoop.app.data.AppState
import java.time.LocalDate
import java.util.Locale

enum class Track { STEPS, WATER, PROTEIN }

/** Colours for one tracker's weekly bars. */
class BarColors(val today: Color, val hit: Color, val idle: Color, val todayText: Color)

fun barColors(track: Track): BarColors = when (track) {
    Track.STEPS -> BarColors(C.Gold, C.GoldHit, C.GoldIdle, C.GoldText)
    Track.WATER -> BarColors(C.Blue, C.pickColor(0xFF5BB8F5, 0xFF3E9BD8), C.pickColor(0xFFD6ECFB, 0xFF14304A), C.BlueDark)
    Track.PROTEIN -> BarColors(C.Orange, C.pickColor(0xFFFFA466, 0xFFE07A35), C.pickColor(0xFFF9E1CC, 0xFF3A2618), C.OrangeText)
}

/** Today's amount and goal for a tracker. */
fun AppState.todayOf(track: Track): Int = when (track) {
    Track.STEPS -> steps
    Track.WATER -> waterTotal
    Track.PROTEIN -> proteinTotal
}

fun AppState.goalOf(track: Track): Int = when (track) {
    Track.STEPS -> stepGoal
    Track.WATER -> waterGoal
    Track.PROTEIN -> proteinGoal
}

/** Last 7 days of one tracker, oldest first, with today last. */
@Composable
fun WeekBars(state: AppState, track: Track, title: String, modifier: Modifier) {
    val today = runCatching { LocalDate.parse(state.date) }.getOrElse { LocalDate.now() }
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val values = days.map { d ->
        if (d == today) state.todayOf(track) else state.history[d.toString()]?.let { r ->
            when (track) {
                Track.STEPS -> r.steps
                Track.WATER -> r.water
                Track.PROTEIN -> r.protein
            }
        } ?: 0
    }
    val goals = days.map { d ->
        val past = state.history[d.toString()]?.let { r ->
            when (track) {
                Track.STEPS -> r.stepGoal
                Track.WATER -> r.waterGoal
                Track.PROTEIN -> r.proteinGoal
            }
        } ?: 0
        if (d == today || past <= 0) state.goalOf(track) else past
    }
    val hits = values.indices.count { values[it] > 0 && values[it] >= goals[it] }
    val top = maxOf(values.maxOrNull() ?: 0, state.goalOf(track), 1)
    val colors = barColors(track)

    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val grow by animateFloatAsState(if (started) 1f else 0f, tween(900, easing = FastOutSlowInEasing), label = "bars")

    Column(
        modifier
            .background(C.Surface, RoundedCornerShape(18.dp))
            .border(2.dp, C.Line, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = T.bold(14), modifier = Modifier.weight(1f))
            Text("Goal hit $hits of 7 days", style = T.body(12, C.Muted))
        }
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEachIndexed { i, d ->
                val isToday = i == 6
                val hit = values[i] > 0 && values[i] >= goals[i]
                val color = when {
                    isToday -> colors.today
                    hit -> colors.hit
                    else -> colors.idle
                }
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                        val f = (values[i].toFloat() / top).coerceIn(0.04f, 1f) * grow
                        Box(
                            Modifier
                                .widthIn(max = 24.dp)
                                .fillMaxWidth()
                                .fillMaxHeight(f)
                                .background(color, RoundedCornerShape(7.dp))
                        )
                    }
                    Text(
                        d.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.US),
                        style = T.bold(11, if (isToday) colors.todayText else C.Muted)
                    )
                }
            }
        }
    }
}
