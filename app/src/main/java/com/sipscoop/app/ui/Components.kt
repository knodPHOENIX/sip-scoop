package com.sipscoop.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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

/**
 * The main button: flat, with a thin edge, that dips slightly when pressed. It keeps the
 * same outer size the old raised button had, so nothing on screen moves.
 */
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
    val scale by animateFloatAsState(if (pressed && enabled) 0.97f else 1f, tween(110), label = "press")
    val shape = RoundedCornerShape(minOf(radius, 14.dp))
    // A filled colour button gets a matching edge; an outlined one keeps its line colour.
    val line = if (border == edge && face != C.Surface && face != C.Faint && face != C.Bg) face else border
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (pressed && enabled) 0.88f else 1f
            }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .then(if (fill) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = minHeight + depth)
            .then(
                if (isNeutralGlass(face)) {
                    Modifier.glass(shape).then(if (border != edge) Modifier.border(1.dp, border, shape) else Modifier)
                } else {
                    Modifier.background(face, shape).glassSheen(shape).border(1.dp, glassEdge(line), shape)
                }
            )
            .padding(contentPadding)
            .padding(vertical = depth / 2),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.Center,
        content = content
    )
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
        targetValue = if (pressed) 96.dp else 100.dp,
        animationSpec = tween(110),
        label = "squish"
    )
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale.value / 100f
                scaleY = scale.value / 100f
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .then(
                if (isNeutralGlass(background)) {
                    Modifier.glass(shape).then(
                        if (borderColor != null && borderColor != C.Line && borderColor != C.NavLine) Modifier.border(1.dp, borderColor, shape) else Modifier
                    )
                } else {
                    Modifier.background(background, shape).glassSheen(shape)
                        .border(1.dp, glassEdge(borderColor ?: background), shape)
                }
            )
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
            .glass(RoundedCornerShape(16.dp))
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
        textStyle = TextStyle(fontFamily = Fonts.display, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = C.Ink),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent.face,
            unfocusedBorderColor = C.Line,
            focusedLabelColor = accent.text,
            unfocusedLabelColor = C.Muted,
            cursorColor = accent.face
        )
    )
}

/**
 * A small streak capsule. Gold shoe = step streak; blue bolt = diet streak (water and protein).
 * Tapping it opens that streak's calendar.
 */
@Composable
fun StreakBadge(days: Int, steps: Boolean, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClickLabel = if (steps) "Open step streak calendar" else "Open diet streak calendar", onClick = onClick)
            .glass(CircleShape, tint = if (steps) C.Gold else C.Blue)
            .heightIn(min = 26.dp)
            .padding(start = 8.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (steps) {
            ShoeIcon(15.dp)
        } else {
            Canvas(Modifier.size(14.dp)) {
                inViewBox(0f, 0f, 24f, 24f) {
                    drawPath(
                        Path().apply {
                            moveTo(13.5f, 2f); lineTo(4.5f, 13.5f); lineTo(11f, 13.5f)
                            lineTo(9.5f, 22f); lineTo(19.5f, 9.5f); lineTo(13f, 9.5f); close()
                        },
                        C.Blue
                    )
                }
            }
        }
        Text("$days", style = T.bold(13, if (steps) C.YellowText else C.BlueDark))
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
                    drawCircle(C.Bg, 7f, Offset(15.5f, 9f))
                }
            }
        }
    }
}

