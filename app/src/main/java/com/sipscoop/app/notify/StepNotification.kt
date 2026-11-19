package com.sipscoop.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sipscoop.app.MainActivity
import com.sipscoop.app.R
import com.sipscoop.app.data.AppState
import com.sipscoop.app.steps.StepSensor
import com.sipscoop.app.steps.StepStatus
import java.util.Locale

/**
 * A thin, silent "ongoing" notification with today's steps against the goal, like
 * "6,240 / 8,000 steps" with a progress bar. It never makes a sound or pops up.
 * It is refreshed whenever the app reads the step counter (live while the app is open,
 * about every 15 minutes while it is closed).
 */
object StepNotification {
    private const val CHANNEL = "steps_live"
    private const val ID = 4100

    private var lastShown = -1
    private var lastGoal = -1
    private var lastTime = 0L

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Steps in notification bar", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Today's steps, shown quietly"
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }
    }

    /** Shows, refreshes or removes the notification to match the settings. */
    fun update(context: Context, s: AppState, force: Boolean = false) {
        val app = context.applicationContext
        val wanted = s.onboarded && s.stepNotify && Reminders.canNotify(app) && StepSensor.status(app) == StepStatus.ON
        if (!wanted) {
            cancel(app)
            return
        }
        // While walking with the app open, refresh at most every few seconds.
        val now = System.currentTimeMillis()
        if (!force && s.steps == lastShown && s.stepGoal == lastGoal) return
        if (!force && s.stepGoal == lastGoal && now - lastTime < 4000 && !(s.stepsMet && lastShown < s.stepGoal)) return

        ensureChannel(app)
        val goal = maxOf(1, s.stepGoal)
        val pct = minOf(100, (s.steps.toLong() * 100 / goal).toInt())
        val open = PendingIntent.getActivity(
            app, 41,
            Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(app, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFF5B940.toInt())
            .setContentTitle("${fmt(s.steps)} / ${fmt(s.stepGoal)} steps")
            .setContentText(if (s.stepsMet) "Step goal done today" else "$pct% of today's goal")
            .setProgress(100, pct, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .build()
        try {
            NotificationManagerCompat.from(app).notify(ID, n)
            lastShown = s.steps
            lastGoal = s.stepGoal
            lastTime = now
        } catch (e: SecurityException) {
            // permission was taken away; nothing to show
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID)
        lastShown = -1
        lastGoal = -1
    }

    private fun fmt(n: Int) = String.format(Locale.US, "%,d", n)
}
