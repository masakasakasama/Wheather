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
import com.example.weather.data.model.today
import com.example.weather.data.model.weatherIcon
import com.example.weather.ui.formatHourMinute
import com.example.weather.ui.nextRainText

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
        val snapshot = AppServices.cache.readSnapshotOnce()
        provideContent {
            WeatherWidgetContent(snapshot)
        }
    }
}

class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()
}

@androidx.compose.runtime.Composable
private fun WeatherWidgetContent(snapshot: WeatherSnapshot?) {
    val size = LocalSize.current
    val modifier = GlanceModifier
        .fillMaxSize()
        .background(ColorProvider(Color(0xFF050505)))
        .clickable(actionStartActivity<MainActivity>())
        .padding(12.dp)

    when {
        snapshot == null -> EmptyWidget(modifier)
        size.width < 180.dp -> SmallWidget(snapshot, modifier)
        size.height < 160.dp -> MediumWidget(snapshot, modifier)
        else -> LargeWidget(snapshot, modifier)
    }
}

@androidx.compose.runtime.Composable
private fun EmptyWidget(modifier: GlanceModifier) {
    Column(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("個人天気", style = widgetText(15, bold = true))
        Spacer(GlanceModifier.height(6.dp))
        Text("アプリを開いて更新", style = widgetText(11, muted = true))
    }
}

@androidx.compose.runtime.Composable
private fun SmallWidget(snapshot: WeatherSnapshot, modifier: GlanceModifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${snapshot.current.temperatureC?.roundText() ?: "--"}°", style = widgetText(28, bold = true))
            Spacer(GlanceModifier.width(8.dp))
            Text(weatherIcon(snapshot.current.weatherCode), style = widgetText(18))
        }
        Text("降水 ${snapshot.today()?.maxPrecipitationProbability ?: "--"}%", style = widgetText(12, muted = true))
        Text(nextRainText(snapshot), style = widgetText(11, muted = true), maxLines = 1)
    }
}

@androidx.compose.runtime.Composable
private fun MediumWidget(snapshot: WeatherSnapshot, modifier: GlanceModifier) {
    Column(modifier) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${snapshot.current.temperatureC?.roundText() ?: "--"}°", style = widgetText(32, bold = true))
            Spacer(GlanceModifier.width(10.dp))
            Text("H ${snapshot.today()?.maxTemperatureC?.roundText() ?: "--"}° / L ${snapshot.today()?.minTemperatureC?.roundText() ?: "--"}°", style = widgetText(12, muted = true))
        }
        Spacer(GlanceModifier.height(6.dp))
        Text(snapshot.hourly.take(6).joinToString(" ") { "${it.precipitationProbability ?: 0}%" }, style = widgetText(12))
        Text(snapshot.hourly.take(6).joinToString(" ") { "${it.temperatureC?.roundText() ?: "--"}°" }, style = widgetText(12, muted = true))
        Text("更新 ${formatHourMinute(snapshot.updatedAtMillis)}", style = widgetText(10, muted = true))
    }
}

@androidx.compose.runtime.Composable
private fun LargeWidget(snapshot: WeatherSnapshot, modifier: GlanceModifier) {
    Column(modifier) {
        MediumWidget(snapshot, GlanceModifier.fillMaxWidth())
        Spacer(GlanceModifier.height(8.dp))
        Text(snapshot.daily.take(3).joinToString("  ") { "${it.date.takeLast(5)} ${weatherIcon(it.weatherCode)} ${it.maxTemperatureC?.roundText() ?: "--"}/${it.minTemperatureC?.roundText() ?: "--"}°" }, style = widgetText(12))
        Text("今日明日 ${snapshot.hourly.take(24).mapNotNull { it.precipitationProbability }.maxOrNull() ?: 0}% / ${snapshot.hourly.drop(24).take(24).mapNotNull { it.precipitationProbability }.maxOrNull() ?: 0}%", style = widgetText(11, muted = true))
    }
}

private fun widgetText(size: Int, bold: Boolean = false, muted: Boolean = false): TextStyle {
    return TextStyle(
        color = ColorProvider(if (muted) Color(0xFFB8B8B8) else Color(0xFFF4F4F4)),
        fontSize = size.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    )
}

private fun Double.roundText(): String = "%.0f".format(this)
