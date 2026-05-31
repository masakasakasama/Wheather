package com.example.weather.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weather.WeatherUiState
import com.example.weather.data.model.DailyWeather
import com.example.weather.data.model.HourlyWeather
import com.example.weather.data.model.PresetLocations
import com.example.weather.data.model.WeatherLocation
import com.example.weather.data.model.WeatherSnapshot
import com.example.weather.data.model.today
import com.example.weather.data.model.weatherIcon
import com.example.weather.data.model.weatherLabel
import java.time.Instant
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
    onDismissError: () -> Unit,
) {
    var showLocationDialog by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf<DailyWeather?>(null) }
    val snapshot = state.snapshot

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 22.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(state.selectedLocation.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("前回更新 ${snapshot?.updatedAtMillis?.let(::formatHourMinute) ?: "--:--"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            item { CurrentSummary(snapshot) }
            item { RainSummary(snapshot) }
            item { HomeHourlySection(snapshot.hourly.take(12)) }
            item {
                HomeWeeklySection(
                    days = snapshot.daily.take(7),
                    onDayClick = { selectedDay = it },
                )
            }
            item {
                Text(
                    if (snapshot.usedFallbackModel) "JMAモデル取得失敗のためOpen-Meteo best matchを使用中" else "Open-Meteo JMA Seamlessモデル",
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
            onUseDeviceLocation = {
                onUseDeviceLocation()
                showLocationDialog = false
            },
        )
    }

    selectedDay?.let { day ->
        DayDetailDialog(day = day, onDismiss = { selectedDay = null })
    }
}

@Composable
private fun CurrentSummary(snapshot: WeatherSnapshot) {
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
                    Text("降水量 ${snapshot.current.precipitationMm?.oneDecimal() ?: "--"} mm", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Metric("最高", "${snapshot.today()?.maxTemperatureC?.roundText() ?: "--"}°")
                Metric("最低", "${snapshot.today()?.minTemperatureC?.roundText() ?: "--"}°")
                Metric("今日の降水", "${snapshot.today()?.maxPrecipitationProbability ?: "--"}%")
            }
        }
    }
}

@Composable
private fun RainSummary(snapshot: WeatherSnapshot) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.small,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("雨の見通し", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text(nextRainText(snapshot), fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
            val peak = snapshot.hourly.take(24).maxByOrNull { it.precipitationProbability ?: -1 }
            Text(
                peak?.let { "24時間以内の最大降水確率 ${it.precipitationProbability ?: "--"}% (${formatHourLabel(it.time)})" } ?: "24時間以内の降水データなし",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun HomeHourlySection(hours: List<HourlyWeather>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("今後12時間", "気温線と降水確率")
        MiniHourlyGraph(hours)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(hours) { hour ->
                HourCompactCard(hour)
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
    val barColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
    val gridColor = Color(0xFF2A2A2A)

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(150.dp),
    ) {
        if (hours.isEmpty()) return@Canvas
        val columnWidth = size.width / hours.size.coerceAtLeast(1)
        repeat(4) { index ->
            val y = size.height * index / 3f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        hours.forEachIndexed { index, hour ->
            val probability = (hour.precipitationProbability ?: 0).coerceIn(0, 100)
            val barHeight = size.height * 0.36f * probability / 100f
            drawRoundRect(
                color = barColor,
                topLeft = Offset(index * columnWidth + columnWidth * 0.3f, size.height - barHeight),
                size = androidx.compose.ui.geometry.Size(columnWidth * 0.4f, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
            )
        }
        val points = hours.mapIndexedNotNull { index, hour ->
            val temp = hour.temperatureC ?: return@mapIndexedNotNull null
            val range = (maxTemp - minTemp).takeIf { it > 0.1 } ?: 1.0
            val x = index * columnWidth + columnWidth / 2f
            val y = size.height * 0.1f + size.height * 0.46f * (1f - ((temp - minTemp) / range).toFloat())
            Offset(x, y)
        }
        points.zipWithNext().forEach { (a, b) ->
            drawLine(lineColor, a, b, strokeWidth = 5f, cap = StrokeCap.Round)
        }
        points.forEach { drawCircle(lineColor, radius = 5f, center = it) }
    }
}

@Composable
private fun HourCompactCard(hour: HourlyWeather) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            Modifier
                .width(76.dp)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(formatHourLabel(hour.time), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(weatherIcon(hour.weatherCode), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("${hour.temperatureC?.roundText() ?: "--"}°", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("${hour.precipitationProbability ?: 0}%", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun HomeWeeklySection(days: List<DailyWeather>, onDayClick: (DailyWeather) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("週間", "カードを押すと詳細")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEach { day ->
                WeeklyRow(day = day, onClick = { onDayClick(day) })
            }
        }
    }
}

@Composable
fun WeeklyRow(day: DailyWeather, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(day.date, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(weatherIcon(day.weatherCode), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("${day.maxTemperatureC?.roundText() ?: "--"}°", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("${day.minTemperatureC?.roundText() ?: "--"}°", fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${day.maxPrecipitationProbability ?: 0}%", color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun DayDetailDialog(day: DailyWeather, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(day.date) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${weatherIcon(day.weatherCode)} ${weatherLabel(day.weatherCode)}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("最高気温: ${day.maxTemperatureC?.roundText() ?: "--"}°")
                Text("最低気温: ${day.minTemperatureC?.roundText() ?: "--"}°")
                Text("最大降水確率: ${day.maxPrecipitationProbability ?: 0}%")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        },
    )
}

@Composable
private fun LocationDialog(
    state: WeatherUiState,
    onDismiss: () -> Unit,
    onSearchLocations: (String) -> Unit,
    onSelectLocation: (WeatherLocation) -> Unit,
    onUseDeviceLocation: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("地点を選択") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                if (state.isSearchingLocation) {
                    Text("検索中...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                state.searchResults.take(8).forEach { location ->
                    Text(
                        text = location.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectLocation(location) }
                            .padding(vertical = 8.dp),
                    )
                }
                Text("プリセット", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetLocations.drop(1).forEach { location ->
                        AssistChip(
                            onClick = { onSelectLocation(location) },
                            label = { Text(location.name) },
                        )
                    }
                }
                TextButton(onClick = onUseDeviceLocation) {
                    Text("現在地を使う")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        },
    )
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

fun nextRainText(snapshot: WeatherSnapshot): String {
    val next = snapshot.hourly.firstOrNull {
        (it.precipitationProbability ?: 0) >= 50 || (it.precipitationMm ?: 0.0) > 0.0
    }
    return next?.let {
        "${formatHourLabel(it.time)}ごろから雨の可能性"
    } ?: "しばらく雨の可能性は低め"
}

fun formatHourLabel(time: String): String {
    val hour = runCatching { LocalDateTime.parse(time).hour }.getOrNull()
    return if (hour == null) "--時" else "${hour}時"
}

fun formatHourMinute(epochMillis: Long): String {
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.of("Asia/Tokyo"))
        .format(DateTimeFormatter.ofPattern("HH:mm"))
}

fun Double.roundText(): String = "%.0f".format(this)
fun Double.oneDecimal(): String = "%.1f".format(this)
