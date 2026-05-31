package com.example.weather.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weather.WeatherUiState
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
    onDismissError: () -> Unit,
) {
    var showLocationDialog by remember { mutableStateOf(false) }
    val snapshot = state.snapshot

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(state.selectedLocation.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("前回更新 ${snapshot?.updatedAtMillis?.let(::formatHourMinute) ?: "--:--"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { showLocationDialog = true }) { Text("地点") }
                Button(onClick = onRefresh, enabled = !state.isRefreshing) { Text(if (state.isRefreshing) "更新中" else "更新") }
            }
        }

        if (state.errorMessage != null) {
            Snackbar(action = { TextButton(onClick = onDismissError) { Text("閉じる") } }) {
                Text(state.errorMessage)
            }
        }

        if (snapshot == null) {
            Spacer(Modifier.height(48.dp))
            CircularProgressIndicator()
            Text("天気を取得しています", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${snapshot.current.temperatureC?.roundText() ?: "--"}°", fontSize = 88.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
            Spacer(Modifier.width(20.dp))
            Column {
                Text(weatherIcon(snapshot.current.weatherCode), fontSize = 42.sp)
                Text(weatherLabel(snapshot.current.weatherCode), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric("最高", "${snapshot.today()?.maxTemperatureC?.roundText() ?: "--"}°")
            Metric("最低", "${snapshot.today()?.minTemperatureC?.roundText() ?: "--"}°")
            Metric("降水量", "${snapshot.current.precipitationMm?.oneDecimal() ?: "--"} mm")
            Metric("今日の降水", "${snapshot.today()?.maxPrecipitationProbability ?: "--"}%")
        }

        Text(nextRainText(snapshot), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(
            if (snapshot.usedFallbackModel) "JMAモデル取得失敗のためOpen-Meteo best matchを使用中" else "Open-Meteo JMA Seamlessモデル",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
    }

    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("地点を選択") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PresetLocations.drop(1).forEach { location ->
                            AssistChip(
                                onClick = {
                                    onSelectLocation(location)
                                    showLocationDialog = false
                                },
                                label = { Text(location.name) },
                            )
                        }
                    }
                    TextButton(onClick = {
                        onUseDeviceLocation()
                        showLocationDialog = false
                    }) {
                        Text("現在地を使う")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLocationDialog = false }) { Text("閉じる") }
            },
        )
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
        val hour = runCatching { LocalDateTime.parse(it.time).hour }.getOrNull()
        if (hour != null) "${hour}時ごろから雨の可能性" else "まもなく雨の可能性"
    } ?: "しばらく雨の可能性は低め"
}

fun formatHourMinute(epochMillis: Long): String {
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.of("Asia/Tokyo"))
        .format(DateTimeFormatter.ofPattern("HH:mm"))
}

fun Double.roundText(): String = "%.0f".format(this)
fun Double.oneDecimal(): String = "%.1f".format(this)
