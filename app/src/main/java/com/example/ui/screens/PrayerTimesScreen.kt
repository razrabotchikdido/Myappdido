package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DayPrayerTimes
import com.example.model.PrayerTimeItem
import com.example.model.PrayerType
import com.example.model.UserSettings
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import java.util.Calendar
import java.util.Locale

@Composable
fun PrayerTimesScreen(
    schedule: DayPrayerTimes,
    selectedCalendar: Calendar,
    settings: UserSettings,
    isLandscapeOrExpanded: Boolean = false,
    onPreviousDay: () -> Unit,
    onToday: () -> Unit,
    onNextDay: () -> Unit,
    onRequestLocation: () -> Unit,
    onOpenProfile: () -> Unit
) {
    if (isLandscapeOrExpanded) {
        // Two-pane layout for tablets or landscape
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                PrayerHeroCard(
                    schedule = schedule,
                    settings = settings,
                    onRequestLocation = onRequestLocation,
                    onOpenProfile = onOpenProfile
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                PrayerScheduleList(
                    schedule = schedule,
                    onPreviousDay = onPreviousDay,
                    onToday = onToday,
                    onNextDay = onNextDay
                )
            }
        }
    } else {
        // Vertical mobile layout
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            item {
                PrayerHeroCard(
                    schedule = schedule,
                    settings = settings,
                    onRequestLocation = onRequestLocation,
                    onOpenProfile = onOpenProfile
                )
            }

            item {
                DateNavigationRow(
                    schedule = schedule,
                    onPreviousDay = onPreviousDay,
                    onToday = onToday,
                    onNextDay = onNextDay
                )
            }

            items(schedule.items) { prayerItem ->
                PrayerTimeCard(prayerItem = prayerItem)
            }

            item {
                IslamicWisdomBanner()
            }
        }
    }
}

@Composable
fun PrayerHeroCard(
    schedule: DayPrayerTimes,
    settings: UserSettings,
    onRequestLocation: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val next = schedule.nextPrayer
    val seconds = schedule.secondsUntilNext
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val sec = seconds % 60

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer_hero_card"),
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            EmeraldDark,
                            EmeraldPrimary,
                            Color(0xFF07271D)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                // Location & Hijri Date Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable { onRequestLocation() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Локация",
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = settings.selectedCity.nameRu,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Обновить геопозицию",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = schedule.dateHijriFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = GoldAccent
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Next Prayer Title & Countdown
                Text(
                    text = if (next != null) "Следующий намаз: ${next.type.titleRu} (${next.type.titleAr})" else "Расписание намаза",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = next?.timeFormatted ?: "--:--",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldAccent
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Осталось времени",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, sec),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Current prayer badge & method note
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val curr = schedule.currentPrayer
                    if (curr != null) {
                        Text(
                            text = "Сейчас: ${curr.type.titleRu} (с ${curr.timeFormatted})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Text(
                        text = settings.calculationMethod.titleRu.take(20) + "...",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun DateNavigationRow(
    schedule: DayPrayerTimes,
    onPreviousDay: () -> Unit,
    onToday: () -> Unit,
    onNextDay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousDay,
            modifier = Modifier.testTag("prev_day_button")
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Предыдущий день",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.clickable { onToday() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = schedule.dateGregorianFormatted,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        IconButton(
            onClick = onNextDay,
            modifier = Modifier.testTag("next_day_button")
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Следующий день",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun PrayerTimeCard(prayerItem: PrayerTimeItem) {
    val isNext = prayerItem.isNext
    val isCurrent = prayerItem.isCurrent

    val cardColor = when {
        isNext -> MaterialTheme.colorScheme.primaryContainer
        isCurrent -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surface
    }

    val textColor = when {
        isNext -> MaterialTheme.colorScheme.onPrimaryContainer
        isCurrent -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer_card_${prayerItem.type.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isNext) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon indicator
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isNext) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val iconVector = when (prayerItem.type) {
                        PrayerType.FAJR -> Icons.Default.NightsStay
                        PrayerType.SUNRISE -> Icons.Default.WbTwilight
                        PrayerType.DHUHR -> Icons.Default.WbSunny
                        PrayerType.ASR -> Icons.Default.LightMode
                        PrayerType.MAGHRIB -> Icons.Default.WbTwilight
                        PrayerType.ISHA -> Icons.Default.Bedtime
                    }
                    Icon(
                        imageVector = iconVector,
                        contentDescription = prayerItem.type.titleRu,
                        tint = if (isNext) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = prayerItem.type.titleRu,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = prayerItem.type.titleAr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }

                    if (isNext) {
                        Text(
                            text = "Следующий намаз",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (isCurrent) {
                        Text(
                            text = "Текущее время",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = prayerItem.timeFormatted,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Уведомление включено",
                    tint = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PrayerScheduleList(
    schedule: DayPrayerTimes,
    onPreviousDay: () -> Unit,
    onToday: () -> Unit,
    onNextDay: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            DateNavigationRow(
                schedule = schedule,
                onPreviousDay = onPreviousDay,
                onToday = onToday,
                onNextDay = onNextDay
            )
        }
        items(schedule.items) { item ->
            PrayerTimeCard(prayerItem = item)
        }
    }
}

@Composable
fun IslamicWisdomBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = GoldAccent,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Пятничный день (Аль-Джума)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Сунна пятницы: совершение гусля, чтение суры «Аль-Кахф», обильное благословение Пророка ﷺ и коллективная молитва.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
