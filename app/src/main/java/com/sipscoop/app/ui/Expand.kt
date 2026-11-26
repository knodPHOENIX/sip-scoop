package com.sipscoop.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sipscoop.app.data.AppState
import com.sipscoop.app.data.stepsKm
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** What a tracker page can be asked to open from the chip menu. */
enum class EditAction { GOAL, TODAY, AMOUNTS }

/** The chip menu under the rings asks a tracker page to open one of its edits. */
object TrackerEdits {
    val request = mutableStateOf<Pair<Kind, EditAction>?>(null)
}

/** Which chart is open in full, and where its card was (it grows from there). */
object ChartDetail {
    val open = mutableStateOf<Pair<Track, Rect>?>(null)
}

/** Makes a chart card open its detailed view when tapped. */
fun Modifier.opensChart(track: Track): Modifier = this.then(
    Modifier.composedChartTap(track)
)

private fun Modifier.composedChartTap(track: Track): Modifier = androidx.compose.ui.composed {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    this
        .onGloballyPositioned { bounds = it.boundsInRoot() }
        .clickable(onClickLabel = "Open detailed chart") { ChartDetail.open.value = track to bounds }
}

/**
 * Grows a card out of [origin] (a chip or a chart card, in root coordinates) into a bigger
 * card, and shrinks it back when closed. [fullScreen] fills the whole area; otherwise the card
 * is [height] tall, full width, starting at the origin's top.
 */
@Composable
private fun GrowOverlay(
    origin: Rect,
    fullScreen: Boolean,
    height: Dp,
    fromRadius: Dp,
    toRadius: Dp,
    tint: Color,
    onClosed: () -> Unit,
    content: @Composable (progress: Float, close: () -> Unit) -> Unit
) {
    val density = LocalDensity.current
    var host by remember { mutableStateOf<Rect?>(null) }
    val p = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }
    LaunchedEffect(closing) {
        if (!closing) p.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
        else {
            p.animateTo(0f, tween(240, easing = FastOutSlowInEasing))
            onClosed()
        }
    }
    val close = { closing = true }
    BackHandler { close() }

    Box(Modifier.fillMaxSize().onGloballyPositioned { host = it.boundsInRoot() }) {
        // Dim the page; tapping it closes.
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f * p.value))
                .pointerInput(Unit) { detectTapGestures { close() } }
        )
        val h = host ?: return@Box
        val start = origin.translate(-h.left, -h.top)
        val margin = with(density) { 16.dp.toPx() }
        val target = if (fullScreen) {
            Rect(0f, 0f, h.width, h.height)
        } else {
            val tall = with(density) { height.toPx() }
            var top = start.top - with(density) { 6.dp.toPx() }
            if (top + tall > h.height - margin) top = (h.height - margin - tall).coerceAtLeast(margin)
            Rect(margin, top, h.width - margin, top + tall)
        }
        val t = p.value
        val r = androidx.compose.ui.geometry.lerp(start, target, t)
        val radius = androidx.compose.ui.unit.lerp(fromRadius, toRadius, t)
        val shape = RoundedCornerShape(radius)
        Box(
            Modifier
                .offset { IntOffset(r.left.roundToInt(), r.top.roundToInt()) }
                .size(with(density) { r.width.toDp() }, with(density) { r.height.toDp() })
                .clip(shape)
                .background(Brush.verticalGradient(listOf(lerpColor(C.SheetTop, tint, 0.18f), C.SheetBottom)), shape)
                .border(1.dp, tint.copy(alpha = 0.55f), shape)
                .pointerInput(Unit) { detectTapGestures { } } // taps inside don't close it
        ) {
            // Lay the content out at its final width, so it doesn't squash while growing.
            Box(
                Modifier
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .requiredWidth(with(density) { target.width.toDp() })
            ) { content(t, close) }
        }
    }
}

private fun lerpColor(a: Color, b: Color, f: Float) = androidx.compose.ui.graphics.lerp(a, b, f)

