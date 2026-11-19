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
import java.util.Locale

const val ACTION_ADD_WATER = "com.sipscoop.app.ADD_WATER"
const val ACTION_ADD_PROTEIN = "com.sipscoop.app.ADD_PROTEIN"

/** Shared logic for both widget sizes. */
abstract class BaseWidget(private val layout: Int) : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val state = Store.load(context)
        ids.forEach { manager.updateAppWidget(it, WidgetSync.build(context, layout, javaClass, state)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ADD_WATER -> {
                Store.update(context) { it.addWater(it.waterQuick.firstOrNull() ?: 250) }
                WidgetSync.refreshAll(context)
            }
            ACTION_ADD_PROTEIN -> {
                Store.update(context) { it.addProtein(it.proteinQuick.firstOrNull() ?: 10) }
                WidgetSync.refreshAll(context)
            }
            else -> super.onReceive(context, intent)
        }
    }
}

class WideWidget : BaseWidget(R.layout.widget_wide)

class CompactWidget : BaseWidget(R.layout.widget_compact)

object WidgetSync {
    private val providers = listOf(
        WideWidget::class.java to R.layout.widget_wide,
        CompactWidget::class.java to R.layout.widget_compact
    )

    fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val state = Store.load(context)
        providers.forEach { (cls, layout) ->
            val ids = manager.getAppWidgetIds(ComponentName(context, cls))
            ids.forEach { manager.updateAppWidget(it, build(context, layout, cls, state)) }
        }
    }

    private fun fmt(n: Int) = String.format(Locale.US, "%,d", n)

    private fun liters(ml: Int): String {
        val s = if (ml % 1000 == 0) "${ml / 1000}" else
            String.format(Locale.US, "%.2f", ml / 1000.0).trimEnd('0').trimEnd('.')
        return "$s L"
    }

    fun build(context: Context, layout: Int, cls: Class<*>, s: AppState): RemoteViews {
        val views = RemoteViews(context.packageName, layout)
        val wide = layout == R.layout.widget_wide
        val waterStep = s.waterQuick.firstOrNull() ?: 250
        val proteinStep = s.proteinQuick.firstOrNull() ?: 10
        val stepPct = pct(s.steps, s.stepGoal)
        val waterPct = pct(s.waterTotal, s.waterGoal)
        val proteinPct = pct(s.proteinTotal, s.proteinGoal)

        // One row each for steps, water and protein.
        views.setTextViewText(R.id.steps_value, fmt(s.steps))
        views.setTextViewText(R.id.steps_goal, if (wide) "/ ${fmt(s.stepGoal)}" else "steps")
        views.setProgressBar(R.id.steps_bar, 100, stepPct, false)
        views.setTextViewText(R.id.water_value, litersNumber(s.waterTotal))
        views.setTextViewText(R.id.water_goal, "/ ${liters(s.waterGoal)}")
        views.setProgressBar(R.id.water_bar, 100, waterPct, false)
        views.setTextViewText(R.id.protein_value, "${s.proteinTotal}")
        views.setTextViewText(R.id.protein_goal, "/ ${s.proteinGoal} g")
        views.setProgressBar(R.id.protein_bar, 100, proteinPct, false)
        views.setTextViewText(R.id.water_add, if (wide) "+$waterStep ml" else "+$waterStep")
        views.setTextViewText(R.id.protein_add, "+$proteinStep g")

        // Both diet goals hit: swap the buttons for a done message.
        val done = s.onboarded && s.todayMet
        views.setViewVisibility(R.id.normal_group, if (done) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.done_group, if (done) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.done_streak, "${s.streak}-day diet streak")
        if (wide) {
            views.setTextViewText(R.id.done_totals, "${liters(s.waterTotal)} water · ${s.proteinTotal} g protein\n${fmt(s.steps)} steps")
        }

        // The rings are drawn as a picture, in the phone's light or dark colours.
        val ringSize = if (wide) 84 else 64
        val fractions = floatArrayOf(stepPct / 100f, waterPct / 100f, proteinPct / 100f)
        runCatching {
            if (wide) views.setImageViewBitmap(R.id.rings, ringsBitmap(context, ringSize, fractions))
            if (done) views.setImageViewBitmap(R.id.done_rings, ringsBitmap(context, ringSize, floatArrayOf(1f, 1f, 1f)))
        }

        views.setOnClickPendingIntent(R.id.water_add, broadcast(context, cls, ACTION_ADD_WATER, 1))
        views.setOnClickPendingIntent(R.id.protein_add, broadcast(context, cls, ACTION_ADD_PROTEIN, 2))

        val open = PendingIntent.getActivity(
            context, 3,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_root, open)
        return views
    }

    private fun pct(value: Int, goal: Int) = if (goal > 0) minOf(100, (value.toLong() * 100 / goal).toInt()) else 0

    /** "1.75" for 1,750 ml (the unit is shown next to the goal). */
    private fun litersNumber(ml: Int): String = liters(ml).removeSuffix(" L")

    /** Three rounded rings (steps, water, protein) drawn into a small picture. */
    private fun ringsBitmap(context: Context, sizeDp: Int, f: FloatArray): Bitmap {
        val density = context.resources.displayMetrics.density
        val px = (sizeDp * density).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val colors = if (dark) intArrayOf(0xFFF5B940.toInt(), 0xFF4EA8FF.toInt(), 0xFFFF7A45.toInt())
        else intArrayOf(0xFFD9940E.toInt(), 0xFF2F80ED.toInt(), 0xFFEF6428.toInt())
        val track = if (dark) 0x1FFFFFFF else 0x140F1318
        val stroke = px * 0.09f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
        }
        val c = px / 2f
        listOf(0.44f, 0.33f, 0.22f).forEachIndexed { i, r ->
            val rad = px * r
            val box = RectF(c - rad, c - rad, c + rad, c + rad)
            paint.color = track
            canvas.drawArc(box, 0f, 360f, false, paint)
            val sweep = 360f * f[i].coerceIn(0f, 1f)
            if (sweep > 0.5f) {
                paint.color = colors[i]
                canvas.drawArc(box, -90f, sweep, false, paint)
            }
        }
        return bmp
    }

    private fun broadcast(context: Context, cls: Class<*>, action: String, code: Int): PendingIntent {
        val intent = Intent(context, cls).setAction(action)
        val requestCode = code + if (cls == CompactWidget::class.java) 10 else 0
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
