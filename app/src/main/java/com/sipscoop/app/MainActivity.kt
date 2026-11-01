package com.sipscoop.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sipscoop.app.data.AppState
import com.sipscoop.app.data.Store
import com.sipscoop.app.ui.AppRoot
import com.sipscoop.app.ui.Fonts
import com.sipscoop.app.ui.SipTheme
import com.sipscoop.app.widget.WidgetSync

class MainActivity : ComponentActivity() {

    private var appState by mutableStateOf(AppState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
        Fonts.init(this)
        appState = Store.load(this)
        setContent {
            SipTheme {
                AppRoot(appState) { change -> update(change) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The widget may have logged something while the app was closed.
        appState = Store.load(this)
    }

    private fun update(change: (AppState) -> AppState) {
        val next = change(appState)
        appState = next
        Store.save(this, next)
        WidgetSync.refreshAll(this)
    }
}
