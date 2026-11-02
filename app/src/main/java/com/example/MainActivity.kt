package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.CityLocation
import com.example.model.PrayerType
import com.example.model.UserSettings
import com.example.notification.NotificationHelper
import com.example.notification.PrayerAlarmReceiver
import com.example.sensor.CompassSensorManager
import com.example.ui.screens.PrayerTimesScreen
import com.example.ui.screens.ProfileDialog
import com.example.ui.screens.QiblaScreen
import com.example.ui.screens.QuranScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.util.CityDatabase
import com.example.util.PrayerCalculator
import com.example.util.QiblaCalculator
import com.example.widget.PrayerWidgetProvider
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.TimeZone

class MainActivity : ComponentActivity() {

    private lateinit var compassSensorManager: CompassSensorManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.createNotificationChannel(this)
        compassSensorManager = CompassSensorManager(this)

        setContent {
            MainApp(
                compassSensorManager = compassSensorManager,
                onScheduleAlarms = { settings ->
                    schedulePrayerAlarms(this, settings)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        compassSensorManager.startListening()
        PrayerWidgetProvider.updateAllWidgets(this)
    }

    override fun onPause() {
        super.onPause()
        compassSensorManager.stopListening()
    }

    companion object {
        fun schedulePrayerAlarms(context: Context, settings: UserSettings) {
            val calendar = Calendar.getInstance()
            val schedule = PrayerCalculator.calculate(
                calendar = calendar,
                latitude = settings.selectedCity.latitude,
                longitude = settings.selectedCity.longitude,
                timeZoneOffsetHours = settings.selectedCity.timeZoneOffsetHours,
                method = settings.calculationMethod,
                asrSchool = settings.asrSchool
            )

            val now = System.currentTimeMillis()
            for (item in schedule.items) {
                val shouldNotify = when (item.type) {
                    PrayerType.FAJR -> settings.notifyFajr
                    PrayerType.SUNRISE -> settings.notifySunrise
                    PrayerType.DHUHR -> settings.notifyDhuhr
                    PrayerType.ASR -> settings.notifyAsr
                    PrayerType.MAGHRIB -> settings.notifyMaghrib
                    PrayerType.ISHA -> settings.notifyIsha
                }

                if (shouldNotify && item.timeMillis > now) {
                    PrayerAlarmReceiver.scheduleAlarm(
                        context = context,
                        triggerMillis = item.timeMillis,
                        prayerType = item.type,
                        cityName = settings.selectedCity.nameRu,
                        muezzinVoice = settings.selectedMuezzin,
                        volume = settings.adhanVolume
                    )
                }
            }

            PrayerWidgetProvider.updateAllWidgets(context)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    compassSensorManager: CompassSensorManager,
    onScheduleAlarms: (UserSettings) -> Unit
) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(UserSettings()) }
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var currentTab by remember { mutableStateOf(0) } // 0: Намаз, 1: Кибла, 2: Коран, 3: Напоминания
    var isProfileOpen by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(0L) }

    // Compass state
    val compassData by compassSensorManager.compassData.collectAsState()

    // Real-time 1-second ticker for prayer countdown and time update
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            tick = System.currentTimeMillis()
        }
    }

    // Calculate prayer times
    val prayerSchedule = remember(
        selectedCalendar.get(Calendar.DAY_OF_YEAR),
        selectedCalendar.get(Calendar.YEAR),
        settings.selectedCity,
        settings.calculationMethod,
        settings.asrSchool,
        tick
    ) {
        PrayerCalculator.calculate(
            calendar = selectedCalendar,
            latitude = settings.selectedCity.latitude,
            longitude = settings.selectedCity.longitude,
            timeZoneOffsetHours = settings.selectedCity.timeZoneOffsetHours,
            method = settings.calculationMethod,
            asrSchool = settings.asrSchool
        )
    }

    // Calculate Qibla bearing
    val qiblaInfo = remember(settings.selectedCity) {
        val info = QiblaCalculator.calculateQibla(
            latitude = settings.selectedCity.latitude,
            longitude = settings.selectedCity.longitude
        )
        compassSensorManager.setTargetQiblaBearing(info.bearingDegrees)
        info
    }

    // Reschedule alarms when settings change
    LaunchedEffect(settings.selectedCity, settings.calculationMethod, settings.selectedMuezzin, settings.adhanVolume) {
        onScheduleAlarms(settings)
    }

    // Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            requestDeviceLocation(context) { newCity ->
                settings = settings.copy(selectedCity = newCity)
                Toast.makeText(context, "Геолокация определена: ${newCity.nameRu}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                context,
                "Разрешение не предоставлено. Вы можете выбрать город вручную в профиле.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Notification Permission Launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun requestLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            requestDeviceLocation(context) { newCity ->
                settings = settings.copy(selectedCity = newCity)
                Toast.makeText(context, "Геолокация: ${newCity.nameRu}", Toast.LENGTH_SHORT).show()
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    MyApplicationTheme(themeMode = settings.themeMode) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscapeOrExpanded = maxWidth > 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Нур Намаз",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${settings.selectedCity.nameRu} • ${prayerSchedule.dateHijriFormatted}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        actions = {
                            // Profile icon placed in the top right corner as requested
                            IconButton(
                                onClick = { isProfileOpen = true },
                                modifier = Modifier
                                    .testTag("profile_button")
                                    .padding(end = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Профиль и персональные настройки",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    if (!isLandscapeOrExpanded) {
                        NavigationBar(
                            modifier = Modifier.testTag("bottom_nav_bar"),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                icon = { Icon(Icons.Default.Schedule, contentDescription = "Намаз") },
                                label = { Text("Намаз") },
                                modifier = Modifier.testTag("nav_prayer")
                            )
                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                icon = { Icon(Icons.Default.Explore, contentDescription = "Кибла") },
                                label = { Text("Кибла") },
                                modifier = Modifier.testTag("nav_qibla")
                            )
                            NavigationBarItem(
                                selected = currentTab == 2,
                                onClick = { currentTab = 2 },
                                icon = { Icon(Icons.Default.AutoStories, contentDescription = "Коран") },
                                label = { Text("Коран") },
                                modifier = Modifier.testTag("nav_quran")
                            )
                            NavigationBarItem(
                                selected = currentTab == 3,
                                onClick = { currentTab = 3 },
                                icon = { Icon(Icons.Default.VolunteerActivism, contentDescription = "Напоминания") },
                                label = { Text("Зикр") },
                                modifier = Modifier.testTag("nav_reminders")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Navigation Rail for Tablets or Landscape mode
                    if (isLandscapeOrExpanded) {
                        NavigationRail(
                            modifier = Modifier.testTag("side_nav_rail"),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            NavigationRailItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                icon = { Icon(Icons.Default.Schedule, contentDescription = "Намаз") },
                                label = { Text("Намаз") }
                            )
                            NavigationRailItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                icon = { Icon(Icons.Default.Explore, contentDescription = "Кибла") },
                                label = { Text("Кибла") }
                            )
                            NavigationRailItem(
                                selected = currentTab == 2,
                                onClick = { currentTab = 2 },
                                icon = { Icon(Icons.Default.AutoStories, contentDescription = "Коран") },
                                label = { Text("Коран") }
                            )
                            NavigationRailItem(
                                selected = currentTab == 3,
                                onClick = { currentTab = 3 },
                                icon = { Icon(Icons.Default.VolunteerActivism, contentDescription = "Напоминания") },
                                label = { Text("Зикр") }
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentTab) {
                            0 -> PrayerTimesScreen(
                                schedule = prayerSchedule,
                                selectedCalendar = selectedCalendar,
                                settings = settings,
                                isLandscapeOrExpanded = isLandscapeOrExpanded,
                                onPreviousDay = {
                                    val newCal = selectedCalendar.clone() as Calendar
                                    newCal.add(Calendar.DAY_OF_YEAR, -1)
                                    selectedCalendar = newCal
                                },
                                onToday = {
                                    selectedCalendar = Calendar.getInstance()
                                },
                                onNextDay = {
                                    val newCal = selectedCalendar.clone() as Calendar
                                    newCal.add(Calendar.DAY_OF_YEAR, 1)
                                    selectedCalendar = newCal
                                },
                                onRequestLocation = { requestLocation() },
                                onOpenProfile = { isProfileOpen = true }
                            )
                            1 -> QiblaScreen(
                                compassData = compassData,
                                cityName = settings.selectedCity.nameRu,
                                qiblaInfo = qiblaInfo,
                                isLandscapeOrExpanded = isLandscapeOrExpanded
                            )
                            2 -> QuranScreen(
                                settings = settings,
                                onSettingsChanged = { settings = it },
                                isLandscapeOrExpanded = isLandscapeOrExpanded
                            )
                            3 -> RemindersScreen(
                                settings = settings,
                                isLandscapeOrExpanded = isLandscapeOrExpanded
                            )
                        }
                    }
                }
            }
        }

        // Profile Dialog
        if (isProfileOpen) {
            ProfileDialog(
                settings = settings,
                onSettingsChanged = { updated ->
                    settings = updated
                },
                onDismissRequest = { isProfileOpen = false }
            )
        }
    }
}

