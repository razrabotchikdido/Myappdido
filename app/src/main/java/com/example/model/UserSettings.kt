package com.example.model

enum class AppThemeMode(val id: String, val titleRu: String) {
    SYSTEM("system", "Системная"),
    LIGHT("light", "Светлая"),
    DARK("dark", "Темная"),
    SEPIA("sepia", "Сепия (Мягкая)"),
    NIGHT("night", "Ночной режим (OLED)")
}

enum class MuezzinVoice(val id: String, val titleRu: String, val descriptionRu: String) {
    MECCA("mecca", "Мекка (Масджид аль-Харам)", "Шейх Али аль-Мулла, классический призыв"),
    MEDINA("medina", "Медина (Масджид ан-Набави)", "Шейх Исам аль-Бухари, торжественный стиль"),
    AL_AQSA("al_aqsa", "Аль-Акса (Иерусалим)", "Традиционный напев святыни"),
    ALAFASY("alafasy", "Мишари Рашид аль-Афаси", "Мелодичный и проникновенный голос"),
    ISTANBUL("istanbul", "Стамбул (Османский макам)", "Напев макама Сабах, умиротворяющий"),
    SOFT_TONE("soft_tone", "Мягкий сигнал (Такбир)", "Краткий звонкий такбир «Аллаху Акбар»")
}

data class UserSettings(
    val userName: String = "Мусульманин",
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val calculationMethod: CalculationMethod = CalculationMethod.RUSSIA_SAM,
    val asrSchool: AsrSchool = AsrSchool.STANDARD,
    val selectedMuezzin: MuezzinVoice = MuezzinVoice.MECCA,
    val adhanVolume: Float = 0.85f,
    val isAutoLocationEnabled: Boolean = true,
    val selectedCity: CityLocation = CityLocation("Москва", "Россия", 55.7558, 37.6173, 3.0),
    val notifyFajr: Boolean = true,
    val notifySunrise: Boolean = false,
    val notifyDhuhr: Boolean = true,
    val notifyAsr: Boolean = true,
    val notifyMaghrib: Boolean = true,
    val notifyIsha: Boolean = true,
    val prePrayerReminderMinutes: Int = 15,
    val isPreReminderEnabled: Boolean = true,
    val quranFontSizeSp: Int = 20,
    val showTransliteration: Boolean = true,
    val tasbihVibrateEnabled: Boolean = true
)
