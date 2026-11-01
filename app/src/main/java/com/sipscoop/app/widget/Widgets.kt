package com.sipscoop.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
        val waterStep = s.waterQuick.firstOrNull() ?: 250
        val proteinStep = s.proteinQuick.firstOrNull() ?: 10
        val waterPct = if (s.waterGoal > 0) minOf(100, s.waterTotal * 100 / s.waterGoal) else 0
        val proteinPct = if (s.proteinGoal > 0) minOf(100, s.proteinTotal * 100 / s.proteinGoal) else 0

        if (layout == R.layout.widget_wide) {
            views.setTextViewText(R.id.water_value, fmt(s.waterTotal))
            views.setTextViewText(R.id.water_goal, "/ ${fmt(s.waterGoal)} ml")
            views.setTextViewText(R.id.protein_value, "${s.proteinTotal}")
            views.setTextViewText(R.id.protein_goal, "/ ${s.proteinGoal} g")
            views.setProgressBar(R.id.water_bar, 100, waterPct, false)
            views.setProgressBar(R.id.protein_bar, 100, proteinPct, false)
            views.setTextViewText(R.id.water_add, "+$waterStep ml")
            views.setTextViewText(R.id.protein_add, "+$proteinStep g")
        } else {
            views.setTextViewText(R.id.water_value, liters(s.waterTotal))
            views.setTextViewText(R.id.protein_value, "${s.proteinTotal} g")
            views.setTextViewText(R.id.water_add, "+$waterStep")
            views.setTextViewText(R.id.protein_add, "+$proteinStep g")
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

    private fun broadcast(context: Context, cls: Class<*>, action: String, code: Int): PendingIntent {
        val intent = Intent(context, cls).setAction(action)
        val requestCode = code + if (cls == CompactWidget::class.java) 10 else 0
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
