package com.sipscoop.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sipscoop.app.data.AppState

/** First-time setup: name, age, then daily goals. */
@Composable
fun Onboarding(initial: AppState, onFinish: (name: String, age: Int, avatar: String, water: Int, protein: Int) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf(initial.name) }
    var age by remember { mutableStateOf(initial.age.toString()) }
    var water by remember { mutableStateOf(initial.waterGoal.toString()) }
    var protein by remember { mutableStateOf(initial.proteinGoal.toString()) }
    var avatar by remember { mutableStateOf(initial.avatar) }

    val ageN = age.toIntOrNull() ?: 0
    val waterN = water.toIntOrNull() ?: 0
    val proteinN = protein.toIntOrNull() ?: 0
    val firstName = name.trim().split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: "friend"
    val canGo = when (step) {
        0 -> name.isNotBlank()
        1 -> ageN in 5..110
        2 -> true
        3 -> waterN in 1..Limits.WATER_MAX && proteinN in 1..Limits.PROTEIN_MAX
        else -> true
    }
    val progress by animateFloatAsState(step / 4f, tween(600, easing = FastOutSlowInEasing), label = "progress")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(C.Bg)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AnimatedVisibility(visible = step in 1..3, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                SquishButton(onClick = { step = maxOf(0, step - 1) }, background = C.Faint, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(10.dp)) {
                    Canvas(Modifier.size(20.dp)) {
                        inViewBox(0f, 0f, 24f, 24f) {
                            val p = Path().apply { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) }
                            drawPath(p, C.Muted, style = Stroke(width = 3f, cap = StrokeCap.Round))
                        }
                    }
                }
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(16.dp)
                    .clip(CircleShape)
                    .background(C.NavLine)
            ) {
                if (progress > 0.01f) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(C.Green)
                    )
                }
            }
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(420)) { w -> if (forward) w / 3 else -w / 3 } + fadeIn(tween(420))) togetherWith
                    (slideOutHorizontally(tween(300)) { w -> if (forward) -w / 3 else w / 3 } + fadeOut(tween(200)))
            },
            label = "steps",
            modifier = Modifier.weight(1f)
        ) { s ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                when (s) {
                    0 -> {
                        Spacer(Modifier.height(16.dp))
                        Asking("Hi, I'm Drip! What should I call you?")
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it.take(30) },
                            singleLine = true,
                            label = { Text("Your name") },
                            placeholder = { Text("Type your name") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            textStyle = T.bold(20),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = C.Blue, unfocusedBorderColor = C.Line,
                                focusedLabelColor = C.BlueDark, unfocusedLabelColor = C.Muted, cursorColor = C.Blue
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    1 -> {
                        Spacer(Modifier.height(16.dp))
                        Asking("Nice to meet you, $firstName! How old are you?")
                        Stepper(
                            label = "Age", value = age, unit = "years", accent = WaterAccent,
                            onChange = { age = it },
                            onMinus = { age = maxOf(5, ageN - 1).toString() },
                            onPlus = { age = minOf(110, ageN + 1).toString() },
                            background = C.BlueCard
                        )
                    }
                    2 -> {
                        Spacer(Modifier.height(8.dp))
                        Asking("Pick your character, $firstName! It shows on your profile.", small = true)
                        AvatarGrid(avatar, { avatar = it })
                        Text(
                            "You can change it later in your profile.",
                            style = T.body(14, C.Muted),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    3 -> {
                        Asking("Last step! Set your daily goals. You can change them anytime.", small = true)
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(C.BlueCard, RoundedCornerShape(22.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Water per day", style = T.bold(16, C.BlueDark))
                            Stepper(
                                label = "Water", value = water, unit = "ml", accent = WaterAccent,
                                onChange = { v -> water = clampGoal(v, Limits.WATER_MAX) },
                                onMinus = { water = maxOf(250, waterN - 250).toString() },
                                onPlus = { water = minOf(Limits.WATER_MAX, waterN + 250).toString() },
                                background = Color.Transparent
                            )
                            Text(
                                "That's about ${(waterN + 125) / 250} glasses of 250 ml",
                                style = T.body(14, C.BlueDark),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Limits.goalWarning(true, waterN)?.let { NoteBox(it, warning = true) }
                            if (waterN >= Limits.WATER_MAX) NoteBox("10 litres is the most you can set.", warning = false)
                        }
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(C.OrangeCard, RoundedCornerShape(22.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Protein per day", style = T.bold(16, C.OrangeText))
                            Stepper(
                                label = "Protein", value = protein, unit = "g", accent = ProteinAccent,
                                onChange = { v -> protein = clampGoal(v, Limits.PROTEIN_MAX) },
                                onMinus = { protein = maxOf(5, proteinN - 5).toString() },
                                onPlus = { protein = minOf(Limits.PROTEIN_MAX, proteinN + 5).toString() },
                                background = Color.Transparent
                            )
                            Limits.goalWarning(false, proteinN)?.let { NoteBox(it, warning = true) }
                            if (proteinN >= Limits.PROTEIN_MAX) NoteBox("400 g is the most you can set.", warning = false)
                        }
                    }
                    else -> AllSet(firstName, avatar, waterN, proteinN)
                }
            }
        }

        Btn3D(
            onClick = {
                if (step < 4) {
                    if (canGo) step++
                } else {
                    onFinish(name.trim(), ageN, avatar, waterN, proteinN)
                }
            },
            face = if (canGo) C.Green else C.Grey,
            edge = if (canGo) C.GreenDark else C.Line,
            enabled = canGo,
            depth = 6.dp,
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Text(if (step < 4) "Continue" else "Start tracking", style = T.display(20, if (canGo) Color.White else C.GreyText))
        }
    }
}

@Composable
private fun Asking(text: String, small: Boolean = false) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Hopping(0, Modifier.padding(top = 18.dp)) {
            DripMascot(if (small) Modifier.size(62.dp, 90.dp) else Modifier.size(76.dp, 110.dp))
        }
        SpeechBubble(text, C.Line, Modifier.weight(1f))
    }
}

@Composable
private fun Stepper(
    label: String,
    value: String,
    unit: String,
    accent: Accent,
    onChange: (String) -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    background: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(22.dp))
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Btn3D(
            onClick = onMinus, face = C.Surface, edge = accent.line, border = accent.line,
            modifier = Modifier.size(54.dp, 58.dp), contentPadding = PaddingValues(0.dp)
        ) { Text("−", style = T.display(26, accent.text)) }
        NumberField(
            value = value, onValueChange = onChange, label = label, suffix = unit,
            accent = accent, modifier = Modifier.weight(1f)
        )
        Btn3D(
            onClick = onPlus, face = C.Surface, edge = accent.line, border = accent.line,
            modifier = Modifier.size(54.dp, 58.dp), contentPadding = PaddingValues(0.dp)
        ) { Text("+", style = T.display(26, accent.text)) }
    }
}

