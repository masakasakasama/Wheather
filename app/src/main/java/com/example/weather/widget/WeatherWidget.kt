package com.example.weather.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.weather.AppServices
import com.example.weather.MainActivity
import com.example.weather.data.model.WeatherSnapshot
import com.example.weather.data.model.effectiveCurrentWeatherCode
import com.example.weather.data.model.effectiveMaxProbability
import com.example.weather.data.model.effectivePrecipitationSum
import com.example.weather.data.model.forecastDays
import com.example.weather.data.model.today
import com.example.weather.data.model.weatherIcon
import com.example.weather.data.model.weatherLabel
import com.example.weather.ui.formatHourMinute
import com.example.weather.ui.nextRainText
import com.example.weather.ui.nextHours
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class WeatherWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 72.dp),
            DpSize(220.dp, 110.dp),
            DpSize(320.dp, 180.dp),
        ),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        AppServices.init(context)
        val weather = AppServices.cache.readWidgetWeatherOnce()
        provideContent {
            WeatherWidgetContent(weather.selectedLocation.name, weather.snapshot)
        }
    }
}

class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()
}

class WeatherSquareWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        AppServices.init(context)
        val weather = AppServices.cache.readWidgetWeatherOnce()
        provideContent {
            WeatherSquareWidgetContent(weather.selectedLocation.name, weather.snapshot)
        }
    }
}

class WeatherSquareWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherSquareWidget()
}

@androidx.compose.runtime.Composable
private fun WeatherWidgetContent(selectedLocationName: String, snapshot: WeatherSnapshot?) {
    val size = LocalSize.current
    val modifier = GlanceModifier
        .fillMaxSize()
        .background(ColorProvider(Color(0xFF050505)))
        .clickable(actionStartActivity<MainActivity>())
        .padding(12.dp)

    when {
        snapshot == null -> EmptyWidget(selectedLocationName, modifier)
        size.width < 180.dp -> SmallWidget(snapshot, modifier)
        size.height < 160.dp -> MediumWidget(snapshot, modifier)
        else -> LargeWidget(snapshot, modifier)
    }
}

