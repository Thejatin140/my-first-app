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
    primary = ImmersivePrimary,
    onPrimary = ImmersiveOnPrimary,
    secondary = ImmersiveSurfaceVariant,
    onSecondary = ImmersiveOnSurface,
    background = ImmersiveBackground,
    onBackground = ImmersiveText,
    surface = ImmersiveSurface,
    onSurface = ImmersiveOnSurface,
    surfaceVariant = ImmersiveSurfaceVariant,
    onSurfaceVariant = ImmersiveOnSurfaceVariant,
    primaryContainer = ImmersivePrimaryContainer,
    onPrimaryContainer = ImmersiveOnPrimaryContainer,
    secondaryContainer = ImmersiveSecondaryContainer,
    onSecondaryContainer = ImmersiveOnSecondaryContainer
  )

private val LightColorScheme = DarkColorScheme // Mirror the premium dark Immersive Theme in light option as well to preserve UI atmosphere

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to true for the dark premium immersive experience
  dynamicColor: Boolean = false, // Set dynamic coloring to false by default so our custom design colors are fully visible
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme


  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
