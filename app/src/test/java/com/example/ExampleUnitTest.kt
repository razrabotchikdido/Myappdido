package com.example

import com.example.model.AsrSchool
import com.example.model.CalculationMethod
import com.example.model.PrayerType
import com.example.util.CityDatabase
import com.example.util.HijriCalendar
import com.example.util.PrayerCalculator
import com.example.util.QiblaCalculator
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {

    @Test
    fun testPrayerCalculation() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 2)
        }

        val schedule = PrayerCalculator.calculate(
            calendar = calendar,
            latitude = 55.7558,
            longitude = 37.6173,
            timeZoneOffsetHours = 3.0,
            method = CalculationMethod.RUSSIA_SAM,
            asrSchool = AsrSchool.STANDARD
        )

        assertNotNull(schedule)
        assertEquals(6, schedule.items.size)

        val fajr = schedule.items.find { it.type == PrayerType.FAJR }
        val dhuhr = schedule.items.find { it.type == PrayerType.DHUHR }
        val asr = schedule.items.find { it.type == PrayerType.ASR }
        val maghrib = schedule.items.find { it.type == PrayerType.MAGHRIB }
        val isha = schedule.items.find { it.type == PrayerType.ISHA }

        assertNotNull(fajr)
        assertNotNull(dhuhr)
        assertNotNull(asr)
        assertNotNull(maghrib)
        assertNotNull(isha)

        assertTrue(fajr!!.timeMillis < dhuhr!!.timeMillis)
        assertTrue(dhuhr.timeMillis < asr!!.timeMillis)
        assertTrue(asr.timeMillis < maghrib!!.timeMillis)
        assertTrue(maghrib.timeMillis < isha!!.timeMillis)
    }

    @Test
    fun testQiblaCalculation() {
        // Moscow to Kaaba
        val qibla = QiblaCalculator.calculateQibla(55.7558, 37.6173)
        // Bearing from Moscow is approximately 182-183 degrees (South)
        assertTrue("Bearing should be near 182 deg", qibla.bearingDegrees in 175f..190f)
        assertTrue("Distance should be approximately 3800km", qibla.distanceKm in 3500..4200)
    }

    @Test
    fun testHijriCalendar() {
        val calendar = Calendar.getInstance()
        val hijriDate = HijriCalendar.fromGregorian(calendar)
        assertTrue(hijriDate.year in 1445..1455)
        assertTrue(hijriDate.month in 1..12)
        assertTrue(hijriDate.day in 1..30)
        assertFalse(hijriDate.monthNameRu.isBlank())
    }

    @Test
    fun testCityDatabaseSearch() {
        val results = CityDatabase.search("Казань")
        assertTrue(results.isNotEmpty())
        assertEquals("Казань", results.first().nameRu)

        val all = CityDatabase.search("")
        assertTrue(all.size > 20)
    }
}
