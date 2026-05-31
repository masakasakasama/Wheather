package com.example.weather.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
            if (state.disasterSummary?.hasImportantInfo == true) {
                item { DisasterSummaryCard(state.disasterSummary) }
            }
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
                    if (snapshot.usedFallbackModel) {
                        "JMA Seamlessが取得できなかったためOpen-Meteo best matchを使用中"
                    } else {
                        "Open-Meteo JMA Seamlessモデル"
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
        DayDetailDialog(day = day, onDismiss = { selectedDay = null })
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
            if (summary.typhoons.isNotEmpty()) {
                summary.typhoons.forEach { typhoon ->
                    Text(
                        "台風第${typhoon.number}号 ${typhoon.category}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
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
                Metric("今日の降水", snapshot.today()?.maxPrecipitationProbability.percentText())
                Metric("雨量", snapshot.today()?.precipitationSumMm.mmText())
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
                peak?.let { "24時間以内の最大降水確率 ${it.precipitationProbability ?: "--"}% (${formatHourLabel(it.time)})" }
                    ?: "24時間以内の降水データなし",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 13.sp,
            )
            Text(
                "今日の予想降水量 ${snapshot.today()?.precipitationSumMm.mmText()}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun HomeHourlySection(hours: List<HourlyWeather>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("今後12時間", "時刻ごとの気温と降水確率")
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
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val mutedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val barColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
    val gridColor = Color(0xFF303036)

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(178.dp),
    ) {
        if (hours.isEmpty()) return@Canvas
        val topPad = 28f
        val bottomPad = 34f
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
            Text(formatHourLabel(hour.time), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(weatherIcon(hour.weatherCode), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("${hour.temperatureC?.roundText() ?: "--"}°", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(hour.precipitationProbability.percentText(), color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
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
            Text(formatDateShort(day.date), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(weatherIcon(day.weatherCode), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("${day.maxTemperatureC?.roundText() ?: "--"}°", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("${day.minTemperatureC?.roundText() ?: "--"}°", fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(horizontalAlignment = Alignment.End) {
                Text(day.maxPrecipitationProbability.percentText(), color = MaterialTheme.colorScheme.secondary)
                Text(day.precipitationSumMm.mmText(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun DayDetailDialog(day: DailyWeather, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(formatDateLong(day.date)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${weatherIcon(day.weatherCode)} ${weatherLabel(day.weatherCode)}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("最高気温: ${day.maxTemperatureC?.roundText() ?: "--"}°")
                Text("最低気温: ${day.minTemperatureC?.roundText() ?: "--"}°")
                Text("最大降水確率: ${day.maxPrecipitationProbability.percentText()}")
                Text("予想降水量: ${day.precipitationSumMm.mmText()}")
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
                    TextButton(onClick = onUseDeviceLocation) {
                        Text("現在地を使う")
                    }
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
                Text(
                    "${location.latitude.oneDecimal()}, ${location.longitude.oneDecimal()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
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

fun formatDateShort(date: String): String {
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
    return parsed?.format(DateTimeFormatter.ofPattern("M/d")) ?: date
}

fun formatDateLong(date: String): String {
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
    return parsed?.format(DateTimeFormatter.ofPattern("yyyy年M月d日")) ?: date
}

fun Double.roundText(): String = "%.0f".format(this)
fun Double.oneDecimal(): String = "%.1f".format(this)
fun Int?.percentText(): String = this?.let { "$it%" } ?: "--%"
fun Double?.mmText(): String = this?.let { "${it.oneDecimal()}mm" } ?: "--mm"

private fun WeatherLocation.samePlaceAs(other: WeatherLocation): Boolean {
    return "%.4f".format(latitude) == "%.4f".format(other.latitude) &&
        "%.4f".format(longitude) == "%.4f".format(other.longitude)
}
