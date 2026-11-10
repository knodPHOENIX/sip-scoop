package com.sipscoop.app.ui

import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sipscoop.app.data.AVATAR_IDS
import com.sipscoop.app.data.AppState
import com.sipscoop.app.data.DayRecord
import com.sipscoop.app.data.resetToday
import com.sipscoop.app.notify.shareApp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------------------------------------------------------------- shared bits

@Composable
fun TopBar(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SquishButton(onClick = onBack, background = C.Faint, shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(11.dp)) {
            LineIcon(Ic.BACK, C.Muted, 22.dp, 3f)
        }
        Text(title, style = T.display(24), modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun IconSquare(bg: Color, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .background(bg, RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun MenuRow(
    title: String,
    iconBg: Color,
    icon: @Composable () -> Unit,
    value: String? = null,
    showDivider: Boolean = true,
    onClick: () -> Unit
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .background(iconBg, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) { icon() }
            Text(title, style = T.bold(15), modifier = Modifier.weight(1f), maxLines = 1)
            if (value != null) Text(value, style = T.body(13, C.Muted), maxLines = 1)
            LineIcon(Ic.CHEVRON, C.Muted, 16.dp, 2.5f)
        }
        if (showDivider) Box(Modifier.fillMaxWidth().height(1.5.dp).background(C.NavLine))
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(C.Surface, RoundedCornerShape(18.dp))
            .border(2.dp, C.Line, RoundedCornerShape(18.dp)),
        content = content
    )
}

@Composable
fun Toggle(on: Boolean, label: String, onChange: (Boolean) -> Unit) {
    val track by animateColorAsState(if (on) C.Green else C.Line, tween(220), label = "track")
    val knob by animateDpAsState(if (on) 23.dp else 3.dp, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), label = "knob")
    Box(
        Modifier
            .size(50.dp, 30.dp)
            .background(track, CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClickLabel = label) { onChange(!on) }
    ) {
        Box(
            Modifier
                .offset(x = knob, y = 3.dp)
                .size(24.dp)
                .background(Color.White, CircleShape)
        )
    }
}

/** A plain pop-up with an icon, a message and one main button. Close with the ✕. */
@Composable
fun MessageDialog(
    title: String,
    body: String,
    confirmLabel: String,
    confirmFace: Color,
    confirmEdge: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    icon: @Composable () -> Unit = {},
    extra: @Composable () -> Unit = {}
) {
    Popup(onDismiss = onDismiss) {
        icon()
        PopupTitle(title)
        Text(body, style = T.body(15, C.Muted), textAlign = TextAlign.Center)
        extra()
        PopupButton(confirmLabel, confirmFace, confirmEdge, onClick = onConfirm)
    }
}

/** The six characters as big tiles; the chosen one gets a blue border and a tick. */
@Composable
fun AvatarGrid(selected: String, onPick: (String) -> Unit, tile: Int = 84) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AVATAR_IDS.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { id ->
                    val sel = id == selected
                    Box(Modifier.weight(1f)) {
                        Btn3D(
                            onClick = { onPick(id) },
                            face = if (sel) C.BlueSoft else C.Surface,
                            edge = if (sel) C.Blue else C.Line,
                            border = if (sel) C.Blue else C.Line,
                            radius = 20.dp, depth = 5.dp,
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) { AvatarImage(id, Modifier.size(tile.dp)) }
                        if (sel) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-6).dp)
                                    .size(28.dp)
                                    .background(C.Green, CircleShape)
                                    .border(3.dp, C.Bg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) { LineIcon(Ic.CHECK, Color.White, 14.dp, 4f) }
                        }
                    }
                }
            }
        }
    }
}

private fun prettyDate(iso: String, pattern: String): String =
    runCatching { LocalDate.parse(iso).format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)) }.getOrDefault(iso)

// ---------------------------------------------------------------- profile

