package com.example.swiftlab.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueDark,
    secondary = SecondaryTeal,
    onSecondary = Color.White,
    background = Slate900,
    surface = Slate800,
    onBackground = Slate50,
    onSurface = Slate50
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = PrimaryBlueDark,
    secondary = SecondaryTeal,
    onSecondary = Color.White,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = Slate900,
    onSurface = Slate900,
    surfaceVariant = SurfaceVariantLight
)

@Composable
fun SwiftLabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
