package com.example.util

import java.util.Calendar
import kotlin.math.floor

object HijriCalendar {

    data class HijriDate(
        val day: Int,
        val month: Int,
        val year: Int,
        val monthNameRu: String,
        val monthNameAr: String
    )

    private val MONTH_NAMES_RU = listOf(
        "Мухаррам",
        "Сафар",
        "Раби аль-авваль",
        "Раби ас-сани",
        "Джумада аль-уля",
        "Джумада ас-сания",
        "Раджаб",
        "Шаабан",
        "Рамадан",
        "Шавваль",
        "Зуль-Каада",
        "Зуль-Хиджа"
    )

    private val MONTH_NAMES_AR = listOf(
        "محرّم",
        "صفر",
        "ربيع الأول",
        "ربيع الثاني",
        "جمادى الأولى",
        "جمادى الثانية",
        "رجب",
        "شعبان",
        "رمضان",
        "شوّال",
        "ذو القعدة",
        "ذو الحجة"
    )

    fun fromGregorian(calendar: Calendar): HijriDate {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH) + 1
        val d = calendar.get(Calendar.DAY_OF_MONTH)

        // Julian Day Number
        var my = y
        var mm = m
        if (mm <= 2) {
            my -= 1
            mm += 12
        }
        val a = floor(my / 100.0)
        val b = 2 - a + floor(a / 4.0)
        val jd = floor(365.25 * (my + 4716)) + floor(30.6001 * (mm + 1)) + d + b - 1524.5

        // Islamic calendar conversion
        val z = jd - 1948440.0 + 10632.0 / 30.0
        val n = floor(z / 10631.0)
        val z1 = z - 10631.0 * n
        val j = floor((z1 - 0.5) / 354.36667)
        val z2 = z1 - floor(j * 354.36667 + 0.5)
        val hijriYear = (30 * n + j + 1).toInt()

        var mCount = 1
        var dCount = z2.toInt()
        while (mCount <= 12) {
            val daysInMonth = if (mCount % 2 == 1 || (mCount == 12 && ((11 * hijriYear + 14) % 30 < 11))) 30 else 29
            if (dCount <= daysInMonth) break
            dCount -= daysInMonth
            mCount++
        }

        val safeMonth = mCount.coerceIn(1, 12)
        val safeDay = dCount.coerceIn(1, 30)

        return HijriDate(
            day = safeDay,
            month = safeMonth,
            year = hijriYear,
            monthNameRu = MONTH_NAMES_RU[safeMonth - 1],
            monthNameAr = MONTH_NAMES_AR[safeMonth - 1]
        )
    }

    fun getHijriDateString(calendar: Calendar): String {
        val h = fromGregorian(calendar)
        return "${h.day} ${h.monthNameRu} ${h.year} г.х."
    }
}
