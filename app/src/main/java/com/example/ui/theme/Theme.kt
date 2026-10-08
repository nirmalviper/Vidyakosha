package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EditorialPrimaryDark,
    onPrimary = EditorialOnPrimaryDark,
    primaryContainer = EditorialPrimaryContainerDark,
    onPrimaryContainer = EditorialOnPrimaryContainerDark,
    secondary = EditorialSecondaryDark,
    onSecondary = EditorialOnSecondaryDark,
    secondaryContainer = EditorialSecondaryContainerDark,
    onSecondaryContainer = EditorialOnSecondaryContainerDark,
    tertiary = EditorialTertiaryDark,
    onTertiary = EditorialOnTertiaryDark,
    tertiaryContainer = EditorialTertiaryContainerDark,
    onTertiaryContainer = EditorialOnTertiaryContainerDark,
    background = EditorialDarkBg,
    onBackground = AlabasterInkDark,
    surface = EditorialSurfaceDark,
    onSurface = AlabasterInkDark,
    surfaceVariant = EditorialSurfaceContainerDark,
    onSurfaceVariant = WarmStoneSecondaryDark,
    surfaceContainerLowest = EditorialDarkBg,
    surfaceContainerLow = EditorialSurfaceLowDark,
    surfaceContainer = EditorialSurfaceContainerDark,
    surfaceContainerHigh = EditorialSurfaceHighDark,
    surfaceContainerHighest = EditorialSurfaceHighestDark,
    outline = EditorialOutlineDark,
    outlineVariant = EditorialOutlineVariantDark,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF492522),
    onErrorContainer = Color(0xFFF9DEDC)
)

private val LightColorScheme = lightColorScheme(
    primary = EditorialPrimaryLight,
    onPrimary = EditorialOnPrimaryLight,
    primaryContainer = EditorialPrimaryContainerLight,
    onPrimaryContainer = EditorialOnPrimaryContainerLight,
    secondary = EditorialSecondaryLight,
    onSecondary = EditorialOnSecondaryLight,
    secondaryContainer = EditorialSecondaryContainerLight,
    onSecondaryContainer = EditorialOnSecondaryContainerLight,
    tertiary = EditorialTertiaryLight,
    onTertiary = EditorialOnTertiaryLight,
    tertiaryContainer = EditorialTertiaryContainerLight,
    onTertiaryContainer = EditorialOnTertiaryContainerLight,
    background = EditorialPaperBg,
    onBackground = CharcoalInkPrimary,
    surface = EditorialSurfaceLight,
    onSurface = CharcoalInkPrimary,
    surfaceVariant = EditorialSurfaceLowLight,
    onSurfaceVariant = WarmStoneSecondaryText,
    surfaceContainerLowest = EditorialSurfaceLight,
    surfaceContainerLow = EditorialSurfaceLowLight,
    surfaceContainer = EditorialSurfaceContainerLight,
    surfaceContainerHigh = EditorialSurfaceHighLight,
    surfaceContainerHighest = EditorialSurfaceHighestLight,
    outline = EditorialOutlineLight,
    outlineVariant = EditorialOutlineVariantLight,
    error = Color(0xFF991B1B),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
