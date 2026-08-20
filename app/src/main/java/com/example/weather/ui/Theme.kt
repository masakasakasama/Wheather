package com.example.weather.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Dark-first palette aligned with the refreshed mobile weather experience. */
object WeatherPalette {
    val Background = Color(0xFF0B0F14)
    val Header = Color(0xFF111821)
    val Surface = Color(0xFF111821)
    val SurfaceElevated = Color(0xFF151D27)
    val SurfaceVariant = Color(0xFF1B2633)
    val ForecastSurface = Color(0xFF121A24)
    val Outline = Color(0xFF2A3948)

    val Primary = Color(0xFF5BA2FF)
    val Secondary = Color(0xFF7CB8FF)
    val Tertiary = Color(0xFFFFB15C)
    val Accent = Color(0xFF88AEEB)
    val Rain = Color(0xFF5BA2FF)
    val HighTemperature = Color(0xFFFF7D70)
    val LowTemperature = Color(0xFF8FB5FF)

    val OnSurface = Color(0xFFF2F7FC)
    val OnSurfaceVariant = Color(0xFF9BAFC2)

    val Danger = Color(0xFFFF6B6B)
    val Warning = Color(0xFFFFB454)
    val Good = Color(0xFF55D58A)
}

val WeatherShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun WeatherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = WeatherPalette.Background,
            surface = WeatherPalette.Surface,
            surfaceVariant = WeatherPalette.SurfaceVariant,
            primary = WeatherPalette.Primary,
            onPrimary = Color(0xFF071522),
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

/** Weather-aware gradient retained for cards and future surfaces. */
fun skyGradient(weatherCode: Int?, isNight: Boolean): Brush {
    val colors = when {
        isNight -> listOf(Color(0xFF172C4A), Color(0xFF090F1B))
        weatherCode == 0 -> listOf(Color(0xFF173A5C), Color(0xFF0D1E32))
        weatherCode in listOf(1, 2) -> listOf(Color(0xFF18364F), Color(0xFF0D1B2A))
        weatherCode == 3 -> listOf(Color(0xFF273746), Color(0xFF111A22))
        weatherCode in listOf(45, 48) -> listOf(Color(0xFF303941), Color(0xFF151A1F))
        weatherCode in listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82) ->
            listOf(Color(0xFF17314B), Color(0xFF0D1824))
        weatherCode in listOf(71, 73, 75, 77, 85, 86) -> listOf(Color(0xFF29435A), Color(0xFF12202B))
        weatherCode in listOf(95, 96, 99) -> listOf(Color(0xFF2F3147), Color(0xFF11121D))
        else -> listOf(Color(0xFF173A5C), Color(0xFF0D1E32))
    }
    return Brush.linearGradient(
        colors = colors,
        start = Offset(0f, 0f),
        end = Offset(900f, 1400f),
    )
}
