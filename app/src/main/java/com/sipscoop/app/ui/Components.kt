package com.sipscoop.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime

/** The chunky button that sinks into its own edge when pressed. */
@Composable
fun Btn3D(
    onClick: () -> Unit,
    face: Color,
    edge: Color,
    modifier: Modifier = Modifier,
    border: Color = edge,
    enabled: Boolean = true,
    fill: Boolean = true,
    radius: Dp = 16.dp,
    depth: Dp = 5.dp,
    minHeight: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shift by animateDpAsState(
        targetValue = if (pressed && enabled) depth - 1.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessHigh),
        label = "press"
    )
    val shape = RoundedCornerShape(radius)
    Box(
        modifier.clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(top = depth)
                .background(edge, shape)
        )
        Column(
            modifier = Modifier
                .padding(bottom = depth)
                .offset(y = shift)
                .then(if (fill) Modifier.fillMaxWidth() else Modifier)
                .heightIn(min = minHeight)
                .background(face, shape)
                .border(2.dp, border, shape)
                .padding(contentPadding),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = Arrangement.Center,
            content = content
        )
    }
}

/** A soft rounded button that squishes a little when pressed. */
@Composable
fun SquishButton(
    onClick: () -> Unit,
    background: Color,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(50),
    borderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateDpAsState(
        targetValue = if (pressed) 92.dp else 100.dp,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "squish"
    )
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale.value / 100f
                scaleY = scale.value / 100f
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .background(background, shape)
            .then(if (borderColor != null) Modifier.border(2.dp, borderColor, shape) else Modifier)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun Pill(text: String, accent: Accent, onClick: () -> Unit) {
    SquishButton(onClick = onClick, background = accent.soft) {
        Text(text, style = T.bold(13, accent.text))
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = T.label(), modifier = modifier)
}

@Composable
fun SpeechBubble(text: String, borderColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text, style = T.body(15))
    }
}

@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String,
    accent: Accent,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onValueChange(v.filter { it.isDigit() }.take(5)) },
        modifier = modifier,
        singleLine = true,
        label = { Text(label) },
        suffix = { Text(suffix, style = T.bold(15, C.Muted)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = TextStyle(fontFamily = Fonts.display, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = C.Ink),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent.face,
            unfocusedBorderColor = C.Line,
            focusedLabelColor = accent.text,
            cursorColor = accent.face
        )
    )
}

/** Lightning-bolt streak badge that gives a little zap every few seconds. */
@Composable
fun StreakBadge(days: Int) {
    val inf = rememberInfiniteTransition(label = "zap")
    val rot by inf.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 4000
                0f at 3400
                -14f at 3560
                10f at 3720
                0f at 3880
            }
        ),
        label = "rot"
    )
    Row(
        modifier = Modifier
            .background(C.YellowSoft, CircleShape)
            .border(2.dp, C.YellowLine, CircleShape)
            .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Canvas(
            Modifier
                .size(24.dp)
                .graphicsLayer { rotationZ = rot }
        ) {
            inViewBox(0f, 0f, 24f, 24f) {
                val bolt = Path().apply {
                    moveTo(13.5f, 2f); lineTo(4.5f, 13.5f); lineTo(11f, 13.5f)
                    lineTo(9.5f, 22f); lineTo(19.5f, 9.5f); lineTo(13f, 9.5f); close()
                }
                drawPath(bolt, C.Yellow)
                drawPath(bolt, C.YellowDark, style = Stroke(width = 1.6f, join = StrokeJoin.Round))
                drawLine(Color(0xFFFFF1A8), Offset(12.4f, 5.5f), Offset(8f, 11.5f), strokeWidth = 1.6f, cap = StrokeCap.Round)
            }
        }
        Text("$days", style = T.display(18, C.YellowText))
    }
}

enum class DayPart { DAY, DUSK, NIGHT }

fun greeting(): Pair<String, DayPart> {
    val h = LocalTime.now().hour
    return when (h) {
        in 5..11 -> "Good morning" to DayPart.DAY
        in 12..16 -> "Good afternoon" to DayPart.DAY
        in 17..20 -> "Good evening" to DayPart.DUSK
        else -> "Good night" to DayPart.NIGHT
    }
}

@Composable
fun DayIcon(part: DayPart) {
    Canvas(Modifier.size(18.dp)) {
        inViewBox(0f, 0f, 24f, 24f) {
            when (part) {
                DayPart.DAY -> {
                    drawCircle(C.Yellow, 5f, Offset(12f, 12f))
                    val rays = listOf(
                        Offset(12f, 2.5f) to Offset(12f, 5f), Offset(12f, 19f) to Offset(12f, 21.5f),
                        Offset(2.5f, 12f) to Offset(5f, 12f), Offset(19f, 12f) to Offset(21.5f, 12f),
                        Offset(5.3f, 5.3f) to Offset(7f, 7f), Offset(17f, 17f) to Offset(18.7f, 18.7f),
                        Offset(5.3f, 18.7f) to Offset(7f, 17f), Offset(17f, 7f) to Offset(18.7f, 5.3f)
                    )
                    rays.forEach { (a, b) -> drawLine(C.Yellow, a, b, strokeWidth = 2f, cap = StrokeCap.Round) }
                }
                DayPart.DUSK -> {
                    val sun = Path().apply {
                        moveTo(5f, 16f)
                        arcTo(androidx.compose.ui.geometry.Rect(5f, 9f, 19f, 23f), 180f, 180f, false)
                        close()
                    }
                    drawPath(sun, C.Orange)
                    drawLine(C.Orange, Offset(2.5f, 19f), Offset(21.5f, 19f), strokeWidth = 2f, cap = StrokeCap.Round)
                }
                DayPart.NIGHT -> {
                    drawCircle(Color(0xFF7B86D9), 8f, Offset(11f, 13f))
                    drawCircle(Color.White, 7f, Offset(15.5f, 9f))
                }
            }
        }
    }
}

