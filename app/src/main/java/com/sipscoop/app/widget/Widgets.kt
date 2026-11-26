package com.sipscoop.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.widget.RemoteViews
import com.sipscoop.app.MainActivity
import com.sipscoop.app.R
import com.sipscoop.app.data.AppState
import com.sipscoop.app.data.Store
import com.sipscoop.app.data.addProtein
import com.sipscoop.app.data.addWater
import com.sipscoop.app.data.stepsKcal
import com.sipscoop.app.data.stepsKm
import java.time.LocalDate
import java.util.Locale

const val ACTION_ADD_WATER = "com.sipscoop.app.ADD_WATER"
const val ACTION_ADD_PROTEIN = "com.sipscoop.app.ADD_PROTEIN"
/** How much a widget button adds; without it the first quick amount is used. */
const val EXTRA_AMOUNT = "amount"

/** Shared logic for every widget. */
abstract class BaseWidget(private val layout: Int) : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val state = Store.load(context)
        ids.forEach { manager.updateAppWidget(it, WidgetSync.build(context, layout, javaClass, state)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val amount = intent.getIntExtra(EXTRA_AMOUNT, -1)
        when (intent.action) {
            ACTION_ADD_WATER -> {
                Store.update(context) { it.addWater(if (amount > 0) amount else it.waterQuick.firstOrNull() ?: 250) }
                WidgetSync.refreshAll(context)
            }
            ACTION_ADD_PROTEIN -> {
                Store.update(context) { it.addProtein(if (amount > 0) amount else it.proteinQuick.firstOrNull() ?: 10) }
                WidgetSync.refreshAll(context)
            }
            else -> super.onReceive(context, intent)
        }
    }
}

/** Big rings (4x2): rings with steps below, water and protein bars, two buttons. */
class WideWidget : BaseWidget(R.layout.widget_wide)

/** Small bars (2x2): three rows and two buttons. */
class CompactWidget : BaseWidget(R.layout.widget_compact)

/** Overview (4x2): rings and three rows, information only. */
class OverviewWidget : BaseWidget(R.layout.widget_overview)

/** Three columns (4x2). */
class ColumnsWidget : BaseWidget(R.layout.widget_columns)

/** Strip (4x1). */
class StripWidget : BaseWidget(R.layout.widget_strip)

/** Large (4x3) with this week's steps. */
class LargeWidget : BaseWidget(R.layout.widget_large)

/** Small rings (2x2). */
class RingsSmallWidget : BaseWidget(R.layout.widget_rings_small)

/** Water only (2x2). */
class WaterWidget : BaseWidget(R.layout.widget_water)

/** Protein only (2x2). */
class ProteinWidget : BaseWidget(R.layout.widget_protein)

/** Steps only (2x2). */
class StepsWidget : BaseWidget(R.layout.widget_steps)

object WidgetSync {
    private val providers = listOf(
        WideWidget::class.java to R.layout.widget_wide,
        CompactWidget::class.java to R.layout.widget_compact,
        OverviewWidget::class.java to R.layout.widget_overview,
        ColumnsWidget::class.java to R.layout.widget_columns,
        StripWidget::class.java to R.layout.widget_strip,
        LargeWidget::class.java to R.layout.widget_large,
        RingsSmallWidget::class.java to R.layout.widget_rings_small,
        WaterWidget::class.java to R.layout.widget_water,
        ProteinWidget::class.java to R.layout.widget_protein,
        StepsWidget::class.java to R.layout.widget_steps
    )

    fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val state = Store.load(context)
        providers.forEach { (cls, layout) ->
            runCatching {
                val ids = manager.getAppWidgetIds(ComponentName(context, cls))
                ids.forEach { manager.updateAppWidget(it, build(context, layout, cls, state)) }
            }
        }
    }

    private fun fmt(n: Int) = String.format(Locale.US, "%,d", n)

    private fun liters(ml: Int): String {
        val s = if (ml % 1000 == 0) "${ml / 1000}" else
            String.format(Locale.US, "%.2f", ml / 1000.0).trimEnd('0').trimEnd('.')
        return "$s L"
    }

    /** "1.75" for 1,750 ml (the unit is shown next to the goal). */
    private fun litersNumber(ml: Int): String = liters(ml).removeSuffix(" L")

    private fun pct(value: Int, goal: Int) = if (goal > 0) minOf(100, (value.toLong() * 100 / goal).toInt()) else 0

    fun build(context: Context, layout: Int, cls: Class<*>, s: AppState): RemoteViews {
        val v = RemoteViews(context.packageName, layout)
        val stepPct = pct(s.steps, s.stepGoal)
        val waterPct = pct(s.waterTotal, s.waterGoal)
        val proteinPct = pct(s.proteinTotal, s.proteinGoal)
        val fractions = floatArrayOf(stepPct / 100f, waterPct / 100f, proteinPct / 100f)
        val water1 = s.waterQuick.firstOrNull() ?: 250
        val water2 = s.waterQuick.getOrNull(2) ?: 100
        val protein1 = s.proteinQuick.firstOrNull() ?: 10
        val protein2 = s.proteinQuick.getOrNull(1) ?: 20

        fun rows(steps: Boolean) {
            if (steps) {
                v.setTextViewText(R.id.steps_value, fmt(s.steps))
                v.setTextViewText(R.id.steps_goal, "/ ${fmt(s.stepGoal)}")
                v.setProgressBar(R.id.steps_bar, 100, stepPct, false)
            }
            v.setTextViewText(R.id.water_value, litersNumber(s.waterTotal))
            v.setTextViewText(R.id.water_goal, "/ ${liters(s.waterGoal)}")
            v.setProgressBar(R.id.water_bar, 100, waterPct, false)
            v.setTextViewText(R.id.protein_value, "${s.proteinTotal}")
            v.setTextViewText(R.id.protein_goal, "/ ${s.proteinGoal} g")
            v.setProgressBar(R.id.protein_bar, 100, proteinPct, false)
        }

        fun rings(id: Int, sizeDp: Int) = runCatching { v.setImageViewBitmap(id, ringsBitmap(context, sizeDp, fractions)) }

        fun ring(id: Int, sizeDp: Int, i: Int) = runCatching { v.setImageViewBitmap(id, ringBitmap(context, sizeDp, fractions[i], i)) }

        fun buttons(waterLabel: String, proteinLabel: String) {
            v.setTextViewText(R.id.water_add, waterLabel)
            v.setTextViewText(R.id.protein_add, proteinLabel)
            v.setOnClickPendingIntent(R.id.water_add, broadcast(context, cls, ACTION_ADD_WATER, 1, water1))
            v.setOnClickPendingIntent(R.id.protein_add, broadcast(context, cls, ACTION_ADD_PROTEIN, 2, protein1))
        }

        when (layout) {
            R.layout.widget_compact -> buildCompact(context, v, cls, s, stepPct, waterPct, proteinPct, water1, protein1)

            R.layout.widget_wide -> {
                rings(R.id.rings, 100)
                v.setTextViewText(R.id.steps_value, fmt(s.steps))
                rows(steps = false)
                buttons("+$water1 ml", "+$protein1 g")
            }

            R.layout.widget_overview -> {
                rings(R.id.rings, 96)
                rows(steps = true)
            }

            R.layout.widget_columns -> {
                ring(R.id.ring_steps, 60, 0); ring(R.id.ring_water, 60, 1); ring(R.id.ring_protein, 60, 2)
                v.setTextViewText(R.id.steps_pct, "$stepPct%")
                v.setTextViewText(R.id.water_pct, "$waterPct%")
                v.setTextViewText(R.id.protein_pct, "$proteinPct%")
                v.setTextViewText(R.id.steps_value, fmt(s.steps))
                v.setTextViewText(R.id.steps_goal, "of ${fmt(s.stepGoal)}")
                v.setTextViewText(R.id.water_value, liters(s.waterTotal))
                v.setTextViewText(R.id.water_goal, "of ${liters(s.waterGoal)}")
                v.setTextViewText(R.id.protein_value, "${s.proteinTotal} g")
                v.setTextViewText(R.id.protein_goal, "of ${s.proteinGoal} g")
                v.setTextViewText(R.id.steps_km, String.format(Locale.US, "%.1f km", stepsKm(s.steps)))
                buttons("+$water1", "+$protein1 g")
            }

            R.layout.widget_strip -> {
                ring(R.id.ring_steps, 28, 0); ring(R.id.ring_water, 28, 1); ring(R.id.ring_protein, 28, 2)
                v.setTextViewText(R.id.steps_value, fmt(s.steps))
                v.setTextViewText(R.id.water_value, liters(s.waterTotal))
                v.setTextViewText(R.id.protein_value, "${s.proteinTotal} g")
                buttons("+$water1", "+$protein1 g")
            }

            R.layout.widget_large -> {
                rings(R.id.rings, 100)
                rows(steps = true)
                buttons("+$water1 ml", "+$protein1 g")
                // This week's steps as bars, with the day letters under them.
                val today = runCatching { LocalDate.parse(s.date) }.getOrElse { LocalDate.now() }
                val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
                val values = days.map { d -> if (d == today) s.steps else s.history[d.toString()]?.steps ?: 0 }
                val hits = values.count { it > 0 && it >= s.stepGoal }
                runCatching { v.setImageViewBitmap(R.id.week_bars, weekBitmap(context, values, s.stepGoal)) }
                v.setTextViewText(R.id.week_note, "$hits of 7 days")
                val ids = listOf(R.id.day0, R.id.day1, R.id.day2, R.id.day3, R.id.day4, R.id.day5, R.id.day6)
                days.forEachIndexed { i, d ->
                    v.setTextViewText(ids[i], d.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.US))
                }
                v.setOnClickPendingIntent(R.id.open_app, openApp(context, 4))
            }

            R.layout.widget_rings_small -> {
                rings(R.id.rings, 110)
                v.setTextViewText(R.id.steps_value, fmt(s.steps))
                v.setTextViewText(R.id.water_value, liters(s.waterTotal))
                v.setTextViewText(R.id.protein_value, "${s.proteinTotal} g")
            }

            R.layout.widget_water -> {
                v.setTextViewText(R.id.water_value, litersNumber(s.waterTotal))
                v.setTextViewText(R.id.water_goal, "/ ${liters(s.waterGoal)}")
                v.setProgressBar(R.id.water_bar, 100, waterPct, false)
                val left = s.waterGoal - s.waterTotal
                v.setTextViewText(R.id.water_note, if (left > 0) "${liters(left)} to go" else "Goal done today")
                v.setTextViewText(R.id.water_add2, "+$water2")
                v.setTextViewText(R.id.water_add, "+$water1")
                v.setOnClickPendingIntent(R.id.water_add2, broadcast(context, cls, ACTION_ADD_WATER, 5, water2))
                v.setOnClickPendingIntent(R.id.water_add, broadcast(context, cls, ACTION_ADD_WATER, 1, water1))
            }

            R.layout.widget_protein -> {
                v.setTextViewText(R.id.protein_value, "${s.proteinTotal}")
                v.setTextViewText(R.id.protein_goal, "/ ${s.proteinGoal} g")
                v.setProgressBar(R.id.protein_bar, 100, proteinPct, false)
                val left = s.proteinGoal - s.proteinTotal
                val kcal = if (s.kcalTotal > 0) " · ${fmt(s.kcalTotal)} kcal" else ""
                v.setTextViewText(R.id.protein_note, (if (left > 0) "$left g to go" else "Goal done today") + kcal)
                v.setTextViewText(R.id.protein_add2, "+$protein1 g")
                v.setTextViewText(R.id.protein_add, "+$protein2 g")
                v.setOnClickPendingIntent(R.id.protein_add2, broadcast(context, cls, ACTION_ADD_PROTEIN, 6, protein1))
                v.setOnClickPendingIntent(R.id.protein_add, broadcast(context, cls, ACTION_ADD_PROTEIN, 2, protein2))
            }

            R.layout.widget_steps -> {
                v.setTextViewText(R.id.steps_value, fmt(s.steps))
                v.setTextViewText(R.id.steps_goal, "/ ${fmt(s.stepGoal)}")
                v.setProgressBar(R.id.steps_bar, 100, stepPct, false)
                val left = s.stepGoal - s.steps
                v.setTextViewText(R.id.steps_note, if (left > 0) "${fmt(left)} to go" else "Goal done today")
                v.setTextViewText(
                    R.id.steps_extra,
                    String.format(Locale.US, "%.1f km · %s kcal", stepsKm(s.steps), fmt(stepsKcal(s.steps)))
                )
            }
        }

        v.setOnClickPendingIntent(R.id.widget_root, openApp(context, 3))
        return v
    }

    /** The small "bars" widget, as before: rows, buttons, and a done message when both diet goals are hit. */
    private fun buildCompact(
        context: Context, views: RemoteViews, cls: Class<*>, s: AppState,
        stepPct: Int, waterPct: Int, proteinPct: Int, water1: Int, protein1: Int
    ) {
        views.setTextViewText(R.id.steps_value, fmt(s.steps))
        views.setTextViewText(R.id.steps_goal, "steps")
        views.setProgressBar(R.id.steps_bar, 100, stepPct, false)
        views.setTextViewText(R.id.water_value, litersNumber(s.waterTotal))
        views.setTextViewText(R.id.water_goal, "/ ${liters(s.waterGoal)}")
        views.setProgressBar(R.id.water_bar, 100, waterPct, false)
        views.setTextViewText(R.id.protein_value, "${s.proteinTotal}")
        views.setTextViewText(R.id.protein_goal, "/ ${s.proteinGoal} g")
        views.setProgressBar(R.id.protein_bar, 100, proteinPct, false)
        views.setTextViewText(R.id.water_add, "+$water1")
        views.setTextViewText(R.id.protein_add, "+$protein1 g")

        val done = s.onboarded && s.todayMet
        views.setViewVisibility(R.id.normal_group, if (done) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.done_group, if (done) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.done_streak, "${s.streak}-day diet streak")
        if (done) runCatching { views.setImageViewBitmap(R.id.done_rings, ringsBitmap(context, 64, floatArrayOf(1f, 1f, 1f))) }

        views.setOnClickPendingIntent(R.id.water_add, broadcast(context, cls, ACTION_ADD_WATER, 1, water1))
        views.setOnClickPendingIntent(R.id.protein_add, broadcast(context, cls, ACTION_ADD_PROTEIN, 2, protein1))
    }

    private fun openApp(context: Context, code: Int): PendingIntent = PendingIntent.getActivity(
        context, code,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun isDark(context: Context) =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private fun palette(context: Context): IntArray = if (isDark(context))
        intArrayOf(0xFFF5B940.toInt(), 0xFF4EA8FF.toInt(), 0xFFFF7A45.toInt())
    else intArrayOf(0xFFD9940E.toInt(), 0xFF2F80ED.toInt(), 0xFFEF6428.toInt())

    private fun trackColor(context: Context) = if (isDark(context)) 0x1FFFFFFF else 0x140F1318

    /** Three rounded rings (steps, water, protein) drawn into a small picture. */
    private fun ringsBitmap(context: Context, sizeDp: Int, f: FloatArray): Bitmap {
        val density = context.resources.displayMetrics.density
        val px = (sizeDp * density).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val colors = palette(context)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = px * 0.09f
            strokeCap = Paint.Cap.ROUND
        }
        val c = px / 2f
        listOf(0.44f, 0.33f, 0.22f).forEachIndexed { i, r ->
            val rad = px * r
            val box = RectF(c - rad, c - rad, c + rad, c + rad)
            paint.color = trackColor(context)
            canvas.drawArc(box, 0f, 360f, false, paint)
            val sweep = 360f * f[i].coerceIn(0f, 1f)
            if (sweep > 0.5f) {
                paint.color = colors[i]
                canvas.drawArc(box, -90f, sweep, false, paint)
            }
        }
        return bmp
    }

    /** One ring (0 steps, 1 water, 2 protein). */
    private fun ringBitmap(context: Context, sizeDp: Int, fraction: Float, i: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val px = (sizeDp * density).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val stroke = px * 0.12f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
        }
        val box = RectF(stroke / 2, stroke / 2, px - stroke / 2, px - stroke / 2)
        paint.color = trackColor(context)
        canvas.drawArc(box, 0f, 360f, false, paint)
        val sweep = 360f * fraction.coerceIn(0f, 1f)
        if (sweep > 0.5f) {
            paint.color = palette(context)[i]
            canvas.drawArc(box, -90f, sweep, false, paint)
        }
        return bmp
    }

    /** Seven bars for this week's steps; today is brightest, goal days a little dimmer. */
    private fun weekBitmap(context: Context, values: List<Int>, goal: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val w = (300 * density).toInt()
        val h = (60 * density).toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val gold = palette(context)[0]
        val top = maxOf(values.maxOrNull() ?: 0, goal, 1).toFloat()
        val slot = w / 7f
        val gap = slot * 0.18f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        values.forEachIndexed { i, value ->
            val barH = maxOf(4 * density, value / top * h)
            paint.color = when {
                i == 6 -> gold
                value > 0 && value >= goal -> (gold and 0x00FFFFFF) or (0xB0 shl 24)
                else -> trackColor(context)
            }
            val left = i * slot + gap / 2
            canvas.drawRoundRect(RectF(left, h - barH, left + slot - gap, h.toFloat()), 5 * density, 5 * density, paint)
        }
        return bmp
    }

    private fun broadcast(context: Context, cls: Class<*>, action: String, code: Int, amount: Int): PendingIntent {
        val intent = Intent(context, cls).setAction(action).putExtra(EXTRA_AMOUNT, amount)
        // A different request code for each widget type and button, so their amounts don't mix up.
        val requestCode = (cls.name.hashCode() and 0xFFFF) * 10 + code
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
