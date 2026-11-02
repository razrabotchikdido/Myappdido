package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = DarkBg,
    primaryContainer = EmeraldPrimary,
    onPrimaryContainer = Color.White,
    secondary = GoldAccent,
    onSecondary = DarkBg,
    secondaryContainer = GoldDark,
    onSecondaryContainer = GoldLight,
    tertiary = EmeraldMint,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = EmeraldDark,
    secondary = GoldDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = EmeraldLight,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary
)

private val SepiaColorScheme = lightColorScheme(
    primary = SepiaAccent,
    onPrimary = Color.White,
    primaryContainer = SepiaSurfaceElevated,
    onPrimaryContainer = SepiaTextPrimary,
    secondary = GoldDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEBDDC9),
    onSecondaryContainer = SepiaTextPrimary,
    tertiary = EmeraldPrimary,
    background = SepiaBg,
    onBackground = SepiaTextPrimary,
    surface = SepiaSurface,
    onSurface = SepiaTextPrimary,
    surfaceVariant = SepiaSurfaceElevated,
    onSurfaceVariant = SepiaTextSecondary
)

private val NightOledColorScheme = darkColorScheme(
    primary = NightOledAccent,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF261D15),
    onPrimaryContainer = NightOledTextPrimary,
    secondary = EmeraldLight,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0F2B20),
    onSecondaryContainer = EmeraldMint,
    tertiary = GoldLight,
    background = NightOledBg,
    onBackground = NightOledTextPrimary,
    surface = NightOledSurface,
    onSurface = NightOledTextPrimary,
    surfaceVariant = NightOledSurfaceElevated,
    onSurfaceVariant = NightOledTextSecondary
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = when (themeMode) {
        AppThemeMode.SYSTEM -> if (systemDark) DarkColorScheme else LightColorScheme
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.SEPIA -> SepiaColorScheme
        AppThemeMode.NIGHT -> NightOledColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
