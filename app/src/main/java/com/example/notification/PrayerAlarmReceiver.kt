package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.model.MuezzinVoice
import com.example.model.PrayerType
import com.example.widget.PrayerWidgetProvider

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prayerOrdinal = intent.getIntExtra("prayer_type_ordinal", -1)
        val cityName = intent.getStringExtra("city_name") ?: "Москва"
        val muezzinVoiceOrdinal = intent.getIntExtra("muezzin_voice_ordinal", MuezzinVoice.MECCA.ordinal)
        val volume = intent.getFloatExtra("volume", 0.85f)

        if (prayerOrdinal >= 0 && prayerOrdinal < PrayerType.values().size) {
            val prayerType = PrayerType.values()[prayerOrdinal]
            val muezzinVoice = MuezzinVoice.values().getOrNull(muezzinVoiceOrdinal) ?: MuezzinVoice.MECCA

            NotificationHelper.showPrayerNotification(
                context = context,
                prayerType = prayerType,
                cityName = cityName,
                muezzinVoice = muezzinVoice,
                volume = volume,
                playAudio = true
            )
        }

        // Trigger widget update
        PrayerWidgetProvider.updateAllWidgets(context)
    }

    companion object {
        const val ACTION_PRAYER_ALARM = "com.aistudio.namazqibla.ACTION_PRAYER_ALARM"

        fun scheduleAlarm(
            context: Context,
            triggerMillis: Long,
            prayerType: PrayerType,
            cityName: String,
            muezzinVoice: MuezzinVoice,
            volume: Float
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = ACTION_PRAYER_ALARM
                putExtra("prayer_type_ordinal", prayerType.ordinal)
                putExtra("city_name", cityName)
                putExtra("muezzin_voice_ordinal", muezzinVoice.ordinal)
                putExtra("volume", volume)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                prayerType.ordinal,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                // If exact alarm permission not granted, fallback to normal set
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        }
    }
}
