package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppThemeMode
import com.example.model.Ayah
import com.example.model.Surah
import com.example.model.UserSettings
import com.example.ui.theme.*
import com.example.util.QuranRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(
    settings: UserSettings,
    onSettingsChanged: (UserSettings) -> Unit,
    isLandscapeOrExpanded: Boolean = false
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSurah by remember { mutableStateOf<Surah?>(QuranRepository.SURAHS.first()) }
    var bookmarkedAyahs by remember { mutableStateOf(setOf<String>()) }
    var playingAyahIndex by remember { mutableStateOf<Int?>(null) }
    var isReadingSettingsOpen by remember { mutableStateOf(false) }

    val filteredSurahs = remember(searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) QuranRepository.SURAHS
        else QuranRepository.SURAHS.filter {
            it.nameTranslit.lowercase().contains(q) ||
                    it.nameTranslationRu.lowercase().contains(q) ||
                    it.nameArabic.contains(q) ||
                    it.number.toString() == q
        }
    }

    if (isLandscapeOrExpanded) {
        // Split-pane layout: Surahs list on the left, Reader on the right
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SurahListPane(
                    surahs = filteredSurahs,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    selectedSurah = selectedSurah,
                    onSelectSurah = { selectedSurah = it }
                )
            }

            Box(modifier = Modifier.weight(1.6f)) {
                if (selectedSurah != null) {
                    SurahReaderPane(
                        surah = selectedSurah!!,
                        settings = settings,
                        onSettingsChanged = onSettingsChanged,
                        bookmarkedAyahs = bookmarkedAyahs,
                        onToggleBookmark = { ayahKey ->
                            bookmarkedAyahs = if (bookmarkedAyahs.contains(ayahKey)) {
                                bookmarkedAyahs - ayahKey
                            } else {
                                bookmarkedAyahs + ayahKey
                            }
                        },
                        playingAyahIndex = playingAyahIndex,
                        onTogglePlayAyah = { index ->
                            playingAyahIndex = if (playingAyahIndex == index) null else index
                        },
                        onBack = null
                    )
                }
            }
        }
    } else {
        // Phone layout: toggle between list and reader
        if (selectedSurah == null) {
            SurahListPane(
                surahs = filteredSurahs,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                selectedSurah = null,
                onSelectSurah = { selectedSurah = it }
            )
        } else {
            SurahReaderPane(
                surah = selectedSurah!!,
                settings = settings,
                onSettingsChanged = onSettingsChanged,
                bookmarkedAyahs = bookmarkedAyahs,
                onToggleBookmark = { ayahKey ->
                    bookmarkedAyahs = if (bookmarkedAyahs.contains(ayahKey)) {
                        bookmarkedAyahs - ayahKey
                    } else {
                        bookmarkedAyahs + ayahKey
                    }
                },
                playingAyahIndex = playingAyahIndex,
                onTogglePlayAyah = { index ->
                    playingAyahIndex = if (playingAyahIndex == index) null else index
                },
                onBack = { selectedSurah = null }
            )
        }
    }
}

@Composable
fun SurahListPane(
    surahs: List<Surah>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedSurah: Surah?,
    onSelectSurah: (Surah) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("quran_search_field"),
            placeholder = { Text("Поиск суры (название, номер)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Очистить")
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(surahs) { surah ->
                val isSelected = selectedSurah?.number == surah.number
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSurah(surah) }
                        .testTag("surah_item_${surah.number}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = surah.number.toString(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = surah.nameTranslit,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${surah.nameTranslationRu} • ${surah.totalVerses} аятов",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = surah.nameArabic,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SurahReaderPane(
    surah: Surah,
    settings: UserSettings,
    onSettingsChanged: (UserSettings) -> Unit,
    bookmarkedAyahs: Set<String>,
    onToggleBookmark: (String) -> Unit,
    playingAyahIndex: Int?,
    onTogglePlayAyah: (Int) -> Unit,
    onBack: (() -> Unit)?
) {
    var showQuickSettings by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("surah_reader_pane")
    ) {
        // Reader Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_to_surahs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад к списку"
                        )
                    }
                }
                Column {
                    Text(
                        text = "${surah.number}. ${surah.nameTranslit} (${surah.nameArabic})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${surah.revelationTypeRu} • ${surah.nameTranslationRu}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = { showQuickSettings = !showQuickSettings }) {
                Icon(
                    imageVector = Icons.Default.FormatSize,
                    contentDescription = "Настройки шрифта и темы"
                )
            }
        }

        // Quick Reader settings panel (Font size & reading mode: Sepia, Night, Light, Dark)
        if (showQuickSettings) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Режим чтения и размер текста (${settings.quranFontSizeSp} sp)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = settings.quranFontSizeSp.toFloat(),
                        onValueChange = { onSettingsChanged(settings.copy(quranFontSizeSp = it.toInt())) },
                        valueRange = 14f..32f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            AppThemeMode.NIGHT to "Ночной (OLED)",
                            AppThemeMode.SEPIA to "Сепия",
                            AppThemeMode.DARK to "Темная",
                            AppThemeMode.LIGHT to "Светлая"
                        ).forEach { (theme, label) ->
                            FilterChip(
                                selected = settings.themeMode == theme,
                                onClick = { onSettingsChanged(settings.copy(themeMode = theme)) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Показывать транскрипцию",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Switch(
                            checked = settings.showTransliteration,
                            onCheckedChange = { onSettingsChanged(settings.copy(showTransliteration = it)) }
                        )
                    }
                }
            }
        }

        // Ayahs List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
        ) {
            // Bismillah Header (except for Surah 9)
            if (surah.number != 9 && surah.number != 1) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            items(surah.ayahs) { ayah ->
                val ayahKey = "${surah.number}:${ayah.numberInSurah}"
                val isBookmarked = bookmarkedAyahs.contains(ayahKey)
                val isPlaying = playingAyahIndex == ayah.numberInSurah

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ayah_card_$ayahKey"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Header: Ayah number & bookmark/play buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ayah.numberInSurah.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = { onTogglePlayAyah(ayah.numberInSurah) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                        contentDescription = "Воспроизвести аят",
                                        tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { onToggleBookmark(ayahKey) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Закладка",
                                        tint = if (isBookmarked) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Arabic Ayah Text
                        Text(
                            text = ayah.arabicText,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = (settings.quranFontSizeSp + 6).sp,
                                lineHeight = (settings.quranFontSizeSp + 16).sp
                            ),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Transliteration
                        if (settings.showTransliteration && ayah.transliteration.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = ayah.transliteration,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = (settings.quranFontSizeSp - 3).sp
                                ),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Russian Translation (Кулиев)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = ayah.translationRu,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = settings.quranFontSizeSp.sp,
                                lineHeight = (settings.quranFontSizeSp + 8).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Tafsir / Commentary
                        if (ayah.tafsirNote != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = ayah.tafsirNote,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
