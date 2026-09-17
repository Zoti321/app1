package com.example.mynativeapp1.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = SkyPrimary,
    onPrimary = SkyOnPrimary,
    secondary = SkySecondary,
    onSecondary = SkyOnSecondary,
    tertiary = SkyTertiary,
    onTertiary = SkyOnTertiary,
    background = SkyBackground,
    onBackground = SkyOnBackground,
    surface = SkySurface,
    onSurface = SkyOnSurface,
    onSurfaceVariant = SkyOnSurfaceVariant,
    surfaceVariant = SkySurfaceVariant,
    outline = SkyOutline,
)

private val DarkColorScheme = darkColorScheme(
    primary = SkyPrimaryDark,
    onPrimary = SkyOnPrimaryDark,
    secondary = SkySecondaryDark,
    onSecondary = SkyOnSecondaryDark,
    tertiary = SkyTertiaryDark,
    onTertiary = SkyOnTertiaryDark,
    background = SkyBackgroundDark,
    onBackground = SkyOnBackgroundDark,
    surface = SkySurfaceDark,
    onSurface = SkyOnSurfaceDark,
    onSurfaceVariant = SkyOnSurfaceVariantDark,
    surfaceVariant = SkySurfaceVariantDark,
    outline = SkyOutlineDark,
)

@Composable
fun MyNativeApp1Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
