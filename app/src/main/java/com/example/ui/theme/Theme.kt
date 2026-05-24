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
    surface = SurfaceDark,
    error = EmergencyRedDark,
    onPrimary = SurfaceDark,
    onSecondary = SurfaceDark,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    primaryContainer = PolishPrimaryContainer,
    onPrimaryContainer = PolishOnPrimaryContainer,
    surfaceVariant = PolishSurfaceVariant,
    onSurfaceVariant = PolishOnSurfaceVariant,
    outline = PolishOutline
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
