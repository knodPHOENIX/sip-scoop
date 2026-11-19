package com.sipscoop.app.steps

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * The phone's step counter never resets; it just keeps counting. So the app must note the
 * count at midnight itself: today's steps = count now - count at midnight.
 *
 * This reads the counter at 23:55 (to finish the day) and at 00:05 (to start the new one),
 * every night, even while the phone sleeps. The few steps in between are split by time.
 * The two readings are 10 minutes apart because Android allows these "wake while asleep"
 * alarms only about once every 9 minutes per app.
 */
object MidnightCheck {
    const val ACTION = "com.sipscoop.app.STEP_MIDNIGHT"
    private val BEFORE: LocalTime = LocalTime.of(23, 55)
    private val AFTER: LocalTime = LocalTime.of(0, 5)

    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 26,
        Intent(context, StepSnapshotReceiver::class.java).setAction(ACTION),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    /** The next 23:55 or 00:05, whichever comes first (at least a minute away). */
    fun next(now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        val today = LocalDate.now()
        return listOf(
            today.atTime(AFTER), today.atTime(BEFORE),
            today.plusDays(1).atTime(AFTER), today.plusDays(1).atTime(BEFORE)
        ).first { it.isAfter(now.plusMinutes(1)) }
    }

    fun schedule(context: Context, on: Boolean) {
        val app = context.applicationContext
        val am = app.getSystemService(AlarmManager::class.java) ?: return
        val pi = pending(app)
        am.cancel(pi)
        if (!on || !StepSensor.available(app)) return
        val at = next().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        runCatching {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                // Not allowed to be exact: Android still runs it close to the time, even asleep.
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        }.onFailure {
            runCatching { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi) }
        }
    }
}
