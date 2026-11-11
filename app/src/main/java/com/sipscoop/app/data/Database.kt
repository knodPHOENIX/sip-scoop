package com.sipscoop.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * The app's SQLite database.
 *
 *  - settings: profile, goals, quick amounts and preferences (one row per setting)
 *  - day:      one row per finished day (totals, goals, steps, whether goals were hit)
 *  - entry:    every single water or protein log, kept for each day
 *  - food:     the user's quick foods, in order
 */
class SipDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, NAME, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)")
        db.execSQL(
            """
            CREATE TABLE day (
                date TEXT PRIMARY KEY NOT NULL,
                water INTEGER NOT NULL DEFAULT 0,
                protein INTEGER NOT NULL DEFAULT 0,
                water_goal INTEGER NOT NULL DEFAULT 0,
                protein_goal INTEGER NOT NULL DEFAULT 0,
                met INTEGER NOT NULL DEFAULT 0,
                steps INTEGER NOT NULL DEFAULT 0,
                step_goal INTEGER NOT NULL DEFAULT 0,
                step_met INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE entry (
                id INTEGER PRIMARY KEY NOT NULL,
                date TEXT NOT NULL,
                kind TEXT NOT NULL,
                time TEXT NOT NULL,
                amount INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX entry_by_day ON entry (date, kind)")
        db.execSQL("CREATE TABLE food (position INTEGER PRIMARY KEY NOT NULL, name TEXT NOT NULL, grams INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 is the first database. Future versions add their changes here, step by step.
    }

    companion object {
        private const val NAME = "sip_scoop.db"
        private const val VERSION = 1

        @Volatile private var instance: SipDatabase? = null

        fun get(context: Context): SipDatabase =
            instance ?: synchronized(this) {
                instance ?: SipDatabase(context.applicationContext).also { instance = it }
            }
    }
}

/** Runs [block] inside one transaction, so a save is all-or-nothing. */
internal inline fun <T> SQLiteDatabase.inTransaction(block: SQLiteDatabase.() -> T): T {
    beginTransaction()
    try {
        val result = block()
        setTransactionSuccessful()
        return result
    } finally {
        endTransaction()
    }
}

internal fun dayValues(date: String, r: DayRecord) = ContentValues().apply {
    put("date", date)
    put("water", r.water)
    put("protein", r.protein)
    put("water_goal", r.waterGoal)
    put("protein_goal", r.proteinGoal)
    put("met", if (r.met) 1 else 0)
    put("steps", r.steps)
    put("step_goal", r.stepGoal)
    put("step_met", if (r.stepMet) 1 else 0)
}