// ---------------------------------------------------------------- chip menu

/** Holding the water or protein chip: it grows into that tracker's edit menu. */
@Composable
fun ChipEditMenu(state: AppState, index: Int, origin: Rect, onClosed: () -> Unit) {
    val kind = if (index == 1) Kind.WATER else Kind.PROTEIN
    val accent = if (index == 1) WaterAccent else ProteinAccent
    val color = if (index == 1) C.Blue else C.Orange
    val value = if (index == 1) "${fmt(state.waterTotal)} ml" else "${state.proteinTotal} g"
    val goal = if (index == 1) liters(state.waterGoal) else "${state.proteinGoal} g"
    val density = LocalDensity.current
    val chipHeight = with(density) { origin.height.toDp() }
    GrowOverlay(
        origin = origin, fullScreen = false, height = chipHeight + 196.dp,
        fromRadius = chipHeight / 2, toRadius = 24.dp, tint = color, onClosed = onClosed
    ) { t, close ->
        val rows = ((t - 0.45f) / 0.55f).coerceIn(0f, 1f)
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().height(chipHeight),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(7.dp).background(color, CircleShape))
                Text(value, style = T.bold(14, accent.text))
                LineIcon(Ic.PENCIL, accent.text, 12.dp, 2f)
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = rows; translationY = (1f - rows) * 12.dp.toPx() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                MenuRow("Daily goal", "Now $goal", accent) {
                    TrackerEdits.request.value = kind to EditAction.GOAL
                    close()
                }
                MenuRow("Edit today", "Change or remove today's entries", accent) {
                    TrackerEdits.request.value = kind to EditAction.TODAY
                    close()
                }
                MenuRow(
                    "Edit amounts",
                    if (index == 1) "Change the quick add buttons" else "Change the quick add amounts",
                    accent
                ) {
                    TrackerEdits.request.value = kind to EditAction.AMOUNTS
                    close()
                }
            }
        }
    }
}

@Composable
private fun MenuRow(title: String, sub: String, accent: Accent, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(32.dp).glass(RoundedCornerShape(10.dp), tint = accent.face),
            contentAlignment = Alignment.Center
        ) { LineIcon(Ic.PENCIL, accent.text, 14.dp, 2f) }
        Column(Modifier.weight(1f)) {
            Text(title, style = T.bold(15))
            Text(sub, style = T.body(12, C.Muted), maxLines = 1)
        }
        LineIcon(Ic.CHEVRON, C.GreyText, 14.dp, 2f)
    }
}

// ---------------------------------------------------------------- detailed chart

private enum class Range(val label: String, val days: Int) {
    WEEK("Week", 7), MONTH("Month", 30), QUARTER("3 months", 90), YEAR("Year", 365)
}

private data class DayPoint(val date: LocalDate, val value: Int, val goal: Int, val known: Boolean, val kcal: Int)

/** A chart card grows into a full-screen, interactive history for its tracker. */
@Composable
fun ChartDetailOverlay(state: AppState, track: Track, origin: Rect, onClosed: () -> Unit) {
    val color = when (track) { Track.STEPS -> C.Gold; Track.WATER -> C.Blue; Track.PROTEIN -> C.Orange }
    GrowOverlay(
        origin = origin, fullScreen = true, height = 0.dp,
        fromRadius = 18.dp, toRadius = 0.dp, tint = color, onClosed = onClosed
    ) { t, close ->
        val show = ((t - 0.35f) / 0.65f).coerceIn(0f, 1f)
        Box(Modifier.fillMaxWidth().graphicsLayer { alpha = show }) {
            ChartDetailContent(state, track, color, close)
        }
    }
}

