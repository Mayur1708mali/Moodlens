package com.example.moodlens.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Indigo80,
    onPrimary = Indigo20,
    primaryContainer = Indigo40,
    onPrimaryContainer = DarkOnSurface,
    secondary = Emerald80,
    onSecondary = Emerald20,
    secondaryContainer = Emerald40,
    onSecondaryContainer = DarkOnSurface,
    tertiary = Amber80,
    onTertiary = Amber20,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    background = DarkBackground,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    onBackground = DarkOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo40,
    onPrimary = LightSurface,
    primaryContainer = Indigo80.copy(alpha = 0.35f),
    onPrimaryContainer = Indigo20,
    secondary = Emerald40,
    onSecondary = LightSurface,
    secondaryContainer = Emerald80.copy(alpha = 0.35f),
    onSecondaryContainer = Emerald20,
    tertiary = Amber40,
    onTertiary = LightSurface,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    background = LightBackground,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    onBackground = LightOnSurface
)

@Composable
fun MoodlensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
