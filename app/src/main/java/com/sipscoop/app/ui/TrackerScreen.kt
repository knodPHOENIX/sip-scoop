package com.sipscoop.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sipscoop.app.data.AppState
import com.sipscoop.app.data.Entry
import com.sipscoop.app.data.addProtein
import com.sipscoop.app.data.addWater
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

enum class Kind { WATER, PROTEIN }

fun fmt(n: Int): String = String.format(Locale.US, "%,d", n)

fun liters(ml: Int): String {
    val s = if (ml % 1000 == 0) "${ml / 1000}" else
        String.format(Locale.US, "%.2f", ml / 1000.0).trimEnd('0').trimEnd('.')
    return "$s L"
}

private val WATER_LABELS = listOf("Glass", "Bottle", "A few sips")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrackerScreen(kind: Kind, state: AppState, update: ((AppState) -> AppState) -> Unit) {
    val isWater = kind == Kind.WATER
    val accent = if (isWater) WaterAccent else ProteinAccent
    val unit = if (isWater) "ml" else "g"
    val total = if (isWater) state.waterTotal else state.proteinTotal
    val goal = maxOf(1, if (isWater) state.waterGoal else state.proteinGoal)
    val entries = if (isWater) state.water else state.protein
    val quick = if (isWater) state.waterQuick else state.proteinQuick
    val fraction = total.toFloat() / goal
    val left = maxOf(0, goal - total)

    val scope = rememberCoroutineScope()
    val pops = remember { mutableStateListOf<Pop>() }
    var bump by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf(false) }
    var showCustom by remember { mutableStateOf(false) }
    var showGoal by remember { mutableStateOf(false) }
    var editEntry by remember { mutableStateOf<Entry?>(null) }

    fun add(amount: Int) {
        if (amount <= 0) return
        update { if (isWater) it.addWater(amount) else it.addProtein(amount) }
        bump++
        val id = System.nanoTime()
        pops.add(Pop(id, "+$amount $unit"))
        scope.launch {
            delay(1000)
            pops.removeAll { it.id == id }
        }
    }

    fun setQuick(index: Int, value: Int) = update { s ->
        if (isWater) s.copy(waterQuick = s.waterQuick.mapIndexed { j, v -> if (j == index) value else v })
        else s.copy(proteinQuick = s.proteinQuick.mapIndexed { j, v -> if (j == index) value else v })
    }

    val msg = if (isWater) {
        when {
            total == 0 -> "Fresh day! Start with one glass of water."
            fraction >= 1f -> "Goal smashed! Your water is done for today."
            fraction < 0.5f -> "Good start! ${fmt(left)} ml to go."
            else -> "Nice pace! ${(left + 249) / 250} more glasses and you hit ${liters(goal)}."
        }
    } else {
        when {
            total == 0 -> "Let's fuel up! Log your first meal."
            fraction >= 1f -> "Protein goal done. Strong work today!"
            fraction < 0.5f -> "$total g in. Keep it coming!"
            else -> "$total g already! Only $left g to go."
        }
    }

    val shown by animateIntAsState(total, tween(700, easing = FastOutSlowInEasing), label = "count")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GreetingHeader(state.firstName, state.streak)

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Hopping(bump) {
                if (isWater) DripMascot(Modifier.size(56.dp, 82.dp), celebrate = fraction >= 1f)
                else EggMascot(Modifier.size(58.dp, 74.dp), celebrate = fraction >= 1f)
            }
            AnimatedContent(
                targetState = msg,
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInHorizontally(tween(300)) { -it / 10 }) togetherWith fadeOut(tween(150))
                },
                label = "msg",
                modifier = Modifier.weight(1f)
            ) { m ->
                SpeechBubble(m, if (isWater) C.Line else Color(0xFFF1DCCB), Modifier.fillMaxWidth())
            }
        }

        if (isWater) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(150.dp, 190.dp), contentAlignment = Alignment.Center) {
                    WaterGlass(fraction, Modifier.size(150.dp, 190.dp))
                    PopLayer(pops, C.BlueDark, Modifier.size(150.dp, 190.dp))
                }
                Column {
                    Pulse(bump) { Text(fmt(shown), style = T.display(44, C.BlueDark)) }
                    Text("of ${fmt(goal)} ml", style = T.bold(15, C.Muted))
                    Spacer(Modifier.height(10.dp))
                    DoneBadge(fraction)
                    Spacer(Modifier.height(8.dp))
                    Pill("Goal ${liters(goal)} · edit", accent) { showGoal = true }
                }
            }
        } else {
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(C.OrangeCard, RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Pulse(bump) { Text("$shown", style = T.display(52, C.OrangeText)) }
                        Text("  / $goal g", style = T.bold(16, C.Muted), modifier = Modifier.weight(1f))
                        Pill("Goal · edit", accent) { showGoal = true }
                    }
                    Text(if (fraction >= 1f) "Goal reached!" else "$left g to go", style = T.bold(14, Color(0xFF8A5A3A)))
                    ProteinBar(fraction)
                    Milestones(total, goal)
                }
                PopLayer(
                    pops, C.OrangeText,
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(70.dp)
                )
            }
        }

        // Quick add
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Quick add")
                Pill(if (editing) "Done" else "Edit amounts", accent) { editing = !editing }
            }
            val cols = if (isWater) 2 else 4
            val cells = quick.indices.toList() + listOf(-1)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                cells.chunked(cols).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { idx ->
                            Box(Modifier.weight(1f)) {
                                when {
                                    idx == -1 -> Btn3D(
                                        onClick = { showCustom = true },
                                        face = accent.card, edge = accent.line, border = accent.line,
                                        minHeight = if (isWater) 62.dp else 50.dp,
                                        horizontalAlignment = if (isWater) Alignment.Start else Alignment.CenterHorizontally,
                                        contentPadding = if (isWater) PaddingValues(horizontal = 16.dp, vertical = 8.dp) else PaddingValues(4.dp)
                                    ) {
                                        Text("Custom", style = T.display(if (isWater) 22 else 17))
                                        if (isWater) Text("Type an amount", style = T.bold(13, C.Muted))
                                    }
                                    editing -> InlineNumber(
                                        value = quick[idx], unit = unit, accent = accent,
                                        onChange = { setQuick(idx, it) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    else -> {
                                        val primary = idx == 0
                                        val v = quick[idx]
                                        Btn3D(
                                            onClick = { add(v) },
                                            face = if (primary) accent.face else Color.White,
                                            edge = if (primary) accent.edge else accent.line,
                                            minHeight = if (isWater) 62.dp else 50.dp,
                                            horizontalAlignment = if (isWater) Alignment.Start else Alignment.CenterHorizontally,
                                            contentPadding = if (isWater) PaddingValues(horizontal = 16.dp, vertical = 8.dp) else PaddingValues(4.dp)
                                        ) {
                                            Text(
                                                "+$v $unit",
                                                style = T.display(if (isWater) 22 else 19, if (primary) Color.White else accent.text),
                                                maxLines = 1
                                            )
                                            if (isWater) {
                                                Text(
                                                    WATER_LABELS.getOrElse(idx) { "Quick" },
                                                    style = T.bold(13, if (primary) Color(0xFFE6F5FE) else C.Muted)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        // Quick foods (protein only)
        if (!isWater) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Quick foods")
                if (!editing) {
                    state.foods.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { f ->
                                Btn3D(
                                    onClick = { add(f.grams) },
                                    face = Color.White, edge = Color(0xFFE6EDF3), border = Color(0xFFE6EDF3),
                                    depth = 4.dp, modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(f.name, style = T.body(14), modifier = Modifier.weight(1f), maxLines = 1)
                                        Text("${f.grams} g", style = T.bold(14, C.OrangeText))
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                } else {
                    state.foods.forEachIndexed { i, f ->
                        key(i) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                InlineText(
                                    value = f.name,
                                    onChange = { n -> update { s -> s.copy(foods = s.foods.mapIndexed { j, x -> if (j == i) x.copy(name = n) else x }) } },
                                    modifier = Modifier.weight(1f)
                                )
                                InlineNumber(
                                    value = f.grams, unit = "g", accent = accent,
                                    onChange = { g -> update { s -> s.copy(foods = s.foods.mapIndexed { j, x -> if (j == i) x.copy(grams = g) else x }) } },
                                    modifier = Modifier.width(86.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Today's log
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Today · tap to change")
                val canUndo = entries.isNotEmpty()
                val undoAlpha by animateFloatAsState(if (canUndo) 1f else 0.4f, label = "undo")
                SquishButton(
                    onClick = {
                        if (canUndo) update { s ->
                            if (isWater) s.copy(water = s.water.dropLast(1)) else s.copy(protein = s.protein.dropLast(1))
                        }
                    },
                    background = Color.Transparent,
                    modifier = Modifier.graphicsLayer { alpha = undoAlpha },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                ) { Text("Undo last", style = T.bold(13, accent.text)) }
            }
            if (entries.isEmpty()) {
                Text("Nothing logged yet today.", style = T.body(14, C.Muted))
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    entries.forEach { e ->
                        key(e.id) {
                            EntryChip("${e.time} · ${e.amount} $unit", accent) { editEntry = e }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (showCustom) {
        AmountDialog(
            title = if (isWater) "Add water" else "Add protein",
            initial = 0, unit = unit, accent = accent, confirmLabel = "Add",
            onDismiss = { showCustom = false },
            onConfirm = { v ->
                add(v)
                showCustom = false
            }
        )
    }
    if (showGoal) {
        AmountDialog(
            title = "Daily goal",
            initial = goal, unit = unit, accent = accent, confirmLabel = "Save",
            step = if (isWater) 250 else 5,
            onDismiss = { showGoal = false },
            onConfirm = { v ->
                update { s -> if (isWater) s.copy(waterGoal = v) else s.copy(proteinGoal = v) }
                showGoal = false
            }
        )
    }
    editEntry?.let { e ->
        AmountDialog(
            title = "Change the ${e.time} entry",
            initial = e.amount, unit = unit, accent = accent, confirmLabel = "Save",
            onDismiss = { editEntry = null },
            onConfirm = { v ->
                update { s ->
                    if (isWater) s.copy(water = s.water.map { if (it.id == e.id) it.copy(amount = v) else it })
                    else s.copy(protein = s.protein.map { if (it.id == e.id) it.copy(amount = v) else it })
                }
                bump++
                editEntry = null
            },
            onDelete = {
                update { s ->
                    if (isWater) s.copy(water = s.water.filter { it.id != e.id })
                    else s.copy(protein = s.protein.filter { it.id != e.id })
                }
                editEntry = null
            }
        )
    }
}

@Composable
private fun DoneBadge(fraction: Float) {
    val text = if (fraction >= 1f) "Goal hit!" else "${(fraction * 100).toInt()}% done"
    AnimatedContent(targetState = text, label = "badge") { t ->
        Text(
            t,
            style = T.bold(13, C.GreenText),
            modifier = Modifier
                .background(C.GreenSoft, CircleShape)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun EntryChip(text: String, accent: Accent, onClick: () -> Unit) {
    val appear = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)) }
    Box(Modifier.graphicsLayer { scaleX = appear.value; scaleY = appear.value; alpha = ((appear.value - 0.6f) / 0.4f).coerceIn(0f, 1f) }) {
        SquishButton(
            onClick = onClick,
            background = if (accent == WaterAccent) C.Faint else Color(0xFFF6F1EC),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) { Text(text, style = T.body(13)) }
    }
}

/** A small box for typing a number straight into a quick-add slot. */
@Composable
fun InlineNumber(value: Int, unit: String, accent: Accent, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    var text by remember(value) { mutableStateOf(if (value > 0) value.toString() else "") }
    Column(
        modifier = modifier
            .heightIn(min = 60.dp)
            .background(accent.card, RoundedCornerShape(16.dp))
            .border(2.dp, accent.face.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BasicTextField(
            value = text,
            onValueChange = { v ->
                val d = v.filter { it.isDigit() }.take(4)
                text = d
                d.toIntOrNull()?.let { if (it > 0) onChange(it) }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = T.display(20).copy(textAlign = TextAlign.Center),
            cursorBrush = SolidColor(accent.face),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(10.dp))
                .padding(vertical = 4.dp)
        )
        Text(unit, style = T.bold(12, accent.text))
    }
}

@Composable
fun InlineText(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf(value) }
    BasicTextField(
        value = text,
        onValueChange = { v ->
            val t = v.take(24)
            text = t
            onChange(t)
        },
        singleLine = true,
        textStyle = T.body(15),
        cursorBrush = SolidColor(C.Orange),
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(2.dp, C.OrangeLine, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    )
}
