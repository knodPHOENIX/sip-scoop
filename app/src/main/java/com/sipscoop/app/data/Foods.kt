package com.sipscoop.app.data

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** One way to measure a food, e.g. "egg (50 g)" = 50 grams, or plain grams = 1. */
data class Portion(val label: String, val grams: Int, val step: Int, val start: Int) {
    val isPlainUnit: Boolean get() = grams == 1
}

/** Nutrition per 100 g (or per 100 ml when [ml] is true). */
data class FoodItem(
    val name: String,
    val protein: Double,
    val kcal: Int?,
    val carbs: Double?,
    val fat: Double?,
    val portions: List<Portion>,
    val aliases: List<String> = emptyList(),
    val estimate: Boolean = false,
    val ml: Boolean = false,
    val brand: String? = null,
    val online: Boolean = false
) {
    val subtitle: String
        get() = buildString {
            if (online) append(brand?.takeIf { it.isNotBlank() } ?: "Packaged product") else append("Built-in")
            if (estimate) append(" · estimate")
            append(if (ml) " · per 100 ml" else " · per 100 g")
        }

    fun proteinFor(grams: Int): Double = grams * protein / 100.0
}

private fun portionsFor(named: List<Pair<String, Int>>, ml: Boolean): List<Portion> {
    val unit = if (ml) "ml" else "grams"
    val plain = Portion(unit, 1, if (ml) 50 else 10, named.firstOrNull()?.second?.coerceAtLeast(10) ?: 100)
    val counted = named.map { (label, g) -> Portion(label, g, 1, 1) }
    return counted + plain
}

/** The built-in food list, read once from assets/foods.json. */
object FoodDb {
    @Volatile private var cache: List<FoodItem>? = null

    fun all(context: Context): List<FoodItem> {
        cache?.let { return it }
        val list = runCatching {
            val text = context.assets.open("foods.json").bufferedReader().use { it.readText() }
            val arr = JSONObject(text).getJSONArray("foods")
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val ml = o.optBoolean("ml")
                val u = o.optJSONArray("u")
                val named = if (u == null) emptyList() else (0 until u.length()).map { k ->
                    val pair = u.getJSONArray(k)
                    pair.getString(0) to pair.getInt(1)
                }
                val a = o.optJSONArray("a")
                FoodItem(
                    name = o.getString("n"),
                    protein = o.getDouble("p"),
                    kcal = o.optInt("k"),
                    carbs = o.optDouble("c"),
                    fat = o.optDouble("f"),
                    portions = portionsFor(named, ml),
                    aliases = if (a == null) emptyList() else (0 until a.length()).map { a.getString(it) },
                    estimate = o.optBoolean("e"),
                    ml = ml
                )
            }
        }.getOrDefault(emptyList())
        cache = list
        return list
    }

    fun find(context: Context, name: String): FoodItem? = all(context).firstOrNull { it.name == name }

    val POPULAR = listOf("Paneer", "Egg, boiled", "Dal, cooked", "Chicken breast, cooked", "Tofu, firm", "Whey protein")

    /** Best matches first: exact, then starts-with, then any word starting with it, then contains. */
    fun search(context: Context, query: String): List<FoodItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return POPULAR.mapNotNull { find(context, it) }
        fun score(text: String): Int {
            val t = text.lowercase()
            return when {
                t == q -> 0
                t.startsWith(q) -> 1
                t.split(' ', ',', '-', '(').any { it.startsWith(q) } -> 2
                t.contains(q) -> 3
                else -> 99
            }
        }
        return all(context)
            .map { f -> f to minOf(score(f.name), f.aliases.minOfOrNull { score(it) + 1 } ?: 99) }
            .filter { it.second < 99 }
            .sortedWith(compareBy({ it.second }, { it.first.name.length }))
            .map { it.first }
            .take(40)
    }
}

/** Looks up packaged products on Open Food Facts (only when the user presses Search). */
object OpenFoodFacts {
    private const val USER_AGENT = "SipAndScoop/1.3.1 (Android; https://github.com/knodPHOENIX/sip-scoop)"
    private const val MIN_GAP_MS = 7000L
    private var lastCall = 0L
    private val memo = HashMap<String, List<FoodItem>>()

    class TooSoon(val waitSeconds: Int) : Exception()

    /** Runs on a background thread. Throws on network trouble or when searching too often. */
    fun search(query: String): List<FoodItem> {
        val q = query.trim().lowercase()
        memo[q]?.let { return it }
        val now = System.currentTimeMillis()
        val gap = now - lastCall
        if (gap < MIN_GAP_MS) throw TooSoon(((MIN_GAP_MS - gap) / 1000 + 1).toInt())
        lastCall = now

        val url = URL(
            "https://world.openfoodfacts.org/cgi/search.pl?search_terms=" +
                URLEncoder.encode(q, "UTF-8") +
                "&search_simple=1&action=process&json=1&page_size=24" +
                "&fields=product_name,brands,nutriments,serving_quantity,product_quantity"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 15000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (conn.responseCode !in 200..299) throw IllegalStateException("HTTP ${conn.responseCode}")
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val products = JSONObject(text).optJSONArray("products")
            val out = ArrayList<FoodItem>()
            if (products != null) {
                for (i in 0 until products.length()) {
                    val p = products.optJSONObject(i) ?: continue
                    val name = p.optString("product_name").trim()
                    val n = p.optJSONObject("nutriments") ?: continue
                    if (name.isEmpty() || !n.has("proteins_100g")) continue
                    val protein = n.optDouble("proteins_100g")
                    if (protein.isNaN() || protein < 0 || protein > 100) continue
                    val named = ArrayList<Pair<String, Int>>()
                    val serving = p.optDouble("serving_quantity")
                    if (!serving.isNaN() && serving in 1.0..2000.0) named.add("serving (${serving.toInt()} g)" to serving.toInt())
                    val pack = p.optDouble("product_quantity")
                    if (!pack.isNaN() && pack in 1.0..5000.0 && pack.toInt() != serving.toInt()) named.add("pack (${pack.toInt()} g)" to pack.toInt())
                    val kcal = n.optDouble("energy-kcal_100g")
                    val carbs = n.optDouble("carbohydrates_100g")
                    val fat = n.optDouble("fat_100g")
                    out.add(
                        FoodItem(
                            name = name.take(60),
                            protein = protein,
                            kcal = if (kcal.isNaN()) null else kcal.toInt(),
                            carbs = if (carbs.isNaN()) null else carbs,
                            fat = if (fat.isNaN()) null else fat,
                            portions = portionsFor(named, false),
                            brand = p.optString("brands").split(",").firstOrNull()?.trim(),
                            online = true
                        )
                    )
                }
            }
            val result = out.distinctBy { it.name.lowercase() + "|" + it.brand }.take(15)
            memo[q] = result
            return result
        } finally {
            conn.disconnect()
        }
    }
}
