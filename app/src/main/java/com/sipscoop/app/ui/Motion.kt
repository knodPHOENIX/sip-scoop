package com.sipscoop.app.ui

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.delay

/**
 * Small touches of feel. They use Android's own feedback types, so they follow the phone's
 * "touch feedback" setting (off there = silent here).
 */
object Haptics {
    /** A very light tick: every button tap. */
    fun tap(view: View) { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }

    /** A tiny tick for each step of a slider. */
    fun tick(view: View) { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }

    /** A firmer bump: long presses. */
    fun long(view: View) { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }

    /** Two quick firm pulses: a ring reached its goal. */
    suspend fun celebrate(view: View) {
        if (android.os.Build.VERSION.SDK_INT >= 30) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        else view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        delay(140)
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}

/** Where something sits on the phone screen (works across the app and its pop-up windows). */
fun LayoutCoordinates.boundsOnScreen(view: View): Rect {
    val screen = IntArray(2).also { view.getLocationOnScreen(it) }
    val window = IntArray(2).also { view.getLocationInWindow(it) }
    return boundsInWindow().translate((screen[0] - window[0]).toFloat(), (screen[1] - window[1]).toFloat())
}

/** Remembers where a button is, so a pop-up it opens can grow out of it. */
class OriginHolder {
    var rect: Rect = Rect.Zero
}

fun Modifier.trackOrigin(holder: OriginHolder): Modifier = composed {
    val view = LocalView.current
    onGloballyPositioned { holder.rect = it.boundsOnScreen(view) }
}

/** The last thing tapped. A pop-up opened right after grows out of it and shrinks back into it. */
object PopupOrigin {
    private var rect: Rect? = null
    private var at = 0L

    fun mark(r: Rect) {
        if (r.width > 0f && r.height > 0f) { rect = r; at = System.currentTimeMillis() }
    }

    /** The tapped spot, if it was tapped just now. */
    fun recent(): Rect? = rect?.takeIf { System.currentTimeMillis() - at < 1200 }
}

/** Taps get a light tick and remember where they happened. */
@Composable
fun rememberTap(onClick: () -> Unit): Pair<OriginHolder, () -> Unit> {
    val view = LocalView.current
    val holder = remember { OriginHolder() }
    return holder to {
        PopupOrigin.mark(holder.rect)
        Haptics.tap(view)
        onClick()
    }
}

// ---------------------------------------------------------------- Profile reveal

/** Where the profile picture is (root coordinates); Profile opens as a circle from there. */
object ProfileOrigin {
    val rect = mutableStateOf(Rect.Zero)
}

fun Modifier.profileOrigin(): Modifier = onGloballyPositioned { ProfileOrigin.rect.value = it.boundsInRoot() }

/** A circle of [radius] around [centre] (local coordinates): the Profile reveal. */
class CircleReveal(private val centre: Offset, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(Path().apply { addOval(Rect(centre, radius.coerceAtLeast(0f))) })
}
