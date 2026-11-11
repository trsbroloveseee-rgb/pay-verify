package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkFintechColorScheme = darkColorScheme(
    primary = Emerald400,
    onPrimary = Slate950,
    primaryContainer = Emerald900,
    onPrimaryContainer = Emerald400,
    secondary = BkashPink,
    onSecondary = Color.White,
    secondaryContainer = BkashPinkSoft,
    onSecondaryContainer = Color(0xFFFFB1C8),
    tertiary = Cyan400,
    onTertiary = Slate950,
    background = Slate950,
    onBackground = Slate50,
    surface = Slate900,
    onSurface = Slate50,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    error = Rose400,
    onError = Slate950,
    errorContainer = Rose900,
    onErrorContainer = Rose400
)

private val LightFintechColorScheme = lightColorScheme(
    primary = Emerald500,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Emerald900,
    secondary = BkashPink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE7F3),
    onSecondaryContainer = Color(0xFF831843),
    tertiary = Indigo500,
    onTertiary = Color.White,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate200,
    onSurfaceVariant = Slate700,
    outline = Slate400,
    error = Rose500,
    onError = Color.White
)

@Composable
fun PayPulseTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkFintechColorScheme else LightFintechColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
