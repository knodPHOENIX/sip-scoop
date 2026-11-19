package com.sipscoop.app.data

import android.content.ContentValues
import android.content.Context
import java.time.LocalDate
import java.time.LocalDateTime

/** One stretch of walking: minutes after midnight, so 7:10 AM is 430. */
data class Walk(val startMin: Int, val endMin: Int, val steps: Int) {
    val minutes: Int get() = maxOf(1, endMin - startMin)
}

/**
 * Keeps a log of when steps happened: steps per hour, and walks of 500+ steps in a row.
 * Every time today's step total is saved, [sync] gets the new steps since the last time,
 * puts them in the current hour and grows (or closes) the walk in progress.
 */
object StepLog {
    const val WALK_MIN_STEPS = 500
    /** No new steps for this long ends a walk. */
    private const val GAP_MIN = 6
    /** Roughly how many steps a minute of walking is. */
    private const val STEPS_PER_MIN = 100
    /** Longer than this between two checks and we can't tell when the steps happened. */
    private const val LONG_GAP_MIN = 30

    private const val PREFS = "step_log"

    fun sync(context: Context, state: AppState, now: LocalDateTime = LocalDateTime.now()) = runCatching {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val db = SipDatabase.get(context).writableDatabase
        val today = state.date
        val nowMin = now.hour * 60 + now.minute
        val sameDay = now.toLocalDate().toString() == today

        // Close a walk left open on an earlier day.
        val walkDate = prefs.getString("walk_date", null)
        if (walkDate != null && walkDate != today) {
            closeWalk(context, db)
        }

        val loggedToday = prefs.getString("logged_date", null) == today
        var logged = if (loggedToday) prefs.getInt("logged", 0) else 0
        // When the last steps were logged (minutes after midnight), -1 if not yet today.
        val loggedMin = if (loggedToday) prefs.getInt("logged_min", -1) else -1
        if (state.steps < logged) logged = state.steps // a reset or a correction
        val delta = state.steps - logged
        val edit = prefs.edit().putString("logged_date", today).putInt("logged", state.steps)
        if (sameDay) edit.putInt("logged_min", nowMin)

        if (delta > 0 && sameDay) {
            // How long these steps took at a normal walking pace.
            val took = (delta + STEPS_PER_MIN - 1) / STEPS_PER_MIN
            // The app may not have been able to check for a while: then we don't know when
            // the steps happened, so they are spread over that time instead of one hour.
            val from = if (loggedMin in 0..nowMin) loggedMin else maxOf(0, nowMin - took)
            addToHours(db, today, from, nowMin, delta)
            val gap = nowMin - from
            if (loggedMin < 0 || gap > LONG_GAP_MIN) {
                // Timing unknown: don't invent a walk from these steps.
                edit.apply()
                if (prefs.getString("walk_date", null) == today) closeWalk(context, db)
                return@runCatching
            }
            val open = prefs.getString("walk_date", null) == today
            val lastMin = prefs.getInt("walk_last", -1)
            if (open && lastMin >= 0 && nowMin - lastMin - took <= GAP_MIN) {
                edit.putInt("walk_last", nowMin).putInt("walk_steps", prefs.getInt("walk_steps", 0) + delta)
            } else {
                edit.apply()
                if (open) closeWalk(context, db)
                val start = maxOf(0, nowMin - took)
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString("walk_date", today).putInt("walk_start", start)
                    .putInt("walk_last", nowMin).putInt("walk_steps", delta).apply()
                return@runCatching
            }
        } else if (prefs.getString("walk_date", null) == today) {
            val lastMin = prefs.getInt("walk_last", -1)
            if (lastMin >= 0 && nowMin - lastMin > GAP_MIN) {
                edit.apply()
                closeWalk(context, db)
                return@runCatching
            }
        }
        edit.apply()
    }

    /** Adds [steps] to the hours between [fromMin] and [toMin], in proportion to the minutes in each. */
    private fun addToHours(db: android.database.sqlite.SQLiteDatabase, date: String, fromMin: Int, toMin: Int, steps: Int) {
        val shares = IntArray(24)
        val span = toMin - fromMin
        if (span <= 0) {
            shares[(toMin / 60).coerceIn(0, 23)] = steps
        } else {
            var given = 0
            for (h in (fromMin / 60)..(toMin / 60).coerceAtMost(23)) {
                val minutes = minOf(toMin, (h + 1) * 60) - maxOf(fromMin, h * 60)
                if (minutes <= 0) continue
                val part = (steps.toLong() * minutes / span).toInt()
                shares[h] += part
                given += part
            }
            shares[(toMin / 60).coerceIn(0, 23)] += steps - given // rounding leftovers
        }
        for (h in 0..23) {
            if (shares[h] <= 0) continue
            // Works on every Android version (no "upsert" before Android 11).
            db.execSQL("INSERT OR IGNORE INTO step_hour (date, hour, steps) VALUES (?, ?, 0)", arrayOf<Any>(date, h))
            db.execSQL("UPDATE step_hour SET steps = steps + ? WHERE date = ? AND hour = ?", arrayOf<Any>(shares[h], date, h))
        }
    }

    /** Saves the walk in progress if it was long enough, and forgets it. */
    private fun closeWalk(context: Context, db: android.database.sqlite.SQLiteDatabase) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val date = prefs.getString("walk_date", null) ?: return
        val steps = prefs.getInt("walk_steps", 0)
        if (steps >= WALK_MIN_STEPS) {
            db.insert("walk", null, ContentValues().apply {
                put("date", date)
                put("start_min", prefs.getInt("walk_start", 0))
                put("end_min", prefs.getInt("walk_last", 0))
                put("steps", steps)
            })
        }
        prefs.edit().remove("walk_date").remove("walk_start").remove("walk_last").remove("walk_steps").apply()
    }

    /** Steps in each of the 24 hours of [date]. */
    fun hours(context: Context, date: String): IntArray {
        val out = IntArray(24)
        runCatching {
            SipDatabase.get(context).readableDatabase
                .query("step_hour", arrayOf("hour", "steps"), "date = ?", arrayOf(date), null, null, null)
                .use { c -> while (c.moveToNext()) out[c.getInt(0).coerceIn(0, 23)] = c.getInt(1) }
        }
        return out
    }

    /** Walks of [date], earliest first, including one still going on today. */
    fun walks(context: Context, date: String): List<Walk> {
        val list = mutableListOf<Walk>()
        runCatching {
            SipDatabase.get(context).readableDatabase
                .query("walk", arrayOf("start_min", "end_min", "steps"), "date = ?", arrayOf(date), null, null, "start_min")
                .use { c -> while (c.moveToNext()) list += Walk(c.getInt(0), c.getInt(1), c.getInt(2)) }
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString("walk_date", null) == date) {
            val steps = prefs.getInt("walk_steps", 0)
            if (steps >= WALK_MIN_STEPS) list += Walk(prefs.getInt("walk_start", 0), prefs.getInt("walk_last", 0), steps)
        }
        return list.sortedBy { it.startMin }
    }

    /** Removes everything (log out). */
    fun clear(context: Context) {
        runCatching {
            val db = SipDatabase.get(context).writableDatabase
            db.delete("step_hour", null, null)
            db.delete("walk", null, null)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    /** Old days beyond the kept history are dropped. */
    fun trim(context: Context, oldest: String) {
        runCatching {
            val db = SipDatabase.get(context).writableDatabase
            db.delete("step_hour", "date < ?", arrayOf(oldest))
            db.delete("walk", "date < ?", arrayOf(oldest))
        }
    }

    @Suppress("unused")
    private fun todayString() = LocalDate.now().toString()
}
