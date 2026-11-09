package com.sipscoop.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class Entry(val id: Long, val time: String, val amount: Int)

data class Food(val name: String, val grams: Int)

/** What happened on one finished day. Older saves only know [met]. */
data class DayRecord(
    val water: Int = 0,
    val protein: Int = 0,
    val waterGoal: Int = 0,
    val proteinGoal: Int = 0,
    /** Water and protein both reached (the diet streak). */
    val met: Boolean = false,
    val steps: Int = 0,
    val stepGoal: Int = 0,
    val stepMet: Boolean = false
)

val DEFAULT_FOODS = listOf(
    Food("Boiled egg", 6),
    Food("Paneer 100 g", 18),
    Food("Dal, 1 bowl", 9),
    Food("Whey scoop", 24)
)

val AVATAR_IDS = listOf("m1", "m2", "m3", "f1", "f2", "f3")

data class AppState(
    val onboarded: Boolean = false,
    val name: String = "",
    val age: Int = 22,
    val avatar: String = "m1",
    val since: String = LocalDate.now().toString(),
    val waterGoal: Int = 3000,
    val proteinGoal: Int = 100,
    val waterQuick: List<Int> = listOf(250, 500, 100),
    val proteinQuick: List<Int> = listOf(10, 20, 30),
    val foods: List<Food> = DEFAULT_FOODS,
    val date: String = LocalDate.now().toString(),
    val water: List<Entry> = emptyList(),
    val protein: List<Entry> = emptyList(),
    val history: Map<String, DayRecord> = emptyMap(),
    /** "light", "dark" or "system" */
    val themeMode: String = "system",
    val remindWater: Boolean = true,
    val remindEveryHours: Int = 2,
    val quietStart: Int = 22,
    val quietEnd: Int = 7,
    val remindProtein: Boolean = true,
    val streakAlert: Boolean = false,
    /** Built-in foods picked from search, newest first. */
    val recentFoods: List<String> = emptyList(),
    val stepGoal: Int = 8000,
    /** Steps counted today. */
    val steps: Int = 0,
    /** Phone step-counter value that today's count starts from (-1 = not known yet). */
    val stepBase: Long = -1,
    /** Steps counted earlier today before the phone restarted. */
    val stepPrior: Int = 0,
    /** Last raw value read from the phone's step counter. */
    val stepLast: Long = -1,
    val remindSteps: Boolean = true
) {
    val waterTotal: Int get() = water.sumOf { it.amount }
    val proteinTotal: Int get() = protein.sumOf { it.amount }
    val todayMet: Boolean get() = waterTotal >= waterGoal && proteinTotal >= proteinGoal
    val stepsMet: Boolean get() = steps >= stepGoal

    /** Diet streak: days in a row with water and protein both reached, counting today once it is. */
    val streak: Int get() = daysInARow(todayMet) { it.met }

    /** Step streak: days in a row with the step goal reached. */
    val stepStreak: Int get() = daysInARow(stepsMet) { it.stepMet }

    private fun daysInARow(today: Boolean, ok: (DayRecord) -> Boolean): Int {
        var count = if (today) 1 else 0
        var day = runCatching { LocalDate.parse(date) }.getOrElse { LocalDate.now() }.minusDays(1)
        while (history[day.toString()]?.let(ok) == true) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    val firstName: String
        get() = name.trim().split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: "friend"
}

/** The most anyone can log in one day. */
const val WATER_DAY_MAX = 10000
const val PROTEIN_DAY_MAX = 400

val AppState.waterRoom: Int get() = (WATER_DAY_MAX - waterTotal).coerceAtLeast(0)
val AppState.proteinRoom: Int get() = (PROTEIN_DAY_MAX - proteinTotal).coerceAtLeast(0)

/** Adds water, but never past the daily maximum (only the part that fits is logged). */
fun AppState.addWater(ml: Int): AppState {
    val add = minOf(ml, waterRoom)
    return if (add <= 0) this else copy(water = water + Store.newEntry(add))
}

fun AppState.addProtein(g: Int): AppState {
    val add = minOf(g, proteinRoom)
    return if (add <= 0) this else copy(protein = protein + Store.newEntry(add))
}

/** Trims a list of entries from the newest end so its total stays within [max]. */
fun capEntries(list: List<Entry>, max: Int): List<Entry> {
    var left = max
    val out = ArrayList<Entry>()
    for (e in list) {
        if (left <= 0) break
        val a = minOf(e.amount, left)
        if (a > 0) out.add(if (a == e.amount) e else e.copy(amount = a))
        left -= a
    }
    return out
}

/** Keeps today's totals within the daily maximums (fixes anything logged by older versions). */
fun AppState.withinDailyLimits(): AppState {
    val w = if (waterTotal > WATER_DAY_MAX) capEntries(water, WATER_DAY_MAX) else water
    val p = if (proteinTotal > PROTEIN_DAY_MAX) capEntries(protein, PROTEIN_DAY_MAX) else protein
    return if (w === water && p === protein) this else copy(water = w, protein = p)
}

fun AppState.resetToday(): AppState = copy(water = emptyList(), protein = emptyList())

const val STEP_GOAL_MAX = 50000
const val STEP_DAY_MAX = 100000

/**
 * Takes a fresh reading from the phone's step counter (steps since the phone last started)
 * and works out today's steps from it. Handles a new day and a phone restart.
 */
fun AppState.withStepReading(total: Long): AppState {
    if (total < 0) return this
    var base = stepBase
    var prior = stepPrior
    if (stepLast >= 0 && total < stepLast) {
        // The counter went backwards: the phone restarted. Keep what was counted so far.
        prior = steps
        base = 0
    } else if (base < 0) {
        // First reading of the day: start from the last value seen, so steps taken since
        // then (for example after midnight) are not lost.
        base = if (stepLast in 0..total) stepLast else total
        prior = 0
    }
    val today = (prior + (total - base)).toInt().coerceIn(0, STEP_DAY_MAX)
    return if (today == steps && base == stepBase && prior == stepPrior && total == stepLast) this
    else copy(steps = today, stepBase = base, stepPrior = prior, stepLast = total)
}

/** About 0.76 m per step and 0.04 kcal per step for an average adult. */
fun stepsKm(steps: Int): Double = steps * 0.00076
fun stepsKcal(steps: Int): Int = (steps * 0.04).toInt()

object Store {
    private const val PREFS = "sip_scoop"
    private const val KEY = "state"
    private var counter = 0L

    fun newEntry(amount: Int): Entry {
        counter++
        return Entry(System.currentTimeMillis() * 1000 + (counter % 1000), now(), amount)
    }

    fun now(): String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

    fun load(context: Context): AppState {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        val state = if (raw == null) AppState() else runCatching { fromJson(JSONObject(raw)) }.getOrElse { AppState() }
        val rolled = rollOver(state).withinDailyLimits()
        if (rolled !== state) save(context, rolled)
        return rolled
    }

    fun save(context: Context, state: AppState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, toJson(state).toString())
            .commit()
    }

    fun update(context: Context, change: (AppState) -> AppState): AppState {
        val next = change(load(context))
        save(context, next)
        return next
    }

    /** Removes everything saved on this phone. */
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }

    /** Starts a fresh day when the date has changed, keeping a record of the old one. */
    private fun rollOver(state: AppState): AppState {
        val today = LocalDate.now().toString()
        if (state.date == today) return state
        val record = DayRecord(
            water = state.waterTotal,
            protein = state.proteinTotal,
            waterGoal = state.waterGoal,
            proteinGoal = state.proteinGoal,
            met = state.todayMet,
            steps = state.steps,
            stepGoal = state.stepGoal,
            stepMet = state.stepsMet
        )
        val history = (state.history + (state.date to record))
            .toSortedMap()
            .entries
            .toList()
            .takeLast(400)
            .associate { it.key to it.value }
        return state.copy(
            date = today, water = emptyList(), protein = emptyList(), history = history,
            steps = 0, stepBase = -1, stepPrior = 0
        )
    }

    private fun entriesToJson(list: List<Entry>) = JSONArray().apply {
        list.forEach { put(JSONObject().put("id", it.id).put("t", it.time).put("a", it.amount)) }
    }

    private fun entriesFromJson(arr: JSONArray?): List<Entry> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Entry(o.optLong("id"), o.optString("t"), o.optInt("a"))
        }
    }

    private fun intsToJson(list: List<Int>) = JSONArray().apply { list.forEach { put(it) } }

    private fun intsFromJson(arr: JSONArray?, fallback: List<Int>): List<Int> {
        if (arr == null || arr.length() == 0) return fallback
        return (0 until arr.length()).map { arr.optInt(it) }
    }

    private fun toJson(s: AppState): JSONObject {
        val foods = JSONArray().apply {
            s.foods.forEach { put(JSONObject().put("n", it.name).put("g", it.grams)) }
        }
        val hist = JSONObject()
        s.history.forEach { (day, r) ->
            hist.put(
                day,
                JSONObject().put("w", r.water).put("p", r.protein)
                    .put("wg", r.waterGoal).put("pg", r.proteinGoal).put("m", r.met)
                    .put("s", r.steps).put("sg", r.stepGoal).put("sm", r.stepMet)
            )
        }
        return JSONObject()
            .put("onboarded", s.onboarded)
            .put("name", s.name)
            .put("age", s.age)
            .put("avatar", s.avatar)
            .put("since", s.since)
            .put("waterGoal", s.waterGoal)
            .put("proteinGoal", s.proteinGoal)
            .put("waterQuick", intsToJson(s.waterQuick))
            .put("proteinQuick", intsToJson(s.proteinQuick))
            .put("foods", foods)
            .put("date", s.date)
            .put("water", entriesToJson(s.water))
            .put("protein", entriesToJson(s.protein))
            .put("history", hist)
            .put("themeMode", s.themeMode)
            .put("remindWater", s.remindWater)
            .put("remindEveryHours", s.remindEveryHours)
            .put("quietStart", s.quietStart)
            .put("quietEnd", s.quietEnd)
            .put("remindProtein", s.remindProtein)
            .put("streakAlert", s.streakAlert)
            .put("recentFoods", JSONArray().apply { s.recentFoods.forEach { put(it) } })
            .put("stepGoal", s.stepGoal)
            .put("steps", s.steps)
            .put("stepBase", s.stepBase)
            .put("stepPrior", s.stepPrior)
            .put("stepLast", s.stepLast)
            .put("remindSteps", s.remindSteps)
    }

    private fun fromJson(o: JSONObject): AppState {
        val def = AppState()
        val foodsArr = o.optJSONArray("foods")
        val foods = if (foodsArr == null || foodsArr.length() == 0) DEFAULT_FOODS else
            (0 until foodsArr.length()).map {
                val f = foodsArr.getJSONObject(it)
                Food(f.optString("n"), f.optInt("g"))
            }
        val histObj = o.optJSONObject("history")
        val history = mutableMapOf<String, DayRecord>()
        if (histObj != null) {
            val keys = histObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val rec = histObj.optJSONObject(k)
                history[k] = if (rec != null) {
                    DayRecord(
                        rec.optInt("w"), rec.optInt("p"), rec.optInt("wg"), rec.optInt("pg"), rec.optBoolean("m"),
                        rec.optInt("s"), rec.optInt("sg"), rec.optBoolean("sm")
                    )
                } else {
                    // saves from version 1 only stored whether the day was a success
                    DayRecord(met = histObj.optBoolean(k))
                }
            }
        }
        val avatar = o.optString("avatar", def.avatar).takeIf { it in AVATAR_IDS } ?: def.avatar
        return AppState(
            onboarded = o.optBoolean("onboarded", false),
            name = o.optString("name", ""),
            age = o.optInt("age", def.age),
            avatar = avatar,
            since = o.optString("since", o.optString("date", def.since)),
            waterGoal = o.optInt("waterGoal", def.waterGoal).coerceIn(1, 10000),
            proteinGoal = o.optInt("proteinGoal", def.proteinGoal).coerceIn(1, 400),
            waterQuick = intsFromJson(o.optJSONArray("waterQuick"), def.waterQuick),
            proteinQuick = intsFromJson(o.optJSONArray("proteinQuick"), def.proteinQuick),
            foods = foods,
            date = o.optString("date", def.date),
            water = entriesFromJson(o.optJSONArray("water")),
            protein = entriesFromJson(o.optJSONArray("protein")),
            history = history,
            themeMode = o.optString("themeMode", def.themeMode),
            remindWater = o.optBoolean("remindWater", def.remindWater),
            remindEveryHours = o.optInt("remindEveryHours", def.remindEveryHours).coerceIn(1, 3),
            quietStart = o.optInt("quietStart", def.quietStart),
            quietEnd = o.optInt("quietEnd", def.quietEnd),
            remindProtein = o.optBoolean("remindProtein", def.remindProtein),
            streakAlert = o.optBoolean("streakAlert", def.streakAlert),
            recentFoods = o.optJSONArray("recentFoods")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
            stepGoal = o.optInt("stepGoal", def.stepGoal).coerceIn(500, STEP_GOAL_MAX),
            steps = o.optInt("steps", 0).coerceIn(0, STEP_DAY_MAX),
            stepBase = o.optLong("stepBase", -1),
            stepPrior = o.optInt("stepPrior", 0),
            stepLast = o.optLong("stepLast", -1),
            remindSteps = o.optBoolean("remindSteps", def.remindSteps)
        )
    }
}
