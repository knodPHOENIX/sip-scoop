package com.sipscoop.app.ui

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
import androidx.compose.material3.AlertDialog
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
    subtitle: String? = null,
    showDivider: Boolean = true,
    titleColor: Color = C.Ink,
    chevron: Boolean = true,
    onClick: () -> Unit
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconSquare(iconBg, icon)
            Column(Modifier.weight(1f)) {
                Text(title, style = T.bold(16, titleColor))
                if (subtitle != null) Text(subtitle, style = T.body(13, C.Muted))
            }
            if (chevron) LineIcon(Ic.CHEVRON, C.Muted, 18.dp, 2.5f)
        }
        if (showDivider) Box(Modifier.fillMaxWidth().height(2.dp).background(C.NavLine))
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

/** A plain pop-up with an icon, a message and two buttons. */
@Composable
fun MessageDialog(
    title: String,
    body: String,
    confirmLabel: String,
    confirmFace: Color,
    confirmEdge: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String? = "Cancel",
    icon: @Composable () -> Unit = {},
    extra: @Composable () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Surface,
        shape = RoundedCornerShape(24.dp),
        icon = icon,
        title = { Text(title, style = T.display(22), textAlign = TextAlign.Center) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(body, style = T.body(15, C.Muted), textAlign = TextAlign.Center)
                extra()
            }
        },
        confirmButton = {
            Btn3D(
                onClick = onConfirm, face = confirmFace, edge = confirmEdge, fill = false, depth = 4.dp,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
            ) { Text(confirmLabel, style = T.bold(15, Color.White)) }
        },
        dismissButton = {
            if (dismissLabel != null) {
                Btn3D(
                    onClick = onDismiss, face = C.Surface, edge = C.Line, fill = false, depth = 4.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) { Text(dismissLabel, style = T.bold(15, C.Muted)) }
            }
        }
    )
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
    onSettings: () -> Unit,
    onHistory: () -> Unit
) {
    val context = LocalContext.current
    var showName by remember { mutableStateOf(false) }
    var showPicture by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }
    var showLogout by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(C.Bg)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TopBar("Profile", onBack)

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(108.dp)) {
                    Box(
                        Modifier
                            .size(104.dp)
                            .border(4.dp, C.BlueSoft, CircleShape)
                            .padding(6.dp)
                    ) { AvatarImage(state.avatar, Modifier.fillMaxSize()) }
                    SquishButton(
                        onClick = { showPicture = true },
                        background = C.Blue,
                        modifier = Modifier.align(Alignment.BottomEnd),
                        borderColor = C.Bg,
                        contentPadding = PaddingValues(9.dp)
                    ) { LineIcon(Ic.PENCIL, Color.White, 16.dp, 2.4f) }
                }
                Text(state.name.ifBlank { "Friend" }, style = T.display(26))
                Text("${state.age} years · tracking since ${prettyDate(state.since, "d MMM")}", style = T.body(14, C.Muted))
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(Modifier.weight(1f), C.YellowSoft, { FillIcon(Ic.BOLT, C.Yellow, 20.dp) }, "${state.streak} ${if (state.streak == 1) "day" else "days"}", C.YellowText, "Streak")
                StatTile(Modifier.weight(1f), C.BlueCard, { FillIcon(Ic.DROP, C.Blue, 20.dp) }, liters(state.waterGoal), C.BlueDark, "Water goal")
                StatTile(Modifier.weight(1f), C.OrangeCard, { FillIcon(Ic.EGG, C.Orange, 20.dp) }, "${state.proteinGoal} g", C.OrangeText, "Protein goal")
            }

            Card {
                MenuRow("Change name", C.BlueSoft, { LineIcon(Ic.PENCIL, C.BlueDark) }) { showName = true }
                MenuRow("History", C.GreenSoft, { LineIcon(Ic.BARS, C.GreenText, 18.dp, 3f) }, onClick = onHistory)
                MenuRow("Reset today's readings", C.OrangeSoft, { LineIcon(Ic.RESET, C.OrangeText, 18.dp, 2.4f) }) { showReset = true }
                MenuRow("Settings", C.Faint, { LineIcon(Ic.SLIDERS, C.Muted) }, onClick = onSettings)
                MenuRow("Refer a friend", C.PinkSoft, { LineIcon(Ic.GIFT, C.PinkText, 18.dp, 2f) }, showDivider = false) { showShare = true }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .background(C.RedSoft, RoundedCornerShape(18.dp))
                    .border(2.dp, C.RedLine, RoundedCornerShape(18.dp))
            ) {
                MenuRow("Log out", C.Surface, { LineIcon(Ic.LOGOUT, C.Red) }, showDivider = false, titleColor = C.Red, chevron = false) { showLogout = true }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(Modifier.size(14.dp)) {
                inViewBox(0f, 0f, 24f, 24f) {
                    drawPath(svg(Ic.GEM), Color(0xFF2F6BDB))
                    drawPath(svg(Ic.GEM_LINES), Color(0xFF9CC0FF), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f))
                }
            }
            Spacer(Modifier.size(6.dp))
            Text("BY SAPPHIRE", style = T.label(C.Muted).copy(letterSpacing = 3.2.sp))
        }
    }

    if (showName) {
        var text by remember { mutableStateOf(state.name) }
        AlertDialog(
            onDismissRequest = { showName = false },
            containerColor = C.Surface,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Change name", style = T.display(22)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.take(30) },
                        singleLine = true,
                        label = { Text("Your name") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        textStyle = T.bold(18),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = C.Blue, unfocusedBorderColor = C.Line,
                            focusedLabelColor = C.BlueDark, unfocusedLabelColor = C.Muted, cursorColor = C.Blue
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Shown in your greeting at the top.", style = T.body(13, C.Muted))
                }
            },
            confirmButton = {
                val ok = text.isNotBlank()
                Btn3D(
                    onClick = {
                        if (ok) {
                            update { it.copy(name = text.trim()) }
                            showName = false
                        }
                    },
                    face = if (ok) C.Blue else C.Grey, edge = if (ok) C.BlueEdge else C.Line,
                    enabled = ok, fill = false, depth = 4.dp,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                ) { Text("Save", style = T.bold(15, if (ok) Color.White else C.GreyText)) }
            },
            dismissButton = {
                Btn3D(
                    onClick = { showName = false }, face = C.Surface, edge = C.Line, fill = false, depth = 4.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) { Text("Cancel", style = T.bold(15, C.Muted)) }
            }
        )
    }

    if (showPicture) {
        var pick by remember { mutableStateOf(state.avatar) }
        AlertDialog(
            onDismissRequest = { showPicture = false },
            containerColor = C.Surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AvatarImage(pick, Modifier.size(56.dp))
                    Column {
                        Text("Change picture", style = T.display(22))
                        Text("Tap a character to preview it.", style = T.body(13, C.Muted))
                    }
                }
            },
            text = { AvatarGrid(pick, { pick = it }, tile = 64) },
            confirmButton = {
                Btn3D(
                    onClick = {
                        update { it.copy(avatar = pick) }
                        showPicture = false
                    },
                    face = C.Blue, edge = C.BlueEdge, fill = false, depth = 4.dp,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                ) { Text("Use this picture", style = T.bold(15, Color.White)) }
            },
            dismissButton = {
                Btn3D(
                    onClick = { showPicture = false }, face = C.Surface, edge = C.Line, fill = false, depth = 4.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) { Text("Cancel", style = T.bold(15, C.Muted)) }
            }
        )
    }

    if (showReset) {
        MessageDialog(
            title = "Reset today's readings?",
            body = "This clears the ${fmt(state.waterTotal)} ml of water and ${state.proteinTotal} g of protein logged today. Earlier days and your streak stay as they are.",
            confirmLabel = "Reset today",
            confirmFace = C.Orange, confirmEdge = C.OrangeDark,
            onConfirm = {
                update { it.resetToday() }
                showReset = false
            },
            onDismiss = { showReset = false },
            dismissLabel = "Keep them",
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
private fun StatTile(modifier: Modifier, bg: Color, icon: @Composable () -> Unit, value: String, valueColor: Color, label: String) {
    Column(
        modifier
            .background(bg, RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        icon()
        Text(value, style = T.display(19, valueColor), maxLines = 1)
        Text(label, style = T.body(12, C.Muted), maxLines = 1)
    }
}

// ---------------------------------------------------------------- settings

@Composable
fun SettingsScreen(
    state: AppState,
    update: ((AppState) -> AppState) -> Unit,
    onBack: () -> Unit,
    onNeedNotificationPermission: () -> Unit
) {
    var showBug by remember { mutableStateOf(false) }
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
        TopBar("Settings", onBack)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Appearance")
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(C.Faint, RoundedCornerShape(16.dp))
                    .padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("light" to "Light", "dark" to "Dark", "system" to "System").forEach { (mode, label) ->
                    val sel = state.themeMode == mode
                    val bg by animateColorAsState(
                        if (sel) (if (ThemeState.dark) C.Line else C.Surface) else Color.Transparent,
                        tween(220), label = "seg"
                    )
                    Row(
                        Modifier
                            .weight(1f)
                            .background(bg, RoundedCornerShape(12.dp))
                            .clickable { update { it.copy(themeMode = mode) } }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (mode) {
                            "light" -> DayIcon(DayPart.DAY)
                            "dark" -> FillIcon(Ic.MOON, Color(0xFF7B86D9), 16.dp)
                            else -> LineIcon(Ic.PHONE, C.Muted, 16.dp, 2f)
                        }
                        Spacer(Modifier.size(6.dp))
                        Text(label, style = T.bold(14, if (sel) C.Ink else C.Muted))
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Notifications")
            Card {
                SettingRow("Water reminders", "A nudge when you're behind") {
                    Toggle(state.remindWater, "Water reminders") { on -> setNotify({ it.copy(remindWater = on) }, on) }
                }
                if (state.remindWater) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Remind me every", style = T.body(14, C.Muted), modifier = Modifier.weight(1f))
                        listOf(1, 2, 3).forEach { h ->
                            val sel = state.remindEveryHours == h
                            SquishButton(
                                onClick = { update { it.copy(remindEveryHours = h) } },
                                background = if (sel) C.Blue else C.Faint,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) { Text("$h h", style = T.bold(13, if (sel) Color.White else C.Muted)) }
                        }
                    }
                }
                Divider()
                SettingRow("Quiet hours", "Tap to change") {
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
                Divider()
                SettingRow("Protein check-in", "Every day at 20:00") {
                    Toggle(state.remindProtein, "Protein check-in") { on -> setNotify({ it.copy(remindProtein = on) }, on) }
                }
                Divider()
                SettingRow("Streak alert", "At 21:00 if today's goals aren't hit") {
                    Toggle(state.streakAlert, "Streak alert") { on -> setNotify({ it.copy(streakAlert = on) }, on) }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Help")
            Card {
                MenuRow("Report a bug", C.RedSoft, { LineIcon(Ic.BUG, C.Red, 18.dp, 2f) }, subtitle = "Write what happened and send it") { showBug = true }
                SettingRow("App version", null, showDivider = false) { Text("1.2.1", style = T.bold(14, C.Muted)) }
            }
        }
    }

    if (showBug) {
        MessageDialog(
            title = "Better luck next time",
            body = "We're still under development. We'll bring a contact soon.",
            confirmLabel = "Okay",
            confirmFace = C.Blue, confirmEdge = C.BlueEdge,
            onConfirm = { showBug = false },
            onDismiss = { showBug = false },
            dismissLabel = null,
            icon = { EggMascot(Modifier.size(64.dp, 76.dp)) },
            extra = {
                Text(
                    "Boys, don't break NNN",
                    style = T.bold(15, C.YellowText),
                    modifier = Modifier
                        .background(C.YellowSoft, CircleShape)
                        .border(2.dp, C.YellowLine, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        )
    }
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
                        if (d.known && (d.water > 0 || d.protein > 0 || d.today)) {
                            Row {
                                Text("${fmt(d.water)} ml", style = T.body(13, C.BlueDark))
                                Text(" · ", style = T.body(13, C.Muted))
                                Text("${d.protein} g", style = T.body(13, C.OrangeText))
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
