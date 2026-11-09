package com.sipscoop.app.steps

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.sipscoop.app.data.Store
import com.sipscoop.app.data.withStepReading
import com.sipscoop.app.widget.WidgetSync

/** What the Steps page can show right now. */
enum class StepStatus { ON, NEED_PERMISSION, NO_SENSOR }

/**
 * The phone's built-in step counter. It keeps counting in hardware even when the app is
 * closed and uses almost no battery; the app only reads the running total.
 */
object StepSensor {
    const val ACTION_SNAPSHOT = "com.sipscoop.app.STEP_SNAPSHOT"

    private fun sensor(context: Context): Sensor? =
        context.getSystemService(SensorManager::class.java)?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    fun available(context: Context): Boolean = sensor(context) != null

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 29 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    fun status(context: Context): StepStatus = when {
        !available(context) -> StepStatus.NO_SENSOR
        !hasPermission(context) -> StepStatus.NEED_PERMISSION
        else -> StepStatus.ON
    }

    /**
     * Listens until the sensor reports, then stops. [onDone] gets the raw total, or null if
     * the phone gave nothing within [timeoutMs]. Runs on the main thread.
     */
    fun readOnce(context: Context, timeoutMs: Long = 4000, onDone: (Long?) -> Unit) {
        val sm = context.getSystemService(SensorManager::class.java)
        val s = sm?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (sm == null || s == null || !hasPermission(context)) {
            onDone(null)
            return
        }
        val handler = Handler(Looper.getMainLooper())
        var finished = false
        lateinit var listener: SensorEventListener
        val finish = { value: Long? ->
            if (!finished) {
                finished = true
                sm.unregisterListener(listener)
                onDone(value)
            }
        }
        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                handler.post { finish(event.values.firstOrNull()?.toLong()) }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        val ok = runCatching { sm.registerListener(listener, s, SensorManager.SENSOR_DELAY_NORMAL, handler) }.getOrDefault(false)
        if (!ok) {
            finish(null)
            return
        }
        handler.postDelayed({ finish(null) }, timeoutMs)
    }

    /** Reads the counter in the background and saves today's steps. */
    fun snapshot(context: Context, then: () -> Unit = {}) {
        val app = context.applicationContext
        readOnce(app) { total ->
            if (total != null) {
                Store.update(app) { it.withStepReading(total) }
                WidgetSync.refreshAll(app)
            }
            then()
        }
    }
}

/** Fires about once an hour so steps (and the widget) stay fresh while the app is closed. */
class StepSnapshotReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != StepSensor.ACTION_SNAPSHOT) return
        val pending = goAsync()
        StepSensor.snapshot(context) { pending.finish() }
    }
}
