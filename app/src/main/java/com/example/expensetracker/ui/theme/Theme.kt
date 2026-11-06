package com.example.expensetracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Light scheme: my own colours (used when dynamic colour is not available)
private val LightColors = lightColorScheme(
    primary = Brand40,
    onPrimary = Color.White,
    primaryContainer = Brand90,
    onPrimaryContainer = Brand10,
    secondary = Accent40,
    onSecondary = Color.White,
    secondaryContainer = Accent90,
    onSecondaryContainer = Accent10,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    error = Error40,
    onError = Color.White
)

// Dark scheme: my own colours (used when dynamic colour is not available)
private val DarkColors = darkColorScheme(
    primary = Brand80,
    onPrimary = Brand20,
    primaryContainer = Brand30,
    onPrimaryContainer = Brand90,
    secondary = Accent80,
    onSecondary = Accent20,
    secondaryContainer = Accent30,
    onSecondaryContainer = Accent90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    error = Error80,
    onError = Error20
)

@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        // Android 12+ : wallpaper-based colours
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Older phones: my own colours
        darkTheme -> DarkColors
        else -> LightColors
    }

    // typography = default Material 3 type scale (MaterialTheme.typography)
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
