package com.example.weather.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weather.data.model.HourlyWeather
import com.example.weather.data.model.WeatherSnapshot
import com.example.weather.data.model.weatherIcon
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun HourlyScreen(snapshot: WeatherSnapshot?) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("時間別天気", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (snapshot == null) {
            Text("データがありません", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        val today = LocalDate.now(ZoneId.of("Asia/Tokyo"))
        val hours = snapshot.hourly.filter {
            val date = runCatching { LocalDateTime.parse(it.time).toLocalDate() }.getOrNull()
            date == today || date == today.plusDays(1)
        }.take(48)

        Text("黄緑: 気温 / 青: 降水確率", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Row(Modifier.horizontalScroll(rememberScrollState())) {
            Column {
                HourlyGraph(hours)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    hours.forEach { hour ->
                        HourCell(hour)
                    }
                }
            }
        }
    }
}

@Composable
private fun HourlyGraph(hours: List<HourlyWeather>) {
    val temps = hours.mapNotNull { it.temperatureC }
    val minTemp = temps.minOrNull() ?: 0.0
    val maxTemp = temps.maxOrNull() ?: 1.0
    val lineColor = MaterialTheme.colorScheme.primary
    val barColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
    val gridColor = Color(0xFF2A2A2A)

    Canvas(Modifier.width((hours.size.coerceAtLeast(1) * 64).dp).height(210.dp)) {
        if (hours.isEmpty()) return@Canvas
        val columnWidth = size.width / hours.size.coerceAtLeast(1)
        repeat(5) { index ->
            val y = size.height * index / 4f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        hours.forEachIndexed { index, hour ->
            val probability = (hour.precipitationProbability ?: 0).coerceIn(0, 100)
            val barHeight = size.height * 0.4f * probability / 100f
            val x = index * columnWidth + columnWidth * 0.25f
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, size.height - barHeight),
                size = androidx.compose.ui.geometry.Size(columnWidth * 0.5f, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
            )
        }
        val points = hours.mapIndexedNotNull { index, hour ->
            val temp = hour.temperatureC ?: return@mapIndexedNotNull null
            val range = (maxTemp - minTemp).takeIf { it > 0.1 } ?: 1.0
            val x = index * columnWidth + columnWidth / 2f
            val y = size.height * 0.12f + (size.height * 0.46f) * (1f - ((temp - minTemp) / range).toFloat())
            Offset(x, y)
        }
        points.zipWithNext().forEach { (a, b) ->
            drawLine(lineColor, a, b, strokeWidth = 5f, cap = StrokeCap.Round)
        }
        points.forEach { drawCircle(lineColor, radius = 5f, center = it) }
    }
}

@Composable
private fun HourCell(hour: HourlyWeather) {
    Column(Modifier.width(58.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(formatHourLabel(hour.time), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Text(weatherIcon(hour.weatherCode), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("${hour.temperatureC?.roundText() ?: "--"}°", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("${hour.precipitationProbability ?: 0}%", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
        Text("${hour.precipitationMm?.oneDecimal() ?: "0.0"}mm", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}
