package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = SophisticatedPrimary,
    secondary = SophisticatedSecondary,
    tertiary = SophisticatedTertiary,
    background = SophisticatedDarkBg,
    surface = SophisticatedSurface,
    surfaceVariant = SophisticatedSurfaceVariant,
    onPrimary = SophisticatedOnPrimary,
    onBackground = SophisticatedOnBg,
    onSurface = SophisticatedOnSurface,
    outline = SophisticatedBorder
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SophisticatedPrimary,
    secondary = SophisticatedSecondary,
    tertiary = SophisticatedTertiary,
    background = SophisticatedOnBg,
    surface = SophisticatedOnSurface,
    onPrimary = SophisticatedOnPrimary,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force Dark Theme for a premium security vault aesthetic
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current

  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