@androidx.compose.runtime.Composable
private fun EmptyWidget(selectedLocationName: String, modifier: GlanceModifier) {
    Column(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("📍 ${selectedLocationName.substringBefore(" (")}", style = widgetText(15, bold = true), maxLines = 1)
        Spacer(GlanceModifier.height(6.dp))
        Text("この地点の天気を取得中", style = widgetText(11, muted = true))
    }
}

@androidx.compose.runtime.Composable
private fun WeatherSquareWidgetContent(selectedLocationName: String, snapshot: WeatherSnapshot?) {
    val size = LocalSize.current
    val modifier = GlanceModifier
        .fillMaxSize()
        .background(ColorProvider(Color(0xFF121416)))
        .clickable(actionStartActivity<MainActivity>())
        .padding(horizontal = 9.dp, vertical = 7.dp)
    if (snapshot == null) {
        EmptyWidget(selectedLocationName, modifier)
        return
    }

    val compact = size.height < 135.dp
    val days = snapshot.forecastDays().take(2)
    val dayWidth = ((size.width.value - 18f) / 2f).dp
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "📍 ${snapshot.location.name.substringBefore(" (")}",
            style = widgetText(if (compact) 11 else 13, bold = true),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(if (compact) 2.dp else 4.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            days.forEachIndexed { index, day ->
                val dayHours = snapshot.hourly.filter { it.time.take(10) == day.date }
                SquareForecastDay(
                    relativeLabel = if (index == 0) "今日" else "明日",
                    date = day.date,
                    icon = weatherIcon(day.weatherCode),
                    weatherName = weatherLabel(day.weatherCode),
                    high = day.maxTemperatureC,
                    low = day.minTemperatureC,
                    probability = day.effectiveMaxProbability(dayHours),
                    compact = compact,
                    modifier = GlanceModifier.width(dayWidth),
                )
            }
        }
        if (!compact) {
            Spacer(GlanceModifier.height(3.dp))
            Text(
                "更新 ${formatHourMinute(snapshot.updatedAtMillis)}",
                style = widgetText(9, muted = true),
                maxLines = 1,
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun SquareForecastDay(
    relativeLabel: String,
    date: String,
    icon: String,
    weatherName: String,
    high: Double?,
    low: Double?,
    probability: Int?,
    compact: Boolean,
    modifier: GlanceModifier,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "$relativeLabel ${formatWidgetDate(date)}",
            style = widgetText(if (compact) 10 else 12, bold = true),
            maxLines = 1,
        )
        Text(icon, style = widgetText(if (compact) 32 else 43), maxLines = 1)
        if (!compact) {
            Text(
                weatherName,
                style = widgetText(10, bold = true),
                maxLines = 1,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${high?.roundText() ?: "--"}°",
                style = widgetText(if (compact) 13 else 16, bold = true, color = Color(0xFFFF665E)),
            )
            Spacer(GlanceModifier.width(5.dp))
            Text(
                "${low?.roundText() ?: "--"}°",
                style = widgetText(if (compact) 13 else 16, bold = true, color = Color(0xFF6FA8FF)),
            )
        }
        Text(
            "💧 ${probability.percentText()}",
            style = widgetText(if (compact) 11 else 14),
            maxLines = 1,
        )
    }
}

@androidx.compose.runtime.Composable
private fun SmallWidget(snapshot: WeatherSnapshot, modifier: GlanceModifier) {
    val today = snapshot.today()
    val todayHours = today?.let { day ->
        snapshot.hourly.filter { it.time.take(10) == day.date }
    }.orEmpty()
    val currentWeatherCode = snapshot.effectiveCurrentWeatherCode()
    Column(modifier) {
        Text(
            "📍 ${snapshot.location.name.substringBefore(" (")}",
            style = widgetText(10, muted = true),
            maxLines = 1,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${snapshot.current.temperatureC?.roundText() ?: "--"}°", style = widgetText(28, bold = true))
            Spacer(GlanceModifier.width(8.dp))
            Text(weatherIcon(currentWeatherCode), style = widgetText(18))
        }
        Text("降水 ${today.effectiveMaxProbability(todayHours).percentText()} / ${today.effectivePrecipitationSum(todayHours).mmText()}", style = widgetText(12, muted = true))
        Text(nextRainText(snapshot), style = widgetText(11, muted = true), maxLines = 1)
    }
}

@androidx.compose.runtime.Composable
private fun MediumWidget(snapshot: WeatherSnapshot, modifier: GlanceModifier) {
    val hours = snapshot.hourly.nextHours(6, snapshot.timezone)
    Column(modifier) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "📍 ${snapshot.location.name.substringBefore(" (")}",
                style = widgetText(11, bold = true),
                maxLines = 1,
            )
            Spacer(GlanceModifier.width(8.dp))
            Text("更新 ${formatHourMinute(snapshot.updatedAtMillis)}", style = widgetText(9, muted = true))
        }
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${snapshot.current.temperatureC?.roundText() ?: "--"}°", style = widgetText(32, bold = true))
            Spacer(GlanceModifier.width(10.dp))
            Text("H ${snapshot.today()?.maxTemperatureC?.roundText() ?: "--"}° / L ${snapshot.today()?.minTemperatureC?.roundText() ?: "--"}° / AQI ${snapshot.airQuality?.europeanAqi?.toString() ?: "--"}", style = widgetText(12, muted = true))
        }
        Spacer(GlanceModifier.height(6.dp))
        Text(hours.joinToString(" ") { it.precipitationProbability.percentText() }, style = widgetText(12))
        Text(hours.joinToString(" ") { "${it.temperatureC?.roundText() ?: "--"}°" }, style = widgetText(12, muted = true))
    }
}

@androidx.compose.runtime.Composable
private fun LargeWidget(snapshot: WeatherSnapshot, modifier: GlanceModifier) {
    val hours = snapshot.hourly.nextHours(48, snapshot.timezone)
    val todayMax = hours.take(24).mapNotNull { it.precipitationProbability }.maxOrNull()
    val tomorrowMax = hours.drop(24).take(24).mapNotNull { it.precipitationProbability }.maxOrNull()
    Column(modifier) {
        MediumWidget(snapshot, GlanceModifier.fillMaxWidth())
        Spacer(GlanceModifier.height(8.dp))
        Text(
            snapshot.forecastDays().take(3).joinToString("  ") { day ->
                val dayHours = snapshot.hourly.filter { it.time.take(10) == day.date }
                "${day.date.takeLast(5)} ${weatherIcon(day.weatherCode)} ${day.maxTemperatureC?.roundText() ?: "--"}/${day.minTemperatureC?.roundText() ?: "--"}° ${day.effectivePrecipitationSum(dayHours).mmText()}"
            },
            style = widgetText(12),
        )
        Text(
            "今後48h ${todayMax.percentText()} / ${tomorrowMax.percentText()}",
            style = widgetText(11, muted = true),
        )
    }
}

private fun widgetText(
    size: Int,
    bold: Boolean = false,
    muted: Boolean = false,
    color: Color? = null,
): TextStyle {
    return TextStyle(
        color = ColorProvider(color ?: if (muted) Color(0xFFB8B8B8) else Color(0xFFF4F4F4)),
        fontSize = size.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    )
}

private fun formatWidgetDate(date: String): String =
    runCatching {
        LocalDate.parse(date).format(DateTimeFormatter.ofPattern("M/d(E)", Locale.JAPANESE))
    }.getOrDefault(date.takeLast(5))

private fun Double.roundText(): String = "%.0f".format(this)
private fun Double.oneDecimal(): String = "%.1f".format(this)
private fun Double?.mmText(): String = this?.let { "${it.oneDecimal()}mm" } ?: "--mm"
private fun Int?.percentText(): String = this?.let { "$it%" } ?: "--%"