@Composable
fun GreetingHeader(
    name: String,
    stepStreak: Int,
    dietStreak: Int,
    avatar: String,
    onStepStreak: () -> Unit = {},
    onDietStreak: () -> Unit = {},
    onProfile: () -> Unit
) {
    val (text, _) = greeting()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text("$text,", style = T.body(14, C.Muted), maxLines = 1)
            Text(name, style = T.display(24), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        StreakBadge(stepStreak, steps = true, onClick = onStepStreak)
        StreakBadge(dietStreak, steps = false, onClick = onDietStreak)
        SquishButton(
            onClick = onProfile,
            background = C.Surface,
            modifier = Modifier.padding(start = 4.dp),
            shape = RoundedCornerShape(50),
            borderColor = C.Line,
            contentPadding = PaddingValues(1.dp)
        ) {
            AvatarImage(avatar, Modifier.size(42.dp).clip(CircleShape))
        }
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

/** Most you can set or log in one go, and the gentle "too low" goal nudges. */
object Limits {
    const val WATER_MAX = com.sipscoop.app.data.WATER_DAY_MAX
    const val PROTEIN_MAX = com.sipscoop.app.data.PROTEIN_DAY_MAX
    const val WATER_LOW = 2000
    const val PROTEIN_LOW = 50

    fun max(isWater: Boolean) = if (isWater) WATER_MAX else PROTEIN_MAX

    fun goalWarning(isWater: Boolean, goal: Int): String? = when {
        goal <= 0 -> null
        isWater && goal < WATER_LOW -> "That's too low. Please keep it at least 2 litres a day for a healthy routine."
        !isWater && goal < PROTEIN_LOW -> "That's not enough for an adult. Aim for at least 50 g a day."
        else -> null
    }

    fun maxNote(isWater: Boolean): String =
        if (isWater) "10 litres is the most you can set. Protein goes up to 400 g."
        else "400 g is the most you can set. Water goes up to 10 litres."
}

/** A yellow warning or blue info note under a number field. */
@Composable
fun NoteBox(text: String, warning: Boolean) {
    val bg = if (warning) C.YellowSoft else C.BlueSoft
    val line = if (warning) C.YellowLine else C.BlueLine
    val fg = if (warning) C.YellowText else C.BlueDark
    Row(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(14.dp), tint = if (warning) C.Gold else C.Blue)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Canvas(Modifier.size(20.dp)) {
            inViewBox(0f, 0f, 24f, 24f) {
                drawPath(svg("M12 3 L22 20 L2 20 Z"), fg, style = Stroke(width = 2.2f, join = StrokeJoin.Round))
                drawLine(fg, Offset(12f, 10f), Offset(12f, 14f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                drawCircle(fg, 1.3f, Offset(12f, 17f))
            }
        }
        Text(text, style = T.body(14, fg), modifier = Modifier.weight(1f))
    }
}

/**
 * Every pop-up in the app: a rounded card with a round ✕ in the top-right corner.
 * Tapping outside or pressing back also closes it. There is no Cancel/Close button.
 */
@Composable
fun Popup(
    onDismiss: () -> Unit,
    centered: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GlassBlurBehind()
        Box(
            Modifier
                .padding(horizontal = 22.dp, vertical = 24.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .wrapContentHeight()
                .background(
                    Brush.verticalGradient(listOf(C.SheetTop, C.SheetBottom)),
                    RoundedCornerShape(24.dp)
                )
                .border(1.dp, glassEdge(C.Line), RoundedCornerShape(24.dp))
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp),
                horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
            SquishButton(
                onClick = onDismiss,
                background = C.Faint,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp),
                contentPadding = PaddingValues(9.dp)
            ) { LineIcon(Ic.CLOSE, C.Muted, 14.dp, 2.4f) }
        }
    }
}

/**
 * Frosts whatever is behind a pop-up (Android 12 and newer). Older phones simply show the
 * pop-up's own glass colour. Call it first thing inside a Dialog.
 */
@Composable
fun GlassBlurBehind(radius: Int = 48) {
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.SideEffect {
        val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        // A deeper dim on every phone, so the frosted sheet stands out.
        if (window != null && android.os.Build.VERSION.SDK_INT < 31) runCatching { window.setDimAmount(0.6f) }
        if (window != null && android.os.Build.VERSION.SDK_INT >= 31) {
            runCatching {
                window.setDimAmount(0.6f)
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                val attrs = window.attributes
                attrs.blurBehindRadius = radius
                window.attributes = attrs
            }
        }
    }
}

/** The single wide main button at the bottom of a pop-up. */
@Composable
fun PopupButton(label: String, face: Color, edge: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Btn3D(
        onClick = onClick,
        face = if (enabled) face else C.Grey,
        edge = if (enabled) edge else C.Line,
        enabled = enabled,
        depth = 4.dp, radius = 14.dp,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        contentPadding = PaddingValues(vertical = 11.dp)
    ) { Text(label, style = T.bold(16, if (enabled) Color.White else C.GreyText)) }
}

/** Popup title that leaves room for the ✕ when it is left-aligned. */
@Composable
fun PopupTitle(text: String, centered: Boolean = true) {
    Text(
        text,
        style = T.display(22),
        textAlign = if (centered) androidx.compose.ui.text.style.TextAlign.Center else null,
        modifier = if (centered) Modifier.padding(horizontal = 30.dp) else Modifier.padding(end = 40.dp)
    )
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
    onDelete: (() -> Unit)? = null,
    max: Int = Int.MAX_VALUE,
    maxNote: String? = null,
    warning: (Int) -> String? = { null }
) {
    var text by remember { mutableStateOf(if (initial > 0) minOf(initial, max).toString() else "") }
    var hitMax by remember { mutableStateOf(false) }
    val value = text.toIntOrNull() ?: 0
    val atMax = value >= max
    val warn = warning(value)

    fun setAmount(v: Int) {
        hitMax = v >= max
        text = v.coerceIn(0, max).toString()
    }

    Popup(onDismiss = onDismiss, centered = false) {
        PopupTitle(title, centered = false)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) {
                Btn3D(
                    onClick = { setAmount(maxOf(step, value - step)) },
                    face = C.Surface, edge = accent.line, border = accent.line,
                    modifier = Modifier.size(52.dp, 56.dp), contentPadding = PaddingValues(0.dp)
                ) { Text("−", style = T.display(24, accent.text)) }
            }
            NumberField(
                value = text,
                onValueChange = { v ->
                    val n = v.toIntOrNull()
                    hitMax = false
                    text = v
                    if (n != null && n > max) setAmount(max)
                },
                label = "Amount", suffix = unit,
                accent = accent, modifier = Modifier.weight(1f)
            )
            if (step > 0) {
                Btn3D(
                    onClick = {
                        hitMax = atMax
                        if (!atMax) setAmount(value + step)
                    },
                    face = if (atMax) C.Faint else C.Surface,
                    edge = accent.line, border = accent.line,
                    modifier = Modifier.size(52.dp, 56.dp), contentPadding = PaddingValues(0.dp)
                ) { Text("+", style = T.display(24, if (atMax) C.Line else accent.text)) }
            }
        }
        if (warn != null) NoteBox(warn, warning = true)
        else if ((hitMax || atMax) && maxNote != null) NoteBox(maxNote, warning = false)
        PopupButton(confirmLabel, accent.face, accent.edge, enabled = value > 0) { if (value > 0) onConfirm(value) }
        if (onDelete != null) {
            Btn3D(
                onClick = onDelete, face = C.Surface, edge = C.RedLine, border = C.RedLine,
                depth = 4.dp, radius = 14.dp,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 9.dp)
            ) { Text("Delete", style = T.bold(15, C.Red)) }
        }
    }
}