@Composable
private fun AllSet(firstName: String, avatar: String, water: Int, protein: Int) {
    val inf = rememberInfiniteTransition(label = "confetti")
    val fall by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2800, easing = LinearEasing)), label = "fall")
    Box(Modifier.fillMaxWidth().height(520.dp)) {
        Canvas(Modifier.matchParentSize()) {
            val colors = listOf(C.Blue, C.Orange, C.Yellow, C.Green, C.Blue, C.Orange)
            colors.forEachIndexed { i, c ->
                val k = (fall + i * 0.17f) % 1f
                val x = size.width * (0.08f + i * 0.17f)
                val y = -30f + k * (size.height + 60f)
                drawRoundRect(
                    c, topLeft = Offset(x, y), size = Size(22f, 30f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f),
                    alpha = if (k > 0.85f) (1f - k) / 0.15f else 1f
                )
            }
        }
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Hopping(1) { AvatarImage(avatar, Modifier.size(110.dp)) }
                Hopping(1) { MascotPair(Modifier.size(150.dp, 142.dp), dripColor = C.Blue, celebrate = true) }
            }
            Text("You're all set, $firstName!", style = T.display(32), textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "${liters(water)} water", style = T.bold(15, C.BlueDark),
                    modifier = Modifier.background(C.BlueSoft, CircleShape).padding(horizontal = 14.dp, vertical = 8.dp)
                )
                Text(
                    "$protein g protein", style = T.bold(15, C.OrangeText),
                    modifier = Modifier.background(C.OrangeSoft, CircleShape).padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

private fun clampGoal(v: String, max: Int): String {
    val n = v.toIntOrNull() ?: return v
    return if (n > max) max.toString() else v
}
