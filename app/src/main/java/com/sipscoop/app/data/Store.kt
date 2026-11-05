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
    val met: Boolean = false
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
    val streakAlert: Boolean = false
) {
    val waterTotal: Int get() = water.sumOf { it.amount }
    val proteinTotal: Int get() = protein.sumOf { it.amount }
    val todayMet: Boolean get() = waterTotal >= waterGoal && proteinTotal >= proteinGoal

    /** Days in a row with both goals reached, counting today once it is reached. */
    val streak: Int
        get() {
            var count = if (todayMet) 1 else 0
            var day = runCatching { LocalDate.parse(date) }.getOrElse { LocalDate.now() }.minusDays(1)
            while (history[day.toString()]?.met == true) {
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
            met = state.todayMet
        )
        val history = (state.history + (state.date to record))
            .toSortedMap()
            .entries
            .toList()
            .takeLast(400)
            .associate { it.key to it.value }
        return state.copy(date = today, water = emptyList(), protein = emptyList(), history = history)
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
                    DayRecord(rec.optInt("w"), rec.optInt("p"), rec.optInt("wg"), rec.optInt("pg"), rec.optBoolean("m"))
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
            streakAlert = o.optBoolean("streakAlert", def.streakAlert)
        )
    }
}
