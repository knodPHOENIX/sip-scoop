package com.sipscoop.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class Entry(val id: Long, val time: String, val amount: Int)

data class Food(val name: String, val grams: Int)

val DEFAULT_FOODS = listOf(
    Food("Boiled egg", 6),
    Food("Paneer 100 g", 18),
    Food("Dal, 1 bowl", 9),
    Food("Whey scoop", 24)
)

data class AppState(
    val onboarded: Boolean = false,
    val name: String = "",
    val age: Int = 22,
    val waterGoal: Int = 3000,
    val proteinGoal: Int = 100,
    val waterQuick: List<Int> = listOf(250, 500, 100),
    val proteinQuick: List<Int> = listOf(10, 20, 30),
    val foods: List<Food> = DEFAULT_FOODS,
    val date: String = LocalDate.now().toString(),
    val water: List<Entry> = emptyList(),
    val protein: List<Entry> = emptyList(),
    /** date -> true when both goals were reached that day */
    val history: Map<String, Boolean> = emptyMap()
) {
    val waterTotal: Int get() = water.sumOf { it.amount }
    val proteinTotal: Int get() = protein.sumOf { it.amount }
    val todayMet: Boolean get() = waterTotal >= waterGoal && proteinTotal >= proteinGoal

    /** Days in a row with both goals reached, counting today once it is reached. */
    val streak: Int
        get() {
            var count = if (todayMet) 1 else 0
            var day = runCatching { LocalDate.parse(date) }.getOrElse { LocalDate.now() }.minusDays(1)
            while (history[day.toString()] == true) {
                count++
                day = day.minusDays(1)
            }
            return count
        }

    val firstName: String
        get() = name.trim().split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: "friend"
}

fun AppState.addWater(ml: Int): AppState =
    if (ml <= 0) this else copy(water = water + Store.newEntry(ml))

fun AppState.addProtein(g: Int): AppState =
    if (g <= 0) this else copy(protein = protein + Store.newEntry(g))

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
        val rolled = rollOver(state)
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

    /** Starts a fresh day when the date has changed, keeping a record of the old one. */
    private fun rollOver(state: AppState): AppState {
        val today = LocalDate.now().toString()
        if (state.date == today) return state
        val history = (state.history + (state.date to state.todayMet))
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
        val hist = JSONObject().apply { s.history.forEach { (k, v) -> put(k, v) } }
        return JSONObject()
            .put("onboarded", s.onboarded)
            .put("name", s.name)
            .put("age", s.age)
            .put("waterGoal", s.waterGoal)
            .put("proteinGoal", s.proteinGoal)
            .put("waterQuick", intsToJson(s.waterQuick))
            .put("proteinQuick", intsToJson(s.proteinQuick))
            .put("foods", foods)
            .put("date", s.date)
            .put("water", entriesToJson(s.water))
            .put("protein", entriesToJson(s.protein))
            .put("history", hist)
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
        val history = mutableMapOf<String, Boolean>()
        if (histObj != null) {
            val keys = histObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                history[k] = histObj.optBoolean(k)
            }
        }
        return AppState(
            onboarded = o.optBoolean("onboarded", false),
            name = o.optString("name", ""),
            age = o.optInt("age", def.age),
            waterGoal = o.optInt("waterGoal", def.waterGoal),
            proteinGoal = o.optInt("proteinGoal", def.proteinGoal),
            waterQuick = intsFromJson(o.optJSONArray("waterQuick"), def.waterQuick),
            proteinQuick = intsFromJson(o.optJSONArray("proteinQuick"), def.proteinQuick),
            foods = foods,
            date = o.optString("date", def.date),
            water = entriesFromJson(o.optJSONArray("water")),
            protein = entriesFromJson(o.optJSONArray("protein")),
            history = history
        )
    }
}
