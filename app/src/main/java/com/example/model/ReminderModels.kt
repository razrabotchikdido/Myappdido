package com.example.model

data class HadithItem(
    val titleRu: String,
    val arabicText: String? = null,
    val textRu: String,
    val narratorRu: String,
    val sourceRu: String
)

data class DhikrItem(
    val id: String,
    val arabicText: String,
    val transliteration: String,
    val translationRu: String,
    val defaultTarget: Int = 33,
    val virtueRu: String
)

data class AzkarCategory(
    val id: String,
    val titleRu: String,
    val items: List<DhikrItem>
)
