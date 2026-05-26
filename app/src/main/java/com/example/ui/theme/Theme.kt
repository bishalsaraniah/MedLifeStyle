package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = MedicalPrimaryDark,
    secondary = MedicalSecondaryDark,
    tertiary = MedicalTertiaryDark,
    background = SurfaceDark,
    surface = Color(0xFF1F2128),
    error = EmergencyRedDark,
    onPrimary = Color(0xFF00344F),
    onSecondary = Color(0xFF1E2429),
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    primaryContainer = Color(0xFF004B6D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    surfaceVariant = Color(0xFF282B33),
    onSurfaceVariant = Color(0xFFC4C7CF),
    outline = Color(0xFF8E9199)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PolishPrimary,
    secondary = PolishSecondary,
    tertiary = PolishTertiary,
    background = PolishBackground,
    surface = PolishSurface,
    error = EmergencyRed,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = PolishOnBackground,
    onSurface = PolishOnSurface,
    primaryContainer = PolishPrimaryContainer,
    onPrimaryContainer = PolishOnPrimaryContainer,
    surfaceVariant = PolishSurfaceVariant,
    onSurfaceVariant = PolishOnSurfaceVariant,
    outline = PolishOutline
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disabling dynamic colors to strictly enforce the "Professional Polish" design style
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