@Composable
fun ProfileScreen(
    state: AppState,
    update: ((AppState) -> AppState) -> Unit,
    onBack: () -> Unit,
    onAppearance: () -> Unit,
    onNotifications: () -> Unit,
    onHistory: () -> Unit
) {
    val context = LocalContext.current
    var showBug by remember { mutableStateOf(false) }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }
    var showEdit by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }
    var showLogout by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(C.Bg)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TopBar("Profile", onBack)

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // Tapping the avatar opens Edit profile. No pencil on purpose: it's there to be discovered.
                SquishButton(
                    onClick = { showEdit = true },
                    background = C.BlueSoft,
                    borderColor = C.Blue,
                    contentPadding = PaddingValues(5.dp)
                ) { AvatarImage(state.avatar, Modifier.size(80.dp)) }
                Text(state.name.ifBlank { "Friend" }, style = T.display(24), modifier = Modifier.padding(top = 4.dp))
                Text("${state.age} years · tracking since ${prettyDate(state.since, "d MMM")}", style = T.body(13, C.Muted))
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StreakBar(Modifier.weight(1f), C.YellowSoft, { ShoeIcon(17.dp) }, "${state.stepStreak}-day step streak", C.YellowText)
                StreakBar(Modifier.weight(1f), C.BlueSoft, { FillIcon(Ic.BOLT, C.Blue, 16.dp) }, "${state.streak}-day diet streak", C.BlueDark)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(Modifier.weight(1f), C.YellowSoft, { ShoeIcon(20.dp) }, fmt(state.stepGoal), C.YellowText, "Step goal")
                StatTile(Modifier.weight(1f), C.BlueSoft, { FillIcon(Ic.DROP, C.Blue, 18.dp) }, liters(state.waterGoal), C.BlueDark, "Water goal")
                StatTile(Modifier.weight(1f), C.OrangeSoft, { FillIcon(Ic.EGG, C.Orange, 18.dp) }, "${state.proteinGoal} g", C.OrangeText, "Protein goal")
            }

            Card {
                MenuRow(
                    "Appearance", C.YellowSoft,
                    { if (ThemeState.dark) FillIcon(Ic.MOON, Color(0xFF9AA4F0), 17.dp) else DayIcon(DayPart.DAY) },
                    value = themeLabel(state.themeMode).replace("Same as phone", "System"), onClick = onAppearance
                )
                MenuRow(
                    "Notifications", C.YellowSoft, { LineIcon(Ic.BELL, C.YellowText, 17.dp, 2.2f) },
                    value = notifySummary(state), onClick = onNotifications
                )
                MenuRow("History", C.GreenSoft, { LineIcon(Ic.BARS, C.GreenText, 17.dp, 3f) }, onClick = onHistory)
                MenuRow("Reset today's readings", C.OrangeSoft, { LineIcon(Ic.RESET, C.OrangeText, 17.dp, 2.4f) }) { showReset = true }
                MenuRow("Report a bug", C.RedSoft, { LineIcon(Ic.BUG, C.Red, 17.dp, 2f) }) { showBug = true }
                MenuRow("Refer a friend", C.PinkSoft, { LineIcon(Ic.GIFT, C.PinkText, 17.dp, 2f) }, showDivider = false) { showShare = true }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(C.RedSoft, RoundedCornerShape(14.dp))
                    .border(2.dp, C.RedLine, RoundedCornerShape(14.dp))
                    .clickable { showLogout = true }
                    .padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                LineIcon(Ic.LOGOUT, C.Red, 18.dp)
                Spacer(Modifier.size(8.dp))
                Text("Log out", style = T.bold(15, C.Red))
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(14.dp)) {
                    inViewBox(0f, 0f, 24f, 24f) {
                        drawPath(svg(Ic.GEM), Color(0xFF2F6BDB))
                        drawPath(svg(Ic.GEM_LINES), Color(0xFF9CC0FF), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f))
                    }
                }
                Spacer(Modifier.size(6.dp))
                Text("BY SAPPHIRE", style = T.label(C.Muted).copy(letterSpacing = 3.2.sp))
            }
            if (version.isNotBlank()) {
                Text("Version $version", style = T.body(11, C.Muted.copy(alpha = 0.75f)))
            }
        }
    }

    if (showBug) BugDialog(onClose = { showBug = false })

    if (showEdit) {
        EditProfileDialog(
            state = state,
            onDismiss = { showEdit = false },
            onSave = { name, avatar ->
                update { it.copy(name = name, avatar = avatar) }
                showEdit = false
            }
        )
    }

    if (showReset) {
        MessageDialog(
            title = "Reset today's readings?",
            body = "This clears the ${fmt(state.waterTotal)} ml of water and ${state.proteinTotal} g of protein logged today. Your steps, earlier days and your streaks stay as they are.",
            confirmLabel = "Reset today",
            confirmFace = C.Orange, confirmEdge = C.OrangeDark,
            onConfirm = {
                update { it.resetToday() }
                showReset = false
            },
            onDismiss = { showReset = false },
            icon = { IconSquare(C.OrangeSoft) { LineIcon(Ic.RESET, C.OrangeText, 22.dp, 2.4f) } }
        )
    }

    if (showShare) {
        MessageDialog(
            title = "Share Sip & Scoop",
            body = "Send the app to a friend. They get the install file and can set it up in a minute.",
            confirmLabel = "Share the app",
            confirmFace = C.Green, confirmEdge = C.GreenDark,
            onConfirm = {
                showShare = false
                shareApp(context)
            },
            onDismiss = { showShare = false },
            icon = { MascotPair(Modifier.size(84.dp, 76.dp), dripColor = C.Blue, celebrate = true) },
            extra = {
                Text(
                    "I'm tracking my water and protein with Sip & Scoop. Try it with me!",
                    style = T.body(14),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(C.Faint, RoundedCornerShape(14.dp, 14.dp, 14.dp, 4.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        )
    }

    if (showLogout) {
        MessageDialog(
            title = "Log out?",
            body = "This removes your name, goals and every logged reading from this phone. You'll start again from setup.",
            confirmLabel = "Log out",
            confirmFace = C.RedFace, confirmEdge = C.RedEdge,
            onConfirm = {
                showLogout = false
                update { AppState(themeMode = it.themeMode) }
            },
            onDismiss = { showLogout = false },
            icon = { IconSquare(C.RedSoft) { LineIcon(Ic.LOGOUT, C.Red, 22.dp) } }
        )
    }
}

@Composable
private fun StreakBar(modifier: Modifier, bg: Color, icon: @Composable () -> Unit, text: String, color: Color) {
    Row(
        modifier
            .background(bg, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
    ) {
        icon()
        Text(text, style = T.bold(13, color), maxLines = 1)
    }
}

@Composable
private fun StatTile(modifier: Modifier, bg: Color, icon: @Composable () -> Unit, value: String, valueColor: Color, label: String) {
    // Icon, value and title share one centre line so the tile is perfectly symmetric.
    Column(
        modifier
            .background(bg, RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) { icon() }
        Text(value, style = T.bold(16, valueColor), maxLines = 1, textAlign = TextAlign.Center)
        Text(label, style = T.body(12, C.Muted), maxLines = 1, textAlign = TextAlign.Center)
    }
}

// ---------------------------------------------------------------- settings

private fun themeLabel(mode: String) = when (mode) {
    "light" -> "Light"
    "dark" -> "Dark"
    else -> "Same as phone"
}

private fun notifySummary(s: AppState): String {
    val on = listOf(s.remindWater, s.remindProtein, s.streakAlert, s.remindSteps).count { it }
    return when (on) {
        0 -> "All off"
        4 -> "All on"
        else -> "$on of 4 on"
    }
}

// ---------------------------------------------------------------- appearance

@Composable
fun AppearanceScreen(
    state: AppState,
    update: ((AppState) -> AppState) -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TopBar("Appearance", onBack)
        Text("Choose how Sip & Scoop looks.", style = T.body(15, C.Muted))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("light", "dark", "system").forEach { mode ->
                ThemeCard(mode, state.themeMode == mode, Modifier.weight(1f)) { update { it.copy(themeMode = mode) } }
            }
        }
        NoteBox(
            "\"Same as phone\" follows your phone's dark mode setting. Home-screen widgets always follow the phone.",
            warning = false
        )

        SectionLabel("Tracker view")
        Column(
            Modifier
                .fillMaxWidth()
                .background(C.Surface, RoundedCornerShape(20.dp))
                .border(2.dp, C.Line, RoundedCornerShape(20.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val rings = state.ringsView
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ViewCard("Classic", "Glass, bar and ring", !rings, Modifier.weight(1f), { ClassicPreview() }) {
                    update { it.copy(ringsView = false) }
                }
                ViewCard("Rings", "All three at a glance", rings, Modifier.weight(1f), { RingsPreview() }) {
                    update { it.copy(ringsView = true) }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val classicColor by animateColorAsState(if (!rings) C.Ink else C.Muted, tween(250), label = "classicLbl")
                val ringsColor by animateColorAsState(if (rings) C.Ink else C.Muted, tween(250), label = "ringsLbl")
                Text("Classic", style = T.bold(15, classicColor))
                ViewSwitch(rings) { on -> update { it.copy(ringsView = on) } }
                Text("Rings", style = T.bold(15, ringsColor))
            }
            AnimatedContent(targetState = rings, label = "viewNote") { r ->
                Text(
                    if (r) "One set of rings for steps, water and protein. Swipe and the colours follow the ring you are on."
                    else "Each tracker has its own picture: a ring for steps, a glass for water and a bar for protein.",
                    style = T.body(13, C.Muted),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        SectionLabel("App icon")
        AppIconPicker()
    }
}

/** A big, easy switch: knob on the left = Classic, on the right = Rings. */
@Composable
private fun ViewSwitch(on: Boolean, onChange: (Boolean) -> Unit) {
    val track by animateColorAsState(if (on) C.Blue else C.Line, tween(250), label = "viewTrack")
    val knob by animateDpAsState(if (on) 31.dp else 3.dp, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium), label = "viewKnob")
    Box(
        Modifier
            .size(64.dp, 36.dp)
            .background(track, CircleShape)
            .toggleable(value = on, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = onChange)
            .semantics { contentDescription = "Rings view" }
    ) {
        Box(
            Modifier
                .offset(x = knob, y = 4.dp)
                .size(28.dp)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
private fun ViewCard(
    title: String,
    sub: String,
    selected: Boolean,
    modifier: Modifier,
    preview: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val border by animateColorAsState(if (selected) C.Blue else C.Line, tween(220), label = "viewBorder")
    val bg by animateColorAsState(if (selected) C.BlueCard else C.Surface, tween(220), label = "viewBg")
    Column(
        modifier
            .background(bg, RoundedCornerShape(16.dp))
            .border(2.dp, border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.size(96.dp, 70.dp)) { preview() }
        Text(title, style = T.bold(14, if (selected) C.Ink else C.Muted))
        Text(sub, style = T.body(11, C.Muted), maxLines = 1)
    }
}

@Composable
private fun ClassicPreview() {
    val glass = C.pickColor(0xFFF1F8FD, 0xFF152536)
    val glassLine = C.pickColor(0xFFB9D3E6, 0xFF35546B)
    val oCard = C.OrangeSoft
    val gCard = C.GoldSoft
    Canvas(Modifier.fillMaxSize()) {
        inViewBox(0f, 0f, 96f, 70f) {
            val cup = svg("M10 8 H40 L36 60 H14 Z")
            drawPath(cup, glass)
            drawPath(svg("M11.5 30 C18 27 24 33 30 30 S38 27 39 30 L36.6 58.5 H13.4 Z"), C.Blue)
            drawPath(cup, glassLine, style = Stroke(2.4f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            drawRoundRect(oCard, Offset(50f, 14f), Size(38f, 16f), androidx.compose.ui.geometry.CornerRadius(6f))
            drawRoundRect(C.Orange, Offset(54f, 19f), Size(22f, 6f), androidx.compose.ui.geometry.CornerRadius(3f))
            drawRoundRect(gCard, Offset(50f, 38f), Size(38f, 16f), androidx.compose.ui.geometry.CornerRadius(6f))
            drawRoundRect(C.Gold, Offset(54f, 43f), Size(28f, 6f), androidx.compose.ui.geometry.CornerRadius(3f))
        }
    }
}

@Composable
private fun RingsPreview() {
    val tracks = listOf(C.GoldTrack, C.RingTrackWater, C.RingTrackProtein)
    val colors = listOf(C.Gold, C.Blue, C.Orange)
    val radii = listOf(30f, 21f, 12f)
    val amounts = listOf(0.78f, 0.58f, 0.70f)
    Canvas(Modifier.fillMaxSize()) {
        inViewBox(0f, 0f, 96f, 70f) {
            for (i in 0..2) {
                val r = radii[i]
                val tl = Offset(48f - r, 35f - r)
                val sz = Size(r * 2, r * 2)
                drawArc(tracks[i], 0f, 360f, false, tl, sz, style = Stroke(7f))
                drawArc(colors[i], -90f, 360f * amounts[i], false, tl, sz, style = Stroke(7f, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun ThemeCard(mode: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val border by animateColorAsState(if (selected) C.Blue else C.Line, tween(220), label = "themeBorder")
    val scale by animateFloatAsState(if (selected) 1f else 0.97f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium), label = "themeScale")
    Column(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(C.Surface, RoundedCornerShape(18.dp))
            .border(if (selected) 3.dp else 2.dp, border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(Modifier.fillMaxWidth().height(110.dp)) {
            fun phone(bg: Color, card: Color, line: Color, left: Float, right: Float) {
                val r = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())
                clipRect(left, 0f, right, size.height) {
                    drawRoundRect(bg, size = size, cornerRadius = r)
                    val pad = 8.dp.toPx()
                    drawRoundRect(line, topLeft = Offset(pad, pad), size = androidx.compose.ui.geometry.Size(size.width * 0.5f, 6.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                    drawRoundRect(card, topLeft = Offset(pad, pad + 14.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width - pad * 2, 34.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
                    drawRoundRect(C.Blue, topLeft = Offset(pad, size.height - pad - 20.dp.toPx()), size = androidx.compose.ui.geometry.Size((size.width - pad * 3) / 2, 20.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
                    drawRoundRect(C.Orange, topLeft = Offset(pad * 2 + (size.width - pad * 3) / 2, size.height - pad - 20.dp.toPx()), size = androidx.compose.ui.geometry.Size((size.width - pad * 3) / 2, 20.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
                }
            }
            val light = Triple(Color(0xFFFFFFFF), Color(0xFFEAF6FE), Color(0xFFD6E2EC))
            val dark = Triple(Color(0xFF0F1923), Color(0xFF142B3E), Color(0xFF2C3D4D))
            when (mode) {
                "light" -> phone(light.first, light.second, light.third, 0f, size.width)
                "dark" -> phone(dark.first, dark.second, dark.third, 0f, size.width)
                else -> {
                    phone(light.first, light.second, light.third, 0f, size.width / 2f)
                    phone(dark.first, dark.second, dark.third, size.width / 2f, size.width)
                }
            }
            drawRoundRect(Color(0x33000000), size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .size(18.dp)
                    .border(2.dp, if (selected) C.Blue else C.Line, CircleShape)
                    .padding(4.dp)
                    .background(if (selected) C.Blue else Color.Transparent, CircleShape)
            )
            Text(themeLabel(mode).replace("Same as phone", "System"), style = T.bold(14, if (selected) C.Ink else C.Muted))
        }
    }
}

// ---------------------------------------------------------------- notifications

@Composable
fun NotificationsScreen(
    state: AppState,
    update: ((AppState) -> AppState) -> Unit,
    onBack: () -> Unit,
    onNeedNotificationPermission: () -> Unit
) {
    val quietPresets = listOf(21 to 7, 22 to 7, 23 to 8, 0 to 0)

    fun setNotify(change: (AppState) -> AppState, turnedOn: Boolean) {
        update(change)
        if (turnedOn) onNeedNotificationPermission()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TopBar("Notifications", onBack)
        Text("Gentle reminders, only when they help.", style = T.body(15, C.Muted))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Water")
            Card {
                SettingRow("Water reminders", "A nudge when you're behind") {
                    Toggle(state.remindWater, "Water reminders") { on -> setNotify({ it.copy(remindWater = on) }, on) }
                }
                AnimatedVisibility(visible = state.remindWater, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Remind me every", style = T.body(14, C.Muted), modifier = Modifier.weight(1f))
                        listOf(1, 2, 3).forEach { h ->
                            val sel = state.remindEveryHours == h
                            val bg by animateColorAsState(if (sel) C.Blue else C.Faint, tween(200), label = "hours")
                            SquishButton(
                                onClick = { update { it.copy(remindEveryHours = h) } },
                                background = bg,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) { Text("$h h", style = T.bold(13, if (sel) Color.White else C.Muted)) }
                        }
                    }
                }
                Divider()
                SettingRow("Quiet hours", "No reminders during this time. Tap to change.") {
                    val label = if (state.quietStart == state.quietEnd) "Off"
                    else String.format(Locale.US, "%02d:00 – %02d:00", state.quietStart, state.quietEnd)
                    SquishButton(
                        onClick = {
                            val i = quietPresets.indexOf(state.quietStart to state.quietEnd)
                            val next = quietPresets[(i + 1) % quietPresets.size]
                            update { it.copy(quietStart = next.first, quietEnd = next.second) }
                        },
                        background = C.Faint,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) { Text(label, style = T.bold(14)) }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Protein and diet streak")
            Card {
                SettingRow("Protein check-in", "Every day at 20:00 if you're short") {
                    Toggle(state.remindProtein, "Protein check-in") { on -> setNotify({ it.copy(remindProtein = on) }, on) }
                }
                Divider()
                SettingRow("Diet streak alert", "At 21:00 if water or protein isn't done") {
                    Toggle(state.streakAlert, "Diet streak alert") { on -> setNotify({ it.copy(streakAlert = on) }, on) }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Steps")
            Card {
                SettingRow("Step reminder", "At 19:00 if you're short of your step goal") {
                    Toggle(state.remindSteps, "Step reminder") { on -> setNotify({ it.copy(remindSteps = on) }, on) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- report a bug

@Composable
fun BugDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }
    MessageDialog(
        title = "Report a bug",
        body = "Tell us what happened and when.",
        confirmLabel = "Email us",
        confirmFace = C.Blue, confirmEdge = C.BlueEdge,
        onConfirm = {
            val opened = emailSupport(context)
            if (!opened) {
                clipboard.setText(AnnotatedString(SUPPORT_EMAIL))
                copied = true
            }
            if (opened) onClose()
        },
        onDismiss = onClose,
        icon = { IconSquare(C.RedSoft) { LineIcon(Ic.BUG, C.Red, 22.dp, 2f) } },
        extra = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SquishButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(SUPPORT_EMAIL))
                        copied = true
                    },
                    background = C.BlueSoft,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) { Text(SUPPORT_EMAIL, style = T.bold(15, C.BlueDark)) }
                AnimatedContent(
                    targetState = copied,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(150)) },
                    label = "copied"
                ) { c ->
                    if (c) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            LineIcon(Ic.CHECK, C.GreenText, 14.dp, 3.5f)
                            Text("Copied", style = T.bold(13, C.GreenText))
                        }
                    } else {
                        Text("Tap the email to copy it", style = T.body(13, C.Muted))
                    }
                }
            }
        }
    )
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(2.dp).background(C.NavLine))
}

@Composable
private fun SettingRow(title: String, subtitle: String?, showDivider: Boolean = false, trailing: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.bold(16))
            if (subtitle != null) Text(subtitle, style = T.body(13, C.Muted))
        }
        trailing()
    }
    if (showDivider) Divider()
}

// ---------------------------------------------------------------- history

private data class DayView(
    val date: LocalDate,
    val water: Int,
    val protein: Int,
    val waterGoal: Int,
    val proteinGoal: Int,
    val met: Boolean,
    val known: Boolean,
    val today: Boolean
)

@Composable
fun HistoryScreen(state: AppState, onBack: () -> Unit) {
    var month by remember { mutableStateOf(false) }
    val todayDate = runCatching { LocalDate.parse(state.date) }.getOrElse { LocalDate.now() }
    val count = if (month) 30 else 7
    val days = (count - 1 downTo 0).map { back ->
        val d = todayDate.minusDays(back.toLong())
        if (back == 0) {
            DayView(d, state.waterTotal, state.proteinTotal, state.waterGoal, state.proteinGoal, state.todayMet, true, true)
        } else {
            val r: DayRecord? = state.history[d.toString()]
            if (r == null) DayView(d, 0, 0, state.waterGoal, state.proteinGoal, false, false, false)
            else DayView(
                d, r.water, r.protein,
                if (r.waterGoal > 0) r.waterGoal else state.waterGoal,
                if (r.proteinGoal > 0) r.proteinGoal else state.proteinGoal,
                r.met, true, false
            )
        }
    }
    val withData = days.filter { it.known && (it.water > 0 || it.protein > 0) }
    val bothHit = days.count { it.met }
    val avgWater = if (withData.isEmpty()) 0 else withData.sumOf { it.water } / withData.size
    val avgProtein = if (withData.isEmpty()) 0 else withData.sumOf { it.protein } / withData.size
    val chartColorBlue = C.Blue
    val chartColorOrange = C.Orange
    val goalLine = C.BlueLine

    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TopBar("History", onBack) {
            Row(Modifier.background(C.Faint, RoundedCornerShape(12.dp)).padding(4.dp)) {
                listOf(false to "Week", true to "Month").forEach { (isMonth, label) ->
                    val sel = month == isMonth
                    Text(
                        label,
                        style = T.bold(13, if (sel) C.Ink else C.Muted),
                        modifier = Modifier
                            .background(if (sel) C.Surface else Color.Transparent, RoundedCornerShape(9.dp))
                            .clickable { month = isMonth }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(C.Surface, RoundedCornerShape(20.dp))
                .border(2.dp, C.Line, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${prettyDate(days.first().date.toString(), "d MMM")} – ${prettyDate(days.last().date.toString(), "d MMM")}",
                    style = T.bold(15), modifier = Modifier.weight(1f)
                )
                Legend(C.Blue, "Water")
                Spacer(Modifier.size(12.dp))
                Legend(C.Orange, "Protein")
            }
            Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                val h = size.height
                val n = days.size
                val group = size.width / n
                val barW = if (month) group * 0.32f else group * 0.26f
                val scale = h / 1.2f
                val goalY = h - scale
                drawLine(
                    goalLine, Offset(0f, goalY), Offset(size.width, goalY), strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
                days.forEachIndexed { i, d ->
                    val cx = group * i + group / 2f
                    val wr = (d.water.toFloat() / maxOf(1, d.waterGoal)).coerceAtMost(1.2f)
                    val pr = (d.protein.toFloat() / maxOf(1, d.proteinGoal)).coerceAtMost(1.2f)
                    val alpha = if (d.today) 0.55f else 1f
                    if (wr > 0f) drawRoundRect(
                        chartColorBlue, topLeft = Offset(cx - barW - 1.5f, h - wr * scale), size = Size(barW, wr * scale),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2.5f), alpha = alpha
                    )
                    if (pr > 0f) drawRoundRect(
                        chartColorOrange, topLeft = Offset(cx + 1.5f, h - pr * scale), size = Size(barW, pr * scale),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2.5f), alpha = alpha
                    )
                }
            }
            Row(Modifier.fillMaxWidth()) {
                if (month) {
                    Text(prettyDate(days.first().date.toString(), "d MMM"), style = T.bold(12, C.Muted), modifier = Modifier.weight(1f))
                    Text(prettyDate(days[days.size / 2].date.toString(), "d MMM"), style = T.bold(12, C.Muted), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("Today", style = T.bold(12, C.Muted), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                } else {
                    days.forEach { d ->
                        Text(
                            prettyDate(d.date.toString(), "EEE"),
                            style = T.bold(12, C.Muted),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile(Modifier.weight(1f), C.GreenSoft, "$bothHit / ${days.size}", C.GreenText, "Days both goals hit")
            SummaryTile(Modifier.weight(1f), C.BlueCard, liters(avgWater), C.BlueDark, "Average water")
            SummaryTile(Modifier.weight(1f), C.OrangeCard, "$avgProtein g", C.OrangeText, "Average protein")
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Day by day")
            days.reversed().forEach { d ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(C.Surface, RoundedCornerShape(16.dp))
                        .border(2.dp, C.Line, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            (if (d.today) "Today · " else "") + prettyDate(d.date.toString(), "EEE d MMM"),
                            style = T.bold(15)
                        )
                        val daySteps = if (d.today) state.steps else state.history[d.date.toString()]?.steps ?: 0
                        if (d.known && (d.water > 0 || d.protein > 0 || d.today || daySteps > 0)) {
                            Row {
                                Text("${fmt(d.water)} ml", style = T.body(13, C.BlueDark))
                                Text(" · ", style = T.body(13, C.Muted))
                                Text("${d.protein} g", style = T.body(13, C.OrangeText))
                                if (daySteps > 0) {
                                    Text(" · ", style = T.body(13, C.Muted))
                                    Text("${fmt(daySteps)} steps", style = T.body(13, C.GoldText))
                                }
                            }
                        } else if (!d.known) {
                            Text("No data", style = T.body(13, C.Muted))
                        }
                    }
                    when {
                        d.met -> StatusPill("Both goals", C.GreenSoft, C.GreenText)
                        d.today -> StatusPill("In progress", C.BlueSoft, C.BlueDark)
                        d.known -> StatusPill("Missed", C.Faint, C.Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
        Text(label, style = T.bold(12, C.Muted))
    }
}

@Composable
private fun SummaryTile(modifier: Modifier, bg: Color, value: String, valueColor: Color, label: String) {
    Column(
        modifier
            .background(bg, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, style = T.display(21, valueColor), maxLines = 1)
        Text(label, style = T.body(12, C.Muted))
    }
}

@Composable
private fun StatusPill(text: String, bg: Color, fg: Color) {
    Text(
        text,
        style = T.bold(12, fg),
        modifier = Modifier
            .background(bg, CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

const val SUPPORT_EMAIL = "kshitijsingh8604@gmail.com"

/** Opens the phone's email app with a bug report ready to fill in. Returns false if there is none. */
fun emailSupport(context: android.content.Context): Boolean {
    val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    val body = "Hi,\n\nWhat happened:\n\nWhat I was doing:\n\n---\nApp version: $version\n" +
        "Phone: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}, Android ${android.os.Build.VERSION.RELEASE}"
    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
        data = android.net.Uri.parse("mailto:")
        putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
        putExtra(android.content.Intent.EXTRA_SUBJECT, "Sip & Scoop bug report")
        putExtra(android.content.Intent.EXTRA_TEXT, body)
    }
    return try {
        context.startActivity(intent)
        true
    } catch (e: android.content.ActivityNotFoundException) {
        false
    }
}

// ---------------------------------------------------------------- edit profile

/** Opens from a tap on the profile picture: pick a character and change the name in one place. */
@Composable
fun EditProfileDialog(state: AppState, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf(state.name) }
    var pick by remember { mutableStateOf(state.avatar) }
    val ok = name.isNotBlank()
    Popup(onDismiss = onDismiss) {
        Box(
            Modifier
                .size(84.dp)
                .background(C.BlueSoft, CircleShape)
                .border(3.dp, C.Blue, CircleShape)
                .padding(6.dp)
        ) {
            AnimatedContent(
                targetState = pick,
                transitionSpec = { (fadeIn(tween(180)) togetherWith fadeOut(tween(120))) },
                label = "avatar"
            ) { id -> AvatarImage(id, Modifier.fillMaxSize()) }
        }
        PopupTitle("Edit profile")
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Your name", style = T.bold(13, C.Muted))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                textStyle = T.bold(17),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = C.Blue, unfocusedBorderColor = C.Line, cursorColor = C.Blue
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Character", style = T.bold(13, C.Muted))
            AVATAR_IDS.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { id ->
                        val sel = id == pick
                        Box(Modifier.weight(1f)) {
                            SquishButton(
                                onClick = { pick = id },
                                background = if (sel) C.BlueSoft else C.Faint,
                                shape = RoundedCornerShape(16.dp),
                                borderColor = if (sel) C.Blue else C.Line,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) { AvatarImage(id, Modifier.size(54.dp).align(Alignment.Center)) }
                            if (sel) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-6).dp)
                                        .size(22.dp)
                                        .background(C.Blue, CircleShape)
                                        .border(2.dp, C.Surface, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) { LineIcon(Ic.CHECK, Color.White, 11.dp, 4f) }
                            }
                        }
                    }
                }
            }
        }
        PopupButton("Save", C.Blue, C.BlueEdge, enabled = ok) { if (ok) onSave(name.trim(), pick) }
    }
}
