package com.example.weather.ui

import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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

        Text("黄緑: 気温 / 青: 降水確率。グラフ上に時刻と気温を表示します。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

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
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val mutedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val barColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
    val gridColor = Color(0xFF303036)

    Canvas(Modifier.width((hours.size.coerceAtLeast(1) * 66).dp).height(230.dp)) {
        if (hours.isEmpty()) return@Canvas
        val topPad = 34f
        val bottomPad = 36f
        val graphHeight = size.height - topPad - bottomPad
        val columnWidth = size.width / hours.size.coerceAtLeast(1)
        val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 24f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedTextColor
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
        repeat(5) { index ->
            val y = topPad + graphHeight * index / 4f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        hours.forEachIndexed { index, hour ->
            val probability = (hour.precipitationProbability ?: 0).coerceIn(0, 100)
            val barHeight = graphHeight * 0.42f * probability / 100f
            val x = index * columnWidth + columnWidth * 0.25f
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, topPad + graphHeight - barHeight),
                size = Size(columnWidth * 0.5f, barHeight),
                cornerRadius = CornerRadius(8f, 8f),
            )
        }
        val points = hours.mapIndexedNotNull { index, hour ->
            val temp = hour.temperatureC ?: return@mapIndexedNotNull null
            val range = (maxTemp - minTemp).takeIf { it > 0.1 } ?: 1.0
            val x = index * columnWidth + columnWidth / 2f
            val y = topPad + graphHeight * 0.1f + graphHeight * 0.56f * (1f - ((temp - minTemp) / range).toFloat())
            Triple(index, Offset(x, y), temp)
        }
        points.zipWithNext().forEach { (a, b) ->
            drawLine(lineColor, a.second, b.second, strokeWidth = 5f, cap = StrokeCap.Round)
        }
        points.forEach { point ->
            drawCircle(lineColor, radius = 5f, center = point.second)
            drawContext.canvas.nativeCanvas.drawText(
                "${point.third.roundText()}°",
                point.second.x,
                (point.second.y - 12f).coerceAtLeast(24f),
                tempPaint,
            )
        }
        hours.forEachIndexed { index, hour ->
            val x = index * columnWidth + columnWidth / 2f
            drawContext.canvas.nativeCanvas.drawText(formatHourLabel(hour.time), x, size.height - 8f, timePaint)
        }
    }
}

@Composable
private fun HourCell(hour: HourlyWeather) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            Modifier
                .width(66.dp)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(formatHourLabel(hour.time), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(weatherIcon(hour.weatherCode), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("${hour.temperatureC?.roundText() ?: "--"}°", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("${hour.precipitationProbability ?: 0}%", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
            Text("${hour.precipitationMm?.oneDecimal() ?: "0.0"}mm", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}
