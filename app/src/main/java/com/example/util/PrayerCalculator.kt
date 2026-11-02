package com.example.util

import com.example.model.AsrSchool
import com.example.model.CalculationMethod
import com.example.model.DayPrayerTimes
import com.example.model.PrayerTimeItem
import com.example.model.PrayerType
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

object PrayerCalculator {

    private const val DEG_TO_RAD = Math.PI / 180.0
    private const val RAD_TO_DEG = 180.0 / Math.PI

    private fun dSin(d: Double): Double = sin(d * DEG_TO_RAD)
    private fun dCos(d: Double): Double = cos(d * DEG_TO_RAD)
    private fun dTan(d: Double): Double = tan(d * DEG_TO_RAD)
    private fun dAsin(x: Double): Double = asin(x.coerceIn(-1.0, 1.0)) * RAD_TO_DEG
    private fun dAcos(x: Double): Double = acos(x.coerceIn(-1.0, 1.0)) * RAD_TO_DEG
    private fun dAtan(x: Double): Double = atan(x) * RAD_TO_DEG

    private fun fixHour(a: Double): Double {
        var h = a - 24.0 * floor(a / 24.0)
        if (h < 0) h += 24.0
        return h
    }

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private data class SunCoordinates(val declination: Double, val equationOfTime: Double)

    private fun sunCoordinates(jd: Double): SunCoordinates {
        val d = jd - 2451545.0
        val g = fixHour(357.529 + 0.98560028 * d)
        val q = fixHour(280.459 + 0.98564736 * d)
        val l = fixHour(q + 1.915 * dSin(g) + 0.020 * dSin(2 * g))

        val e = 23.439 - 0.00000036 * d
        val ra = fixHour(dAtan(dCos(e) * dSin(l) / dCos(l))) / 15.0

        val declination = dAsin(dSin(e) * dSin(l))
        val equationOfTime = (q / 15.0 - ra) * 60.0 // in minutes
        return SunCoordinates(declination, equationOfTime)
    }

