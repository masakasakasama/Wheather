package com.example.weather.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.example.weather.WeatherUiState
import com.example.weather.data.model.AirQuality
import com.example.weather.data.model.DailyWeather
import com.example.weather.data.model.DisasterSummary
import com.example.weather.data.model.HourlyWeather
import com.example.weather.data.model.PresetLocations
import com.example.weather.data.model.WeatherLocation
import com.example.weather.data.model.WeatherSnapshot
import com.example.weather.data.model.today
import com.example.weather.data.model.weatherIcon
import com.example.weather.data.model.weatherLabel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: WeatherUiState,
    onRefresh: () -> Unit,
    onUseDeviceLocation: () -> Unit,
    onSelectLocation: (WeatherLocation) -> Unit,
    onSearchLocations: (String) -> Unit,
    onMoveLocation: (WeatherLocation, Int) -> Unit,
    onDeleteLocation: (WeatherLocation) -> Unit,
    onDismissError: () -> Unit,
) {
    var showLocationDialog by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf<DailyWeather?>(null) }
    val snapshot = state.snapshot
    val next48Hours = remember(snapshot) { snapshot?.hourly?.nextHours(48).orEmpty() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 22.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(state.selectedLocation.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatFreshness(snapshot?.updatedAtMillis), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { showLocationDialog = true }) { Text("地点") }
                    Button(onClick = onRefresh, enabled = !state.isRefreshing) { Text(if (state.isRefreshing) "更新中" else "更新") }
                }
            }
        }

        if (state.errorMessage != null) {
            item {
                Snackbar(action = { TextButton(onClick = onDismissError) { Text("閉じる") } }) {
                    Text(state.errorMessage)
                }
            }
        }

        if (snapshot == null) {
            item {
                Spacer(Modifier.height(48.dp))
                CircularProgressIndicator()
                Text("天気を取得しています", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            if (state.disasterSummary?.hasImportantInfo == true) {
                item { DisasterSummaryCard(state.disasterSummary) }
            }
            item { CurrentSummary(snapshot) }
            item { AirQualityCard(snapshot.airQuality) }
            item { RainSummary(snapshot, next48Hours) }
            item { HomeHourlySection(next48Hours) }
            item {
                HomeWeeklySection(
                    days = snapshot.daily.take(14),
                    hourly = snapshot.hourly,
                    onDayClick = { selectedDay = it },
                )
            }
            item {
                Text(
                    if (snapshot.usedFallbackModel) {
                        "Open-Meteo best matchモデル"
                    } else {
                        "Open-Meteo JMA Seamless優先。降水確率は必要に応じてbest matchで補完"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
    }

    if (showLocationDialog) {
        LocationDialog(
            state = state,
            onDismiss = { showLocationDialog = false },
            onSearchLocations = onSearchLocations,
            onSelectLocation = {
                onSelectLocation(it)
                showLocationDialog = false
            },
            onMoveLocation = onMoveLocation,
            onDeleteLocation = onDeleteLocation,
            onUseDeviceLocation = {
                onUseDeviceLocation()
                showLocationDialog = false
            },
        )
    }

    selectedDay?.let { day ->
        DayDetailDialog(
            day = day,
            dayHours = snapshot?.hourly?.forDate(day.date).orEmpty(),
            onDismiss = { selectedDay = null },
        )
    }
}

@Composable
private fun DisasterSummaryCard(summary: DisasterSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1717)),
        shape = MaterialTheme.shapes.small,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("重要な気象情報", fontSize = 13.sp, color = Color(0xFFFFB4AB), fontWeight = FontWeight.SemiBold)
            summary.typhoons.forEach { typhoon ->
                Text("台風第${typhoon.number}号 ${typhoon.category}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            if (summary.activeWarnings.isNotEmpty()) {
                Text(
                    "${summary.officeName ?: "現在地周辺"}: ${summary.activeWarnings.take(5).joinToString(" / ")}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            summary.warningHeadline?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Text("気象庁発表。避難判断は自治体・気象庁の最新情報を確認", color = Color(0xFFFFDAD6), fontSize = 11.sp)
        }
    }
}

@Composable
private fun CurrentSummary(snapshot: WeatherSnapshot) {
    val today = snapshot.today()
    val todayHours = today?.let { snapshot.hourly.forDate(it.date) }.orEmpty()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.small,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${snapshot.current.temperatureC?.roundText() ?: "--"}°", fontSize = 82.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
                Spacer(Modifier.width(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(weatherIcon(snapshot.current.weatherCode), fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Text(weatherLabel(snapshot.current.weatherCode), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("現在降水量 ${snapshot.current.precipitationMm?.oneDecimal() ?: "--"}mm", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Metric("最高", "${today?.maxTemperatureC?.roundText() ?: "--"}°")
                Metric("最低", "${today?.minTemperatureC?.roundText() ?: "--"}°")
                Metric("降水確率", today.effectiveMaxProbability(todayHours).percentText())
                Metric("雨量", today.effectivePrecipitationSum(todayHours).mmText())
            }
            HorizontalDivider(color = Color(0xFF303036))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetailMetric("体感", snapshot.current.apparentTemperatureC.temperatureText(), Modifier.weight(1f))
                    DetailMetric("湿度", snapshot.current.humidityPercent.percentText(), Modifier.weight(1f))
                    DetailMetric("風", windText(snapshot.current.windSpeedKmh, snapshot.current.windDirectionDeg), Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetailMetric("気圧", snapshot.current.pressureHpa.pressureText(), Modifier.weight(1f))
                    DetailMetric("UV", today?.uvIndexMax.uvText(), Modifier.weight(1f))
                    DetailMetric("日の出/入", "${formatTimeOnly(today?.sunrise)} / ${formatTimeOnly(today?.sunset)}", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AirQualityCard(airQuality: AirQuality?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.small,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("空気質", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Text(aqiLabel(airQuality?.europeanAqi), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("AQI ${airQuality?.europeanAqi?.toString() ?: "--"}", color = aqiColor(airQuality?.europeanAqi), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(airQuality?.time?.let(::formatDateHourLabel) ?: "未取得", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AirMetric("PM2.5", airQuality?.pm25.microgramText(), Modifier.weight(1f))
                AirMetric("PM10", airQuality?.pm10.microgramText(), Modifier.weight(1f))
                AirMetric("オゾン", airQuality?.ozone.microgramText(), Modifier.weight(1f))
            }
            val peak = airQuality?.hourly
                ?.filter { runCatching { !LocalDateTime.parse(it.time).isBefore(LocalDateTime.now(ZoneId.of("Asia/Tokyo")).withMinute(0).withSecond(0).withNano(0)) }.getOrDefault(false) }
                ?.take(24)
                ?.maxByOrNull { it.europeanAqi ?: -1 }
            Text(
                peak?.let { "24時間以内の最大AQI ${it.europeanAqi ?: "--"} (${formatDateHourLabel(it.time)})" }
                    ?: "空気質データを取得できません",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun RainSummary(snapshot: WeatherSnapshot, next48Hours: List<HourlyWeather>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.small,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("雨の見通し", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text(nextRainText(snapshot), fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
            val peak = next48Hours.maxByOrNull { it.precipitationProbability ?: -1 }
            Text(
                peak?.let { "48時間以内の最大降水確率 ${it.precipitationProbability.percentText()} (${formatDateHourLabel(it.time)})" }
                    ?: "48時間以内の降水データなし",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 13.sp,
            )
            val today = snapshot.today()
            val todayHours = today?.let { snapshot.hourly.forDate(it.date) }.orEmpty()
            Text("今日の予想降水量 ${today.effectivePrecipitationSum(todayHours).mmText()}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

@Composable
private fun HomeHourlySection(hours: List<HourlyWeather>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("今後48時間", "1時間ごとの気温・降水")
        val scrollState = rememberScrollState()
        Row(Modifier.horizontalScroll(scrollState), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                hours.forEach { hour -> HourCompactCard(hour) }
            }
        }
    }
}

@Composable
private fun MiniHourlyGraph(hours: List<HourlyWeather>) {
    val temps = hours.mapNotNull { it.temperatureC }
    val minTemp = temps.minOrNull() ?: 0.0
    val maxTemp = temps.maxOrNull() ?: 1.0
    val lineColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val mutedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val barColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
    val gridColor = Color(0xFF303036)

    Canvas(
        Modifier
            .width((hours.size.coerceAtLeast(1) * 92).dp)
            .height(190.dp),
    ) {
        if (hours.isEmpty()) return@Canvas
        val topPad = 30f
        val bottomPad = 40f
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
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }
        repeat(4) { index ->
            val y = topPad + graphHeight * index / 3f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        hours.forEachIndexed { index, hour ->
            val probability = (hour.precipitationProbability ?: 0).coerceIn(0, 100)
            val barHeight = graphHeight * 0.42f * probability / 100f
            drawRoundRect(
                color = barColor,
                topLeft = Offset(index * columnWidth + columnWidth * 0.3f, topPad + graphHeight - barHeight),
                size = Size(columnWidth * 0.4f, barHeight),
                cornerRadius = CornerRadius(8f, 8f),
            )
        }
        val points = hours.mapIndexedNotNull { index, hour ->
            val temp = hour.temperatureC ?: return@mapIndexedNotNull null
            val range = (maxTemp - minTemp).takeIf { it > 0.1 } ?: 1.0
            val x = index * columnWidth + columnWidth / 2f
            val y = topPad + graphHeight * 0.12f + graphHeight * 0.56f * (1f - ((temp - minTemp) / range).toFloat())
            IndexedPoint(index, Offset(x, y), temp)
        }
        points.zipWithNext().forEach { (a, b) ->
            drawLine(lineColor, a.offset, b.offset, strokeWidth = 5f, cap = StrokeCap.Round)
        }
        points.forEach { point ->
            drawCircle(lineColor, radius = 5f, center = point.offset)
            drawContext.canvas.nativeCanvas.drawText(
                "${point.temperature.roundText()}°",
                point.offset.x,
                (point.offset.y - 12f).coerceAtLeast(22f),
                tempPaint,
            )
        }
        hours.forEachIndexed { index, hour ->
            val x = index * columnWidth + columnWidth / 2f
            drawContext.canvas.nativeCanvas.drawText(formatHourLabel(hour.time), x, size.height - 8f, timePaint)
        }
    }
}

private data class IndexedPoint(
    val index: Int,
    val offset: Offset,
    val temperature: Double,
)

@Composable
private fun HourCompactCard(hour: HourlyWeather) {
    val probability = (hour.precipitationProbability ?: 0).coerceIn(0, 100)
    val barColor = MaterialTheme.colorScheme.secondary.copy(alpha = if (probability == 0) 0.14f else 0.75f)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            Modifier
                .width(82.dp)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(formatDateHourLabel(hour.time), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Text(weatherIcon(hour.weatherCode), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("${hour.temperatureC?.roundText() ?: "--"}°", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Canvas(Modifier.fillMaxWidth().height(34.dp)) {
                val trackTop = size.height - 8f
                drawRoundRect(
                    color = Color(0xFF35363B),
                    topLeft = Offset(0f, trackTop),
                    size = Size(size.width, 7f),
                    cornerRadius = CornerRadius(6f, 6f),
                )
                val barHeight = (size.height - 2f) * probability / 100f
                if (probability > 0) {
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(size.width * 0.22f, trackTop - barHeight),
                        size = Size(size.width * 0.56f, barHeight),
                        cornerRadius = CornerRadius(7f, 7f),
                    )
                }
            }
            Text(hour.precipitationProbability.percentText(), color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
            Text(hour.precipitationMm.mmText(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

@Composable
private fun HomeWeeklySection(days: List<DailyWeather>, hourly: List<HourlyWeather>, onDayClick: (DailyWeather) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("2週間", "AM / PMの概況")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEach { day ->
                WeeklyRow(day = day, dayHours = hourly.forDate(day.date), onClick = { onDayClick(day) })
            }
        }
    }
}

@Composable
fun WeeklyRow(day: DailyWeather, dayHours: List<HourlyWeather>, onClick: () -> Unit) {
    val parts = dayPeriodSummaries(dayHours)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.small,
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(formatDateShort(day.date), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(weatherIcon(day.weatherCode), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${day.maxTemperatureC?.roundText() ?: "--"}°", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("${day.minTemperatureC?.roundText() ?: "--"}°", fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(horizontalAlignment = Alignment.End) {
                    Text(day.effectiveMaxProbability(dayHours).percentText(), color = MaterialTheme.colorScheme.secondary)
                    Text(day.effectivePrecipitationSum(dayHours).mmText(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PeriodChip(parts.first, Modifier.weight(1f))
                PeriodChip(parts.second, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PeriodChip(summary: DayPeriodSummary, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.small,
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(summary.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${weatherIcon(summary.weatherCode)} ${summary.maxTemp?.roundText() ?: "--"}°", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${summary.maxProbability.percentText()} / ${summary.precipitationSum.mmText()}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun DayDetailDialog(day: DailyWeather, dayHours: List<HourlyWeather>, onDismiss: () -> Unit) {
    val parts = dayPeriodSummaries(dayHours)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(formatDateLong(day.date)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${weatherIcon(day.weatherCode)} ${weatherLabel(day.weatherCode)}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("最高気温: ${day.maxTemperatureC?.roundText() ?: "--"}°")
                Text("最低気温: ${day.minTemperatureC?.roundText() ?: "--"}°")
                Text("最大降水確率: ${day.effectiveMaxProbability(dayHours).percentText()}")
                Text("予想降水量: ${day.effectivePrecipitationSum(dayHours).mmText()}")
                Text("UV指数: ${day.uvIndexMax.uvText()}")
                Text("日の出 / 日の入: ${formatTimeOnly(day.sunrise)} / ${formatTimeOnly(day.sunset)}")
                Text("AM: ${weatherIcon(parts.first.weatherCode)} ${parts.first.maxProbability.percentText()} / ${parts.first.precipitationSum.mmText()}")
                Text("PM: ${weatherIcon(parts.second.weatherCode)} ${parts.second.maxProbability.percentText()} / ${parts.second.precipitationSum.mmText()}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocationDialog(
    state: WeatherUiState,
    onDismiss: () -> Unit,
    onSearchLocations: (String) -> Unit,
    onSelectLocation: (WeatherLocation) -> Unit,
    onMoveLocation: (WeatherLocation, Int) -> Unit,
    onDeleteLocation: (WeatherLocation) -> Unit,
    onUseDeviceLocation: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("地点") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            onSearchLocations(it)
                        },
                        singleLine = true,
                        label = { Text("世界中の都市を検索") },
                        placeholder = { Text("例: Seoul, London, New York") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    TextButton(onClick = onUseDeviceLocation) { Text("現在地を使う") }
                }
                item {
                    Text("保存地点", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                items(state.savedLocations) { location ->
                    LocationRow(
                        location = location,
                        selected = location.samePlaceAs(state.selectedLocation),
                        onSelect = { onSelectLocation(location) },
                        onMoveUp = { onMoveLocation(location, -1) },
                        onMoveDown = { onMoveLocation(location, 1) },
                        onDelete = { onDeleteLocation(location) },
                    )
                }
                item { HorizontalDivider() }
                if (state.isSearchingLocation) {
                    item { Text("検索中...", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                if (state.searchResults.isNotEmpty()) {
                    item { Text("検索結果", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
                    items(state.searchResults.take(8)) { location ->
                        Text(
                            text = location.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectLocation(location) }
                                .padding(vertical = 8.dp),
                        )
                    }
                }
                item {
                    Text("プリセット", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PresetLocations.drop(1).forEach { location ->
                            AssistChip(
                                onClick = { onSelectLocation(location) },
                                label = { Text(location.name) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        },
    )
}

@Composable
private fun LocationRow(
    location: WeatherLocation,
    selected: Boolean,
    onSelect: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(location.name, fontWeight = FontWeight.SemiBold)
                Text("${location.latitude.oneDecimal()}, ${location.longitude.oneDecimal()}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(onClick = onMoveUp) { Text("↑") }
                TextButton(onClick = onMoveDown) { Text("↓") }
                TextButton(onClick = onDelete) { Text("削除") }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DetailMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AirMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

fun nextRainText(snapshot: WeatherSnapshot): String {
    val next = snapshot.hourly.nextHours(48).firstOrNull {
        (it.precipitationProbability ?: 0) >= 50 || (it.precipitationMm ?: 0.0) > 0.0
    }
    return next?.let {
        "${formatDateHourLabel(it.time)}ごろから雨の可能性"
    } ?: "48時間以内の雨の可能性は低め"
}

fun formatHourLabel(time: String): String {
    val hour = runCatching { LocalDateTime.parse(time).hour }.getOrNull() ?: return "--"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "${if (hour < 12) "AM" else "PM"} ${displayHour}時"
}

fun formatDateHourLabel(time: String): String {
    val parsed = runCatching { LocalDateTime.parse(time) }.getOrNull() ?: return "--"
    return "${parsed.format(DateTimeFormatter.ofPattern("M/d"))} ${formatHourLabel(time)}"
}

fun formatHourMinute(epochMillis: Long): String {
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.of("Asia/Tokyo"))
        .format(DateTimeFormatter.ofPattern("HH:mm"))
}

fun formatFreshness(epochMillis: Long?): String {
    if (epochMillis == null) return "前回更新 --:--"
    val updated = Instant.ofEpochMilli(epochMillis)
    val ageMinutes = java.time.Duration.between(updated, Instant.now()).toMinutes()
    val staleText = if (ageMinutes >= 120) "（古いデータ）" else ""
    return "前回更新 ${formatHourMinute(epochMillis)}$staleText"
}

fun formatTimeOnly(time: String?): String {
    val parsed = time?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } ?: return "--:--"
    return parsed.format(DateTimeFormatter.ofPattern("HH:mm"))
}

fun formatDateShort(date: String): String {
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
    return parsed?.format(DateTimeFormatter.ofPattern("M/d")) ?: date
}

fun formatDateLong(date: String): String {
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
    return parsed?.format(DateTimeFormatter.ofPattern("yyyy年M月d日")) ?: date
}

fun List<HourlyWeather>.nextHours(count: Int): List<HourlyWeather> {
    val now = LocalDateTime.now(ZoneId.of("Asia/Tokyo")).withMinute(0).withSecond(0).withNano(0)
    return filter { hour ->
        runCatching { !LocalDateTime.parse(hour.time).isBefore(now) }.getOrDefault(false)
    }.take(count)
}

fun List<HourlyWeather>.forDate(date: String): List<HourlyWeather> {
    return filter { hour ->
        runCatching { LocalDateTime.parse(hour.time).toLocalDate().toString() == date }.getOrDefault(false)
    }
}

data class DayPeriodSummary(
    val label: String,
    val weatherCode: Int?,
    val maxTemp: Double?,
    val maxProbability: Int?,
    val precipitationSum: Double?,
)

fun dayPeriodSummaries(hours: List<HourlyWeather>): Pair<DayPeriodSummary, DayPeriodSummary> {
    return summarizePeriod("AM", hours.filter { runCatching { LocalDateTime.parse(it.time).hour < 12 }.getOrDefault(false) }) to
        summarizePeriod("PM", hours.filter { runCatching { LocalDateTime.parse(it.time).hour >= 12 }.getOrDefault(false) })
}

private fun summarizePeriod(label: String, hours: List<HourlyWeather>): DayPeriodSummary {
    val maxRainHour = hours.maxByOrNull { it.precipitationProbability ?: -1 }
    val representativeWeather = maxRainHour?.weatherCode ?: hours.firstOrNull()?.weatherCode
    return DayPeriodSummary(
        label = label,
        weatherCode = representativeWeather,
        maxTemp = hours.mapNotNull { it.temperatureC }.maxOrNull(),
        maxProbability = hours.mapNotNull { it.precipitationProbability }.maxOrNull(),
        precipitationSum = hours.mapNotNull { it.precipitationMm }.takeIf { it.isNotEmpty() }?.sum(),
    )
}

fun Double.roundText(): String = "%.0f".format(this)
fun Double.oneDecimal(): String = "%.1f".format(this)
fun Int?.percentText(): String = this?.let { "$it%" } ?: "--%"
fun Double?.mmText(): String = this?.let { "${it.oneDecimal()}mm" } ?: "--mm"
fun Double?.temperatureText(): String = this?.let { "${it.roundText()}°" } ?: "--°"
fun Double?.pressureText(): String = this?.let { "${it.roundText()}hPa" } ?: "--hPa"
fun Double?.uvText(): String = this?.let { it.oneDecimal() } ?: "--"
fun Double?.microgramText(): String = this?.let { "${it.oneDecimal()}μg/m³" } ?: "--μg/m³"

fun aqiLabel(value: Int?): String = when (value) {
    null -> "取得できません"
    in 0..20 -> "良好"
    in 21..40 -> "まあ良い"
    in 41..60 -> "普通"
    in 61..80 -> "悪い"
    in 81..100 -> "非常に悪い"
    else -> "かなり悪い"
}

@Composable
fun aqiColor(value: Int?): Color = when (value) {
    null -> MaterialTheme.colorScheme.onSurfaceVariant
    in 0..40 -> MaterialTheme.colorScheme.primary
    in 41..60 -> MaterialTheme.colorScheme.tertiary
    else -> Color(0xFFFF8A80)
}

fun windText(speedKmh: Double?, directionDeg: Int?): String {
    val speed = speedKmh?.oneDecimal() ?: "--"
    val direction = windDirectionText(directionDeg)
    return if (direction.isBlank()) "${speed}km/h" else "$direction ${speed}km/h"
}

fun windDirectionText(degrees: Int?): String {
    if (degrees == null) return ""
    val labels = listOf("北", "北東", "東", "南東", "南", "南西", "西", "北西")
    val index = (((degrees % 360) + 22.5) / 45.0).toInt() % labels.size
    return labels[index]
}

fun DailyWeather?.effectiveMaxProbability(dayHours: List<HourlyWeather>): Int? {
    val hourlyMax = dayHours.mapNotNull { it.precipitationProbability }.maxOrNull()
    return listOfNotNull(this?.maxPrecipitationProbability, hourlyMax).maxOrNull()
}

fun DailyWeather?.effectivePrecipitationSum(dayHours: List<HourlyWeather>): Double? {
    val hourlyValues = dayHours.mapNotNull { it.precipitationMm }
    val hourlySum = hourlyValues.takeIf { it.isNotEmpty() }?.sum()
    return listOfNotNull(this?.precipitationSumMm, hourlySum).maxOrNull()
}

private fun WeatherLocation.samePlaceAs(other: WeatherLocation): Boolean {
    return "%.4f".format(latitude) == "%.4f".format(other.latitude) &&
        "%.4f".format(longitude) == "%.4f".format(other.longitude)
}
