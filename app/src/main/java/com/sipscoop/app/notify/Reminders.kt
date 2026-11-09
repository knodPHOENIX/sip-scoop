package com.sipscoop.app.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.sipscoop.app.MainActivity
import com.sipscoop.app.R
import com.sipscoop.app.data.AppState
import com.sipscoop.app.data.Store
import com.sipscoop.app.steps.StepSensor
import com.sipscoop.app.steps.StepSnapshotReceiver
import java.io.File
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

private const val CHANNEL = "reminders"
private const val ACTION_WATER = "com.sipscoop.app.REMIND_WATER"
private const val ACTION_PROTEIN = "com.sipscoop.app.REMIND_PROTEIN"
private const val ACTION_STREAK = "com.sipscoop.app.REMIND_STREAK"
private const val ACTION_STEPS = "com.sipscoop.app.REMIND_STEPS"

/** Sets up (or clears) the repeating reminders to match the current settings. */
object Reminders {

    fun schedule(context: Context, s: AppState) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val water = pending(context, ACTION_WATER, 21)
        val protein = pending(context, ACTION_PROTEIN, 22)
        val streak = pending(context, ACTION_STREAK, 23)
        am.cancel(water)
        am.cancel(protein)
        am.cancel(streak)
        val steps = pending(context, ACTION_STEPS, 24)
        am.cancel(steps)
        val snapshot = PendingIntent.getBroadcast(
            context, 25,
            Intent(context, StepSnapshotReceiver::class.java).setAction(StepSensor.ACTION_SNAPSHOT),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        am.cancel(snapshot)
        if (!s.onboarded) return

        if (StepSensor.available(context)) {
            // Read the step counter about once an hour so steps stay right across midnight.
            am.setInexactRepeating(
                AlarmManager.RTC, System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR,
                AlarmManager.INTERVAL_HOUR, snapshot
            )
            if (s.remindSteps) {
                am.setInexactRepeating(AlarmManager.RTC_WAKEUP, nextTime(19), AlarmManager.INTERVAL_DAY, steps)
            }
        }

        val now = System.currentTimeMillis()
        if (s.remindWater) {
            val every = s.remindEveryHours.coerceIn(1, 3) * AlarmManager.INTERVAL_HOUR
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, now + every, every, water)
        }
        if (s.remindProtein) {
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, nextTime(20), AlarmManager.INTERVAL_DAY, protein)
        }
        if (s.streakAlert) {
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, nextTime(21), AlarmManager.INTERVAL_DAY, streak)
        }
    }

    private fun nextTime(hour: Int): Long {
        var t = LocalDateTime.now().withHour(hour).withMinute(0).withSecond(0).withNano(0)
        if (!t.isAfter(LocalDateTime.now())) t = t.plusDays(1)
        return t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun pending(context: Context, action: String, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, code,
            Intent(context, ReminderReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Water, protein, steps and streak reminders"
                }
            )
        }
    }

    fun show(context: Context, id: Int, title: String, text: String) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, 30,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF1E9BF0.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (e: SecurityException) {
            // permission was taken away; nothing to show
        }
    }

    fun inQuietHours(s: AppState, hour: Int): Boolean {
        if (s.quietStart == s.quietEnd) return false
        return if (s.quietStart > s.quietEnd) hour >= s.quietStart || hour < s.quietEnd
        else hour >= s.quietStart && hour < s.quietEnd
    }
}

/** Fires on each reminder and decides whether there's anything worth saying. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val s = Store.load(context)
        if (!s.onboarded) return
        val hour = LocalTime.now().hour
        when (intent.action) {
            ACTION_WATER -> {
                if (!s.remindWater || Reminders.inQuietHours(s, hour)) return
                if (s.waterTotal >= s.waterGoal) return
                // expected progress between 07:00 and 22:00
                val dayFraction = ((hour - 7).coerceIn(0, 15)) / 15f
                val expected = (s.waterGoal * dayFraction).toInt()
                if (s.waterTotal >= expected && hour < 21) return
                val left = s.waterGoal - s.waterTotal
                Reminders.show(
                    context, 1, "Time for a glass of water",
                    String.format(Locale.US, "You're at %,d of %,d ml. %,d ml to go!", s.waterTotal, s.waterGoal, left)
                )
            }
            ACTION_PROTEIN -> {
                if (!s.remindProtein || s.proteinTotal >= s.proteinGoal) return
                Reminders.show(
                    context, 2, "Protein check-in",
                    "${s.proteinTotal} g so far. ${s.proteinGoal - s.proteinTotal} g left to hit your goal."
                )
            }
            ACTION_STREAK -> {
                if (!s.streakAlert || s.todayMet) return
                val streak = s.streak
                Reminders.show(
                    context, 3,
                    if (streak > 0) "Your $streak-day diet streak is at risk" else "Still time to hit today's goals",
                    "Finish your water and protein before midnight."
                )
            }
            ACTION_STEPS -> {
                if (!s.remindSteps) return
                val pending = goAsync()
                // Read the latest count first so the message is right.
                StepSensor.snapshot(context) {
                    val now = Store.load(context)
                    if (now.steps < now.stepGoal) {
                        val left = now.stepGoal - now.steps
                        Reminders.show(
                            context, 4, "Time for a quick walk?",
                            String.format(Locale.US, "You're %,d steps short of %,d. A short walk will do it!", left, now.stepGoal)
                        )
                    }
                    pending.finish()
                }
            }
        }
    }
}

/** Puts the reminders back after the phone restarts. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Reminders.schedule(context, Store.load(context))
        }
    }
}

/** Shares this app's own install file so a friend can install it. */
fun shareApp(context: Context) {
    try {
        val source = File(context.applicationInfo.sourceDir)
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val apk = File(dir, "SipAndScoop.apk")
        source.copyTo(apk, overwrite = true)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "I'm tracking my water and protein with Sip & Scoop. Try it with me!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, "Share Sip & Scoop").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        val text = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "I'm tracking my water and protein with Sip & Scoop. Try it with me!")
        }
        context.startActivity(Intent.createChooser(text, "Share Sip & Scoop").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