@SuppressLint("MissingPermission")
private fun requestDeviceLocation(context: Context, onCityFound: (CityLocation) -> Unit) {
    try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
            if (loc != null) {
                findNearestCity(loc.latitude, loc.longitude, onCityFound)
            } else {
                // Fallback to LocationManager
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val fallbackLoc = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (fallbackLoc != null) {
                    findNearestCity(fallbackLoc.latitude, fallbackLoc.longitude, onCityFound)
                }
            }
        }.addOnFailureListener {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val fallbackLoc = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (fallbackLoc != null) {
                findNearestCity(fallbackLoc.latitude, fallbackLoc.longitude, onCityFound)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun findNearestCity(lat: Double, lon: Double, onCityFound: (CityLocation) -> Unit) {
    var nearest = CityDatabase.CITIES.first()
    var minDistance = Double.MAX_VALUE

    for (c in CityDatabase.CITIES) {
        val dLat = lat - c.latitude
        val dLon = lon - c.longitude
        val distSq = dLat * dLat + dLon * dLon
        if (distSq < minDistance) {
            minDistance = distSq
            nearest = c
        }
    }

    val tz = TimeZone.getDefault()
    val offsetHours = tz.rawOffset / (1000.0 * 3600.0)

    val detectedCity = CityLocation(
        nameRu = nearest.nameRu,
        countryRu = nearest.countryRu,
        latitude = lat,
        longitude = lon,
        timeZoneOffsetHours = offsetHours
    )
    onCityFound(detectedCity)
}
