package com.example.model

data class Ayah(
    val numberInSurah: Int,
    val arabicText: String,
    val transliteration: String,
    val translationRu: String,
    val tafsirNote: String? = null
)

data class Surah(
    val number: Int,
    val nameArabic: String,
    val nameTranslit: String,
    val nameTranslationRu: String,
    val revelationTypeRu: String, // Мекканская / Мединская
    val totalVerses: Int,
    val ayahs: List<Ayah>
)