@Composable
fun GreetingHeader(name: String, streak: Int) {
    val (text, part) = greeting()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DayIcon(part)
                Text("$text,", style = T.bold(14, C.Muted))
            }
            Text(name, style = T.display(28), maxLines = 1)
        }
        StreakBadge(streak)
    }
}

/** A number that floats up and fades, like "+250 ml". */
data class Pop(val id: Long, val text: String)

@Composable
fun PopLayer(pops: SnapshotStateList<Pop>, color: Color, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        pops.forEach { p ->
            key(p.id) { FloatingText(p.text, color) }
        }
    }
}

@Composable
private fun FloatingText(text: String, color: Color) {
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(950, easing = FastOutSlowInEasing)) }
    Text(
        text,
        style = T.display(24, color),
        modifier = Modifier.graphicsLayer {
            val v = t.value
            translationY = -v * 90.dp.toPx()
            alpha = if (v < 0.2f) v / 0.2f else 1f - (v - 0.2f) / 0.8f
            val s = 0.7f + 0.45f * minOf(1f, v * 4f) - 0.15f * maxOf(0f, v - 0.25f)
            scaleX = s
            scaleY = s
        }
    )
}

/** Hops whenever [bump] changes and bobs gently while idle. */
@Composable
fun Hopping(bump: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val hop = remember { Animatable(0f) }
    val squash = remember { Animatable(1f) }
    LaunchedEffect(bump) {
        if (bump > 0) {
            hop.animateTo(-16f, tween(140))
            hop.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium))
        }
    }
    LaunchedEffect(bump) {
        if (bump > 0) {
            squash.animateTo(1.08f, tween(140))
            squash.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium))
        }
    }
    val inf = rememberInfiniteTransition(label = "idle")
    val idle by inf.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    Box(
        modifier.graphicsLayer {
            translationY = (hop.value + idle) * density
            scaleY = squash.value
            scaleX = 2f - squash.value
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
        }
    ) { content() }
}

/** Pops a little bigger whenever [bump] changes. */
@Composable
fun Pulse(bump: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val s = remember { Animatable(1f) }
    LaunchedEffect(bump) {
        if (bump > 0) {
            s.animateTo(1.14f, tween(120))
            s.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
        }
    }
    Box(
        modifier.graphicsLayer {
            scaleX = s.value
            scaleY = s.value
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.7f)
        }
    ) { content() }
}

/** Dialog to type or adjust an amount; optional step buttons and a Delete button. */
@Composable
fun AmountDialog(
    title: String,
    initial: Int,
    unit: String,
    accent: Accent,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    step: Int = 0,
    onDelete: (() -> Unit)? = null
) {
    var text by remember { mutableStateOf(if (initial > 0) initial.toString() else "") }
    val value = text.toIntOrNull() ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, style = T.display(22)) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step > 0) {
                    Btn3D(
                        onClick = { text = maxOf(step, value - step).toString() },
                        face = Color.White, edge = accent.line, border = accent.line,
                        modifier = Modifier.size(52.dp, 56.dp), contentPadding = PaddingValues(0.dp)
                    ) { Text("−", style = T.display(24, accent.text)) }
                }
                NumberField(
                    value = text, onValueChange = { text = it }, label = "Amount", suffix = unit,
                    accent = accent, modifier = Modifier.weight(1f)
                )
                if (step > 0) {
                    Btn3D(
                        onClick = { text = (value + step).toString() },
                        face = Color.White, edge = accent.line, border = accent.line,
                        modifier = Modifier.size(52.dp, 56.dp), contentPadding = PaddingValues(0.dp)
                    ) { Text("+", style = T.display(24, accent.text)) }
                }
            }
        },
        confirmButton = {
            Btn3D(
                onClick = { if (value > 0) onConfirm(value) },
                face = if (value > 0) accent.face else C.Grey,
                edge = if (value > 0) accent.edge else C.Line,
                enabled = value > 0, fill = false, depth = 4.dp,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
            ) { Text(confirmLabel, style = T.bold(15, if (value > 0) Color.White else C.GreyText)) }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    Btn3D(
                        onClick = onDelete, face = Color.White, edge = Color(0xFFF0CFCB),
                        fill = false, depth = 4.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) { Text("Delete", style = T.bold(15, C.Red)) }
                }
                Btn3D(
                    onClick = onDismiss, face = Color.White, edge = C.Line,
                    fill = false, depth = 4.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) { Text("Cancel", style = T.bold(15, C.Muted)) }
            }
        }
    )
}