@Composable
private fun ChartDetailContent(state: AppState, track: Track, color: Color, close: () -> Unit) {
    var range by remember { mutableStateOf(Range.MONTH) }
    val today = runCatching { LocalDate.parse(state.date) }.getOrElse { LocalDate.now() }
    val days = remember(range, state) {
        (range.days - 1 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            val r = state.history[d.toString()]
            when {
                d == today -> DayPoint(d, state.todayOf(track), state.goalOf(track), true, state.kcalTotal)
                r != null -> {
                    val v = when (track) { Track.STEPS -> r.steps; Track.WATER -> r.water; Track.PROTEIN -> r.protein }
                    val g = when (track) { Track.STEPS -> r.stepGoal; Track.WATER -> r.waterGoal; Track.PROTEIN -> r.proteinGoal }
                    DayPoint(d, v, if (g > 0) g else state.goalOf(track), true, r.kcal)
                }
                else -> DayPoint(d, 0, state.goalOf(track), false, 0)
            }
        }
    }
    var sel by remember(range) { mutableIntStateOf(days.lastIndex) }
    val title = when (track) { Track.STEPS -> "Steps"; Track.WATER -> "Water"; Track.PROTEIN -> "Protein" }
    val known = days.filter { it.known }
    val withData = known.filter { it.value > 0 }
    val total = known.sumOf { it.value.toLong() }
    val avg = if (withData.isNotEmpty()) (withData.sumOf { it.value.toLong() } / withData.size).toInt() else 0
    val best = withData.maxByOrNull { it.value }
    val hits = known.count { it.goal > 0 && it.value >= it.goal }
    val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
    val shortFmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = T.display(24), modifier = Modifier.weight(1f))
            SquishButton(onClick = close, background = C.Faint, contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
                LineIcon(Ic.CLOSE, C.Muted, 14.dp, 2.4f)
            }
        }
        // Range
        Row(Modifier.fillMaxWidth().glass(CircleShape).padding(3.dp)) {
            Range.entries.forEach { r ->
                val on = r == range
                Text(
                    r.label,
                    style = T.bold(13, if (on) Color.White else C.Muted),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (on) color else Color.Transparent, CircleShape)
                        .clickable { range = r }
                        .padding(vertical = 8.dp)
                )
            }
        }
        // Chart
        Column(
            Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row {
                Text("${days.first().date.format(shortFmt)} – ${days.last().date.format(shortFmt)}", style = T.body(12, C.Muted), modifier = Modifier.weight(1f))
                Text("Goal ${valueText(track, state.goalOf(track))} ┄", style = T.body(12, C.Muted))
            }
            val p = days[sel.coerceIn(0, days.lastIndex)]
            // The picked day, right above the bars.
            Row(
                Modifier.fillMaxWidth().glass(RoundedCornerShape(12.dp), tint = color).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(if (p.date == today) "Today" else p.date.format(dayFmt), style = T.body(12, C.Muted))
                    Text(if (p.known) valueText(track, p.value) else "No data", style = T.bold(16, if (p.known) color else C.Muted))
                }
                Text(dayNote(track, p), style = T.body(12, C.Muted), textAlign = TextAlign.End)
            }
            val goalLine = state.goalOf(track)
            val top = maxOf(days.maxOf { it.value }, goalLine, 1) * 1.08f
            val idle = C.Grey
            val dim = color.copy(alpha = 0.45f)
            val guide = C.Ink.copy(alpha = 0.35f)
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .pointerInput(days.size) {
                        detectTapGestures { pos -> sel = ((pos.x / size.width) * days.size).toInt().coerceIn(0, days.lastIndex) }
                    }
                    .pointerInput(days.size) {
                        detectHorizontalDragGestures { change, _ ->
                            sel = ((change.position.x / size.width) * days.size).toInt().coerceIn(0, days.lastIndex)
                        }
                    }
            ) {
                val n = days.size
                val slot = size.width / n
                val gap = if (n <= 31) slot * 0.25f else if (n <= 90) slot * 0.15f else 0f
                val w = (slot - gap).coerceAtLeast(1f)
                days.forEachIndexed { i, d ->
                    val hgt = (d.value / top) * size.height
                    val x = i * slot + gap / 2
                    val c = when {
                        i == sel -> color
                        !d.known || d.value == 0 -> idle
                        d.goal > 0 && d.value >= d.goal -> color.copy(alpha = 0.8f)
                        else -> dim
                    }
                    val barH = if (d.value > 0) hgt.coerceAtLeast(2f) else 3f
                    drawRoundRect(c, Offset(x, size.height - barH), Size(w, barH), CornerRadius(minOf(w / 2, 6.dp.toPx())))
                }
                val gy = size.height - (goalLine / top) * size.height
                drawLine(color.copy(alpha = 0.7f), Offset(0f, gy), Offset(size.width, gy), 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
                val sx = sel * slot + slot / 2
                drawLine(guide, Offset(sx, 0f), Offset(sx, size.height), 1.dp.toPx())
            }
            Row(Modifier.fillMaxWidth()) {
                Text(days.first().date.format(shortFmt), style = T.body(10, C.GreyText), modifier = Modifier.weight(1f))
                Text(days[days.size / 2].date.format(shortFmt), style = T.body(10, C.GreyText), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("Today", style = T.body(10, C.GreyText), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
            Text("Tap or slide along the bars to see each day", style = T.body(11, C.GreyText), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        // Summary
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat(valueText(track, avg), "Average a day", Modifier.weight(1f))
            Stat(best?.let { valueText(track, it.value) } ?: "–", best?.let { "Best day · ${it.date.format(shortFmt)}" } ?: "Best day", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("$hits of ${known.size}", "Days goal hit", Modifier.weight(1f))
            Stat(totalText(track, total), "Total, ${range.label.lowercase()}", Modifier.weight(1f))
        }
        // Every day
        SectionLabel("Day by day")
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(18.dp)).padding(horizontal = 12.dp, vertical = 2.dp)) {
            days.asReversed().forEachIndexed { i, d ->
                val ok = d.known && d.goal > 0 && d.value >= d.goal
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { sel = days.indexOf(d) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(if (d.date == today) "Today · ${d.date.format(dayFmt)}" else d.date.format(dayFmt), style = T.body(14))
                        if (track == Track.PROTEIN && d.kcal > 0) Text("${fmt(d.kcal)} kcal", style = T.body(11, C.Muted))
                    }
                    Text(if (d.known) valueText(track, d.value) else "–", style = T.bold(14, if (ok) color else C.Ink))
                    Spacer(Modifier.width(10.dp))
                    if (ok) LineIcon(Ic.CHECK, color, 14.dp, 2.4f) else Text("–", style = T.body(13, C.GreyText))
                }
                if (i < days.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(C.NavLine))
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier.glass(RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(value, style = T.bold(16), maxLines = 1)
        Text(label, style = T.body(11, C.Muted), maxLines = 1)
    }
}

private fun valueText(track: Track, v: Int): String = when (track) {
    Track.STEPS -> fmt(v)
    Track.WATER -> "${fmt(v)} ml"
    Track.PROTEIN -> "$v g"
}

private fun totalText(track: Track, v: Long): String = when (track) {
    Track.STEPS -> String.format(Locale.US, "%,d", v)
    Track.WATER -> if (v >= 10_000) String.format(Locale.US, "%.1f L", v / 1000.0) else "${fmt(v.toInt())} ml"
    Track.PROTEIN -> if (v >= 10_000) String.format(Locale.US, "%.1f kg", v / 1000.0) else String.format(Locale.US, "%,d g", v)
}

private fun dayNote(track: Track, p: DayPoint): String {
    if (!p.known) return ""
    val hit = if (p.goal > 0 && p.value >= p.goal) "Goal hit" else "Goal ${valueText(track, p.goal)}"
    return when (track) {
        Track.STEPS -> "${String.format(Locale.US, "%.1f", stepsKm(p.value))} km\n$hit"
        Track.PROTEIN -> if (p.kcal > 0) "${fmt(p.kcal)} kcal\n$hit" else hit
        Track.WATER -> hit
    }
}
