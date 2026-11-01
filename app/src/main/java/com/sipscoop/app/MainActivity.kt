package com.sipscoop.app

import android.Manifest
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sipscoop.app.data.AppState
import com.sipscoop.app.data.Store
import com.sipscoop.app.notify.Reminders
import com.sipscoop.app.ui.AppRoot
import com.sipscoop.app.ui.Fonts
import com.sipscoop.app.ui.SipTheme
import com.sipscoop.app.ui.ThemeState
import com.sipscoop.app.widget.WidgetSync

class MainActivity : ComponentActivity() {

    private var appState by mutableStateOf(AppState())
    private var barsStyledDark: Boolean? = null

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fonts.init(this)
        appState = Store.load(this)
        applyTheme(appState)
        Reminders.schedule(this, appState)
        setContent {
            SipTheme {
                AppRoot(
                    appState,
                    update = { change -> update(change) },
                    onNeedNotificationPermission = { askForNotifications() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The widget may have logged something while the app was closed.
        appState = Store.load(this)
        applyTheme(appState)
    }

    private fun update(change: (AppState) -> AppState) {
        val before = appState
        val next = change(before)
        appState = next
        if (!next.onboarded && before.onboarded) {
            // logged out: wipe everything stored, then keep only the fresh state
            Store.clear(this)
        }
        Store.save(this, next)
        applyTheme(next)
        WidgetSync.refreshAll(this)
        Reminders.schedule(this, next)
        if (next.onboarded && !before.onboarded) askForNotifications()
    }

    private fun applyTheme(s: AppState) {
        val systemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val dark = when (s.themeMode) {
            "dark" -> true
            "light" -> false
            else -> systemDark
        }
        ThemeState.dark = dark
        if (barsStyledDark != dark) {
            barsStyledDark = dark
            val style = if (dark) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        }
    }

    private fun askForNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(this)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
