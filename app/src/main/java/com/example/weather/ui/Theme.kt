package com.example.weather.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Light, airy palette aligned with the refreshed mobile weather experience. */
object WeatherPalette {
    val Background = Color(0xFFF4F8FC)
    val Header = Color(0xFFFFFFFF)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceElevated = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFEAF1F8)
    val ForecastSurface = Color(0xFFFFFFFF)
    val Outline = Color(0xFFDDE6EF)

    val Primary = Color(0xFF2F7AF8)
    val Secondary = Color(0xFF4C8FFB)
    val Tertiary = Color(0xFFEE8A2B)
    val Accent = Color(0xFF6D93D8)
    val Rain = Color(0xFF2F7AF8)
    val HighTemperature = Color(0xFFF15A4A)
    val LowTemperature = Color(0xFF6D93D8)

    val OnSurface = Color(0xFF10233A)
    val OnSurfaceVariant = Color(0xFF6C7C8E)

    val Danger = Color(0xFFD94B4B)
    val Warning = Color(0xFFE88A22)
    val Good = Color(0xFF2EA66B)
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
        colorScheme = lightColorScheme(
            background = WeatherPalette.Background,
            surface = WeatherPalette.Surface,
            surfaceVariant = WeatherPalette.SurfaceVariant,
            primary = WeatherPalette.Primary,
            onPrimary = Color.White,
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
        isNight -> listOf(Color(0xFF21365D), Color(0xFF101C38))
        weatherCode == 0 -> listOf(Color(0xFF65B4F2), Color(0xFFD9ECFF))
        weatherCode in listOf(1, 2) -> listOf(Color(0xFF72B3E8), Color(0xFFD3E7F9))
        weatherCode == 3 -> listOf(Color(0xFF91A9BE), Color(0xFFDCE7F0))
        weatherCode in listOf(45, 48) -> listOf(Color(0xFF9EADB9), Color(0xFFE0E8EE))
        weatherCode in listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82) ->
            listOf(Color(0xFF6F91B5), Color(0xFFD2E2F0))
        weatherCode in listOf(71, 73, 75, 77, 85, 86) -> listOf(Color(0xFFA7BACD), Color(0xFFE7F0F7))
        weatherCode in listOf(95, 96, 99) -> listOf(Color(0xFF626C87), Color(0xFFC9D0DF))
        else -> listOf(Color(0xFF65B4F2), Color(0xFFD9ECFF))
    }
    return Brush.linearGradient(
        colors = colors,
        start = Offset(0f, 0f),
        end = Offset(900f, 1400f),
    )
}
