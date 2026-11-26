package com.sipscoop.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** The app version (versionCode) whose "What's new" pop-up is shown once after updating. */
const val WHATS_NEW_VERSION = 23

/** How long the pop-up stays before it can be closed, so it gets read. */
private const val READ_SECONDS = 5

/**
 * "What's new" in 2.1, shown once after updating. For the first 5 seconds it can't be
 * closed (a countdown runs in the corner); then the cross and "Keep tracking" appear.
 */
@Composable
fun WhatsNewDialog(onDone: () -> Unit) {
    var left by remember { mutableIntStateOf(READ_SECONDS) }
    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(1000)
            left--
        }
    }
    val ready = left == 0
    // The countdown ring empties smoothly over the 5 seconds.
    val ring by animateFloatAsState(
        if (ready) 0f else left.toFloat() / READ_SECONDS, tween(1000, easing = LinearEasing), label = "countdown"
    )

    Popup(
        onDismiss = onDone,
        centered = false,
        closable = ready,
        lockedCorner = {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                val track = C.Grey
                val ink = C.Ink
                Canvas(Modifier.size(32.dp)) {
                    val w = 3.dp.toPx()
                    val tl = androidx.compose.ui.geometry.Offset(w / 2, w / 2)
                    val sz = androidx.compose.ui.geometry.Size(size.width - w, size.height - w)
                    drawArc(track, 0f, 360f, false, tl, sz, style = Stroke(w))
                    drawArc(ink, -90f, 360f * ring, false, tl, sz, style = Stroke(w, cap = StrokeCap.Round))
                }
                Text("$left", style = T.bold(12, C.Muted))
            }
        }
    ) {
        Text("VERSION 2.1", style = T.label(C.BlueDark))
        Text("What's new", style = T.display(24))
        Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            NewItem(1, C.Gold, C.GoldText, "Smoother rings", "All three rings stay visible; the one you're on grows a little broader as you swipe.")
            NewItem(2, C.Blue, C.BlueDark, "Real numbers up front", "The chips under the rings show today's steps, water and protein instead of percentages.")
            NewItem(3, C.Orange, C.OrangeText, "New calorie tracker", "Now on the Protein page. Food from search adds its calories; tap + kcal to type your own.")
            NewItem(
                4, C.pickColor(0xFF8A5CF6, 0xFFB48CFF), C.pickColor(0xFF6D3FD6, 0xFFC9ABFF),
                "Detailed graph view", "Tap any graph to expand it: every day, week, month or year, with your averages and best days."
            )
        }
        Text(
            "Tip: hold the calorie bar to change your daily goal.",
            style = T.body(13, C.Muted),
            modifier = Modifier
                .fillMaxWidth()
                .glass(RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
        PopupButton(
            if (ready) "Keep tracking" else "Keep tracking · $left",
            C.Blue, C.BlueEdge, enabled = ready
        ) { if (ready) onDone() }
    }
}

@Composable
private fun NewItem(n: Int, color: Color, text: Color, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(28.dp)
                .glass(RoundedCornerShape(9.dp), tint = color),
            contentAlignment = Alignment.Center
        ) { Text("$n", style = T.bold(13, text)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = T.bold(15))
            Text(body, style = T.body(12, C.Muted))
        }
    }
}
