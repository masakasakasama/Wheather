package com.example.weather.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

/**
 * Central design tokens for the app. Keeping the palette and gradients in one
 * place makes it easy to keep every screen visually consistent.
 */
object WeatherPalette {
    val Background = Color(0xFF0A0E16)
    val Surface = Color(0xFF141926)
    val SurfaceElevated = Color(0xFF1A2030)
    val SurfaceVariant = Color(0xFF222A3B)
    val Outline = Color(0xFF2C3447)

    val Primary = Color(0xFF5AC8FA)
    val Secondary = Color(0xFF64D2FF)
    val Tertiary = Color(0xFFFFC95C)
    val Accent = Color(0xFF8E97FD)

    val OnSurface = Color(0xFFF4F6FB)
    val OnSurfaceVariant = Color(0xFFA7B0C4)

    val Danger = Color(0xFFFF7A85)
    val Warning = Color(0xFFFFB74D)
    val Good = Color(0xFF6FE3A0)
}

val WeatherShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(20.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun WeatherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = WeatherPalette.Background,
            surface = WeatherPalette.Surface,
            surfaceVariant = WeatherPalette.SurfaceVariant,
            primary = WeatherPalette.Primary,
            onPrimary = Color(0xFF052235),
            secondary = WeatherPalette.Secondary,
            tertiary = WeatherPalette.Tertiary,
            onBackground = WeatherPalette.OnSurface,
            onSurface = WeatherPalette.OnSurface,
            onSurfaceVariant = WeatherPalette.OnSurfaceVariant,
            outline = WeatherPalette.Outline,
            error = WeatherPalette.Danger,
        ),
        shapes = WeatherShapes,
        content = content,
    )
}

/**
 * A diagonal gradient that reflects the current sky: time of day plus the
 * dominant weather condition. Used behind the hero "current weather" card.
 */
fun skyGradient(weatherCode: Int?, isNight: Boolean): Brush {
    val colors = when {
        isNight -> listOf(Color(0xFF1B2440), Color(0xFF0E1530))
        weatherCode == 0 -> listOf(Color(0xFF2E8BE6), Color(0xFF6FB7FF))
        weatherCode in listOf(1, 2) -> listOf(Color(0xFF3D7CC4), Color(0xFF5C97D6))
        weatherCode == 3 -> listOf(Color(0xFF394A60), Color(0xFF566379))
        weatherCode in listOf(45, 48) -> listOf(Color(0xFF454F5E), Color(0xFF5B6472))
        weatherCode in listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82) ->
            listOf(Color(0xFF2B3D52), Color(0xFF3E5872))
        weatherCode in listOf(71, 73, 75, 77, 85, 86) -> listOf(Color(0xFF4A5C73), Color(0xFF6E8197))
        weatherCode in listOf(95, 96, 99) -> listOf(Color(0xFF2A3140), Color(0xFF45364F))
        else -> listOf(Color(0xFF2E8BE6), Color(0xFF6FB7FF))
    }
    return Brush.linearGradient(
        colors = colors,
        start = Offset(0f, 0f),
        end = Offset(900f, 1400f),
    )
}
