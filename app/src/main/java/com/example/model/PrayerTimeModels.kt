package com.example.model

enum class PrayerType(
    val id: String,
    val titleRu: String,
    val titleAr: String,
    val isFardPrayer: Boolean
) {
    FAJR("fajr", "Фаджр", "الفجر", true),
    SUNRISE("sunrise", "Восход", "الشروق", false),
    DHUHR("dhuhr", "Зухр", "الظهر", true),
    ASR("asr", "Аср", "العصر", true),
    MAGHRIB("maghrib", "Магриб", "المغرب", true),
    ISHA("isha", "Иша", "العشاء", true)
}

data class PrayerTimeItem(
    val type: PrayerType,
    val timeFormatted: String, // "05:32"
    val timeMillis: Long,
    val isPassed: Boolean = false,
    val isCurrent: Boolean = false,
    val isNext: Boolean = false,
    val notificationEnabled: Boolean = true
)

data class DayPrayerTimes(
    val dateGregorianFormatted: String,
    val dateHijriFormatted: String,
    val items: List<PrayerTimeItem>,
    val nextPrayer: PrayerTimeItem?,
    val currentPrayer: PrayerTimeItem?,
    val secondsUntilNext: Long
)

enum class CalculationMethod(
    val id: String,
    val titleRu: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaMinutesAfterMaghrib: Int? = null
) {
    RUSSIA_SAM("sam_russia", "ДУМ РФ (Россия)", 16.0, 15.0),
    MUSLIM_WORLD_LEAGUE("mwl", "Всемирная Исламская Лига (MWL)", 18.0, 17.0),
    UMM_AL_QURA("umm_al_qura", "Умм аль-Кура (Мекка)", 18.5, 0.0, 90),
    DIYANET("diyanet", "Диянет (Турция)", 18.0, 17.0),
    EGYPT("egypt", "Египетское генеральное управление", 19.5, 17.5),
    ISNA("isna", "ISNA (Северная Америка)", 15.0, 15.0),
    KARACHI("karachi", "Университет исламских наук (Карачи)", 18.0, 18.0)
}

enum class AsrSchool(val id: String, val titleRu: String, val shadowFactor: Double) {
    STANDARD("standard", "Шафии, Малики, Ханбали (Стандарт)", 1.0),
    HANAFI("hanafi", "Ханафи (Двойная тень)", 2.0)
}

data class CityLocation(
    val nameRu: String,
    val countryRu: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneOffsetHours: Double
)
