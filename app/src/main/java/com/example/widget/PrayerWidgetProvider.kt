package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.model.PrayerType
import com.example.util.CityDatabase
import com.example.util.PrayerCalculator
import java.util.Calendar
import java.util.Locale

class PrayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.prayer_widget)

            // Calculate current prayer schedule for default or stored city
            val calendar = Calendar.getInstance()
            val city = CityDatabase.CITIES.first() // Default Moscow or current
            val schedule = PrayerCalculator.calculate(
                calendar = calendar,
                latitude = city.latitude,
                longitude = city.longitude,
                timeZoneOffsetHours = city.timeZoneOffsetHours
            )

            views.setTextViewText(R.id.widget_city, city.nameRu)
            views.setTextViewText(R.id.widget_hijri_date, schedule.dateHijriFormatted)

            val next = schedule.nextPrayer
            if (next != null) {
                views.setTextViewText(
                    R.id.widget_next_prayer_title,
                    "Следующий: ${next.type.titleRu}"
                )
                views.setTextViewText(R.id.widget_next_prayer_time, next.timeFormatted)

                val hours = schedule.secondsUntilNext / 3600
                val mins = (schedule.secondsUntilNext % 3600) / 60
                views.setTextViewText(
                    R.id.widget_countdown,
                    String.format(Locale.getDefault(), "через %02d:%02d", hours, mins)
                )
            }

            // Fill individual prayer times
            for (item in schedule.items) {
                when (item.type) {
                    PrayerType.FAJR -> views.setTextViewText(R.id.widget_time_fajr, item.timeFormatted)
                    PrayerType.DHUHR -> views.setTextViewText(R.id.widget_time_dhuhr, item.timeFormatted)
                    PrayerType.ASR -> views.setTextViewText(R.id.widget_time_asr, item.timeFormatted)
                    PrayerType.MAGHRIB -> views.setTextViewText(R.id.widget_time_maghrib, item.timeFormatted)
                    PrayerType.ISHA -> views.setTextViewText(R.id.widget_time_isha, item.timeFormatted)
                    else -> {}
                }
            }

            // Open app on click
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, PrayerWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }
    }
}
