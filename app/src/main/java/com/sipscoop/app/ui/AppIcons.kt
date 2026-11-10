package com.sipscoop.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sipscoop.app.R

/** The three app icons. Each one is a launcher entry (activity-alias) in the manifest. */
enum class AppIcon(val alias: String, val label: String, val bg: Int, val fg: Int) {
    BLUE("IconBlue", "Bright blue", R.drawable.ic_icon_blue_bg, R.drawable.ic_icon_drip_fg),
    NAVY("IconNavy", "Deep navy", R.drawable.ic_icon_navy_bg, R.drawable.ic_icon_drip_fg),
    WHITE("IconWhite", "Clean white", R.drawable.ic_icon_white_bg, R.drawable.ic_icon_drip_blue_fg)
}

object AppIconSwitcher {
    private fun component(context: Context, icon: AppIcon) =
        ComponentName(context.packageName, "com.sipscoop.app.${icon.alias}")

    /** The icon currently shown on the home screen. Bright blue is the default. */
    fun current(context: Context): AppIcon {
        val pm = context.packageManager
        return AppIcon.entries.firstOrNull { icon ->
            pm.getComponentEnabledSetting(component(context, icon)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } ?: AppIcon.BLUE
    }

    /** Switches the home-screen icon. The new one is turned on before the others are turned off. */
    fun apply(context: Context, icon: AppIcon) {
        val pm = context.packageManager
        runCatching {
            pm.setComponentEnabledSetting(
                component(context, icon),
                if (icon == AppIcon.BLUE) PackageManager.COMPONENT_ENABLED_STATE_DEFAULT else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            AppIcon.entries.filter { it != icon }.forEach {
                pm.setComponentEnabledSetting(component(context, it), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
        }
    }
}

/** How an icon looks on a home screen: the centre of the 108dp adaptive icon, in a rounded square. */
@Composable
fun AppIconPreview(icon: AppIcon, size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.23f)),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.requiredSize(size * 1.5f)) {
            Image(painterResource(icon.bg), contentDescription = null, modifier = Modifier.fillMaxSize())
            Image(painterResource(icon.fg), contentDescription = null, modifier = Modifier.fillMaxSize())
        }
    }
}

/** Appearance → App icon: tap one of the three to change the home-screen icon. */
@Composable
fun AppIconPicker() {
    val context = LocalContext.current
    var chosen by remember { mutableStateOf(AppIconSwitcher.current(context)) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppIcon.entries.forEach { icon ->
                val selected = icon == chosen
                val border by animateColorAsState(if (selected) C.Blue else C.Line, tween(220), label = "iconBorder")
                val scale by animateFloatAsState(
                    if (selected) 1f else 0.96f,
                    spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium), label = "iconScale"
                )
                Box(Modifier.weight(1f)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .background(if (selected) C.BlueCard else C.Surface, RoundedCornerShape(18.dp))
                            .border(if (selected) 3.dp else 2.dp, border, RoundedCornerShape(18.dp))
                            .clickable {
                                if (!selected) {
                                    chosen = icon
                                    AppIconSwitcher.apply(context, icon)
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppIconPreview(icon, 60.dp)
                        Text(
                            icon.label + if (icon == AppIcon.BLUE) "\n(default)" else "\n ",
                            style = T.bold(12, if (selected) C.Ink else C.Muted),
                            textAlign = TextAlign.Center
                        )
                    }
                    if (selected) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(22.dp)
                                .background(C.Blue, CircleShape)
                                .border(2.dp, C.Surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) { LineIcon(Ic.CHECK, Color.White, 11.dp, 4f) }
                    }
                }
            }
        }
        Text(
            "Your home screen may take a few seconds to show the new icon.",
            style = T.body(13, C.Muted)
        )
    }
}
