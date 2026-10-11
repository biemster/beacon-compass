package com.beaconcompass.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val TacticalColorScheme = darkColorScheme(
  primary = AmberPrimary,
  secondary = AmberSecondary,
  tertiary = AlertGreen,
  background = PitchBlack,
  surface = DarkGrey,
  onPrimary = PitchBlack,
  onSecondary = PitchBlack,
  onTertiary = PitchBlack,
  onBackground = DimWhite,
  onSurface = DimWhite,
  error = AlertRed,
  onError = PitchBlack
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme to preserve battery and support night vision
  dynamicColor: Boolean = false, // Turn off dynamic colors so we get custom aviation-style tactical amber
  content: @Composable () -> Unit,
) {
  // Use TacticalColorScheme to comply with tactical wilderness usage guidelines
  MaterialTheme(
    colorScheme = TacticalColorScheme,
    typography = Typography,
    content = content
  )
}