    /**
     * Calculates prayer times for a given day, latitude, longitude, and settings.
     */
    fun calculate(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        timeZoneOffsetHours: Double,
        method: CalculationMethod = CalculationMethod.RUSSIA_SAM,
        asrSchool: AsrSchool = AsrSchool.STANDARD
    ): DayPrayerTimes {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val jd = julianDay(year, month, day)
        val sun = sunCoordinates(jd)

        // Midday (Transit)
        val noon = fixHour(12.0 + timeZoneOffsetHours - (longitude / 15.0) - (sun.equationOfTime / 60.0))

        // Hour angle for an altitude angle (negative below horizon)
        fun hourAngle(altitude: Double): Double {
            val cosH = (dSin(altitude) - dSin(latitude) * dSin(sun.declination)) /
                    (dCos(latitude) * dCos(sun.declination))
            return if (cosH < -1.0) 180.0
            else if (cosH > 1.0) 0.0
            else dAcos(cosH)
        }

        val sunriseH = hourAngle(-0.8333)
        val sunsetH = hourAngle(-0.8333)

        // Fajr
        val fajrAngle = -method.fajrAngle
        var fajrH = hourAngle(fajrAngle)
        // High latitude adjustment fallback (e.g. northern regions in summer)
        if (fajrH <= 0.0 || fajrH >= 180.0) {
            val nightLength = 24.0 - (sunsetH - (-sunriseH)) / 15.0
            fajrH = sunriseH + (nightLength / 7.0) * 15.0
        }

        // Isha
        var ishaH = if (method.ishaMinutesAfterMaghrib != null) {
            0.0
        } else {
            hourAngle(-method.ishaAngle)
        }
        if (method.ishaMinutesAfterMaghrib == null && (ishaH <= 0.0 || ishaH >= 180.0)) {
            val nightLength = 24.0 - (sunsetH - (-sunriseH)) / 15.0
            ishaH = sunsetH + (nightLength / 7.0) * 15.0
        }

        // Asr altitude
        val shadowFactor = asrSchool.shadowFactor
        val delta = kotlin.math.abs(latitude - sun.declination)
        val asrAltitude = dAtan(1.0 / (shadowFactor + dTan(delta)))
        val asrH = hourAngle(asrAltitude)

        val fajrTime = fixHour(noon - fajrH / 15.0)
        val sunriseTime = fixHour(noon - sunriseH / 15.0)
        val dhuhrTime = fixHour(noon + 2.0 / 60.0) // 2 min margin after zenith
        val asrTime = fixHour(noon + asrH / 15.0)
        val maghribTime = fixHour(noon + sunsetH / 15.0 + 2.0 / 60.0)
        val ishaTime = if (method.ishaMinutesAfterMaghrib != null) {
            fixHour(maghribTime + method.ishaMinutesAfterMaghrib / 60.0)
        } else {
            fixHour(noon + ishaH / 15.0)
        }

        fun toMillis(hourDouble: Double): Long {
            val cal = calendar.clone() as Calendar
            val hours = floor(hourDouble).toInt()
            val minutes = floor((hourDouble - hours) * 60.0).toInt()
            val seconds = floor(((hourDouble - hours) * 60.0 - minutes) * 60.0).toInt()
            cal.set(Calendar.HOUR_OF_DAY, hours)
            cal.set(Calendar.MINUTE, minutes)
            cal.set(Calendar.SECOND, seconds)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun formatTime(hourDouble: Double): String {
            val hours = floor(hourDouble).toInt()
            val minutes = floor((hourDouble - hours) * 60.0).toInt()
            return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
        }

        val fajrMillis = toMillis(fajrTime)
        val sunriseMillis = toMillis(sunriseTime)
        val dhuhrMillis = toMillis(dhuhrTime)
        val asrMillis = toMillis(asrTime)
        val maghribMillis = toMillis(maghribTime)
        val ishaMillis = toMillis(ishaTime)

        val now = System.currentTimeMillis()

        val rawItems = listOf(
            PrayerTimeItem(PrayerType.FAJR, formatTime(fajrTime), fajrMillis),
            PrayerTimeItem(PrayerType.SUNRISE, formatTime(sunriseTime), sunriseMillis),
            PrayerTimeItem(PrayerType.DHUHR, formatTime(dhuhrTime), dhuhrMillis),
            PrayerTimeItem(PrayerType.ASR, formatTime(asrTime), asrMillis),
            PrayerTimeItem(PrayerType.MAGHRIB, formatTime(maghribTime), maghribMillis),
            PrayerTimeItem(PrayerType.ISHA, formatTime(ishaTime), ishaMillis)
        )

        // Determine current and next prayer
        var nextItem: PrayerTimeItem? = null
        var currentItem: PrayerTimeItem? = null

        for (i in rawItems.indices) {
            val item = rawItems[i]
            if (item.timeMillis > now) {
                nextItem = item
                if (i > 0) {
                    currentItem = rawItems[i - 1]
                } else {
                    currentItem = rawItems.last()
                }
                break
            }
        }

        // If all prayers today have passed, next prayer is Fajr tomorrow
        if (nextItem == null) {
            currentItem = rawItems.last()
            val tomorrowFajrMillis = fajrMillis + 24 * 3600 * 1000L
            nextItem = rawItems.first().copy(timeMillis = tomorrowFajrMillis)
        }

        val secondsUntilNext = ((nextItem.timeMillis - now) / 1000L).coerceAtLeast(0L)

        val processedItems = rawItems.map { item ->
            val isNext = item.type == nextItem.type
            val isCurrent = item.type == currentItem?.type
            val isPassed = item.timeMillis < now && !isCurrent
            item.copy(isPassed = isPassed, isCurrent = isCurrent, isNext = isNext)
        }

        val gregFormatted = String.format(
            Locale("ru"),
            "%d %s %d г.",
            day,
            getRussianMonthName(month),
            year
        )
        val hijriFormatted = HijriCalendar.getHijriDateString(calendar)

        return DayPrayerTimes(
            dateGregorianFormatted = gregFormatted,
            dateHijriFormatted = hijriFormatted,
            items = processedItems,
            nextPrayer = nextItem,
            currentPrayer = currentItem,
            secondsUntilNext = secondsUntilNext
        )
    }

    private fun getRussianMonthName(month: Int): String = when (month) {
        1 -> "января"
        2 -> "февраля"
        3 -> "марта"
        4 -> "апреля"
        5 -> "мая"
        6 -> "июня"
        7 -> "июля"
        8 -> "августа"
        9 -> "сентября"
        10 -> "октября"
        11 -> "ноября"
        12 -> "декабря"
        else -> ""
    }
}
