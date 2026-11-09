package com.sipscoop.app

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.provider.Settings
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
import com.sipscoop.app.data.withStepReading
import com.sipscoop.app.data.withinDailyLimits
import com.sipscoop.app.steps.StepSensor
import com.sipscoop.app.steps.StepStatus
import java.time.LocalDate
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

    private var stepStatus by mutableStateOf(StepStatus.NEED_PERMISSION)
    private var stepListener: SensorEventListener? = null
    private var lastStepSave = 0L

    private val stepPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            stepStatus = StepSensor.status(this)
            startSteps()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fonts.init(this)
        appState = Store.load(this)
        applyTheme(appState)
        Reminders.schedule(this, appState)
        stepStatus = StepSensor.status(this)
        setContent {
            SipTheme {
                AppRoot(
                    appState,
                    update = { change -> update(change) },
                    onNeedNotificationPermission = { askForNotifications() },
                    stepStatus = stepStatus,
                    onAskSteps = { askForSteps() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The widget may have logged something while the app was closed.
        appState = Store.load(this)
        applyTheme(appState)
        stepStatus = StepSensor.status(this)
        startSteps()
    }

    override fun onPause() {
        stopSteps()
        Store.save(this, appState)
        WidgetSync.refreshAll(this)
        super.onPause()
    }

    // ------------------------------------------------------------------ steps

    private fun startSteps() {
        if (stepListener != null || stepStatus != StepStatus.ON) return
        val sm = getSystemService(SensorManager::class.java) ?: return
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val total = event.values.firstOrNull()?.toLong() ?: return
                onStepReading(total)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_UI)) stepListener = l
    }

    private fun stopSteps() {
        stepListener?.let { getSystemService(SensorManager::class.java)?.unregisterListener(it) }
        stepListener = null
    }

    private fun onStepReading(total: Long) {
        if (appState.date != LocalDate.now().toString()) {
            // Midnight passed while the app was open: close yesterday first.
            Store.save(this, appState)
            appState = Store.load(this)
        }
        val before = appState
        val next = before.withStepReading(total)
        if (next === before) return
        appState = next
        // Saving on every step is wasteful; save every few seconds, or right away when the goal is hit.
        val now = System.currentTimeMillis()
        if (now - lastStepSave > 5000 || (next.stepsMet && !before.stepsMet)) {
            lastStepSave = now
            Store.save(this, next)
            WidgetSync.refreshAll(this)
        }
    }

    private fun askForSteps() {
        val prefs = getSharedPreferences("sip_scoop_flags", MODE_PRIVATE)
        val askedBefore = prefs.getBoolean("steps_asked", false)
        if (Build.VERSION.SDK_INT >= 29) {
            if (askedBefore && !shouldShowRequestPermissionRationale(Manifest.permission.ACTIVITY_RECOGNITION)) {
                // Android won't show the prompt again; open this app's settings instead.
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } else {
                prefs.edit().putBoolean("steps_asked", true).apply()
                stepPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        } else {
            stepStatus = StepSensor.status(this)
            startSteps()
        }
    }

    private fun update(change: (AppState) -> AppState) {
        val before = appState
        val next = change(before).withinDailyLimits()
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
