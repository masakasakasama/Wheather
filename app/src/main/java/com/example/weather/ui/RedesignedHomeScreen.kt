package com.example.weather.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weather.WeatherUiState
import com.example.weather.data.model.DailyWeather
import com.example.weather.data.model.NotificationSettings
import com.example.weather.data.model.WeatherLocation
import com.example.weather.data.model.WeatherSnapshot
import com.example.weather.data.model.appliesTo
import com.example.weather.data.model.effectiveCurrentWeatherCode
import com.example.weather.data.model.effectiveCurrentWeatherLabel
import com.example.weather.data.model.effectiveMaxProbability
import com.example.weather.data.model.effectivePrecipitationSum
import com.example.weather.data.model.forecastDays
import com.example.weather.data.model.forecastZoneId
import com.example.weather.data.model.freshRadarPrecipitation
import com.example.weather.data.model.intensityLabel
import com.example.weather.data.model.isInJapan
import com.example.weather.data.model.isRaining
import com.example.weather.data.model.nextExpectedPrecipitation
import com.example.weather.data.model.sameSavedPlaceAs
import com.example.weather.data.model.today
import com.example.weather.data.model.weatherIcon
import com.example.weather.data.model.weatherLabel
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val RedesignBlue = Color(0xFF2F7AF8)
private val RedesignText = Color(0xFF10233A)
private val RedesignMuted = Color(0xFF6C7C8E)
private val RedesignCard = Color.White
private val RedesignSoftBlue = Color(0xFFEAF3FF)
private val RedesignSoftGreen = Color(0xFFEAF8F0)
private val RedesignGreen = Color(0xFF2EA66B)
private val RedesignSoftPurple = Color(0xFFF1EDFF)
private val RedesignPurple = Color(0xFF6B55DF)
private val RedesignRed = Color(0xFFF15A4A)
private val RedesignLow = Color(0xFF6D93D8)

@Composable
fun RedesignedHomeScreen(
    state: WeatherUiState,
    appVersionName: String,
    onRefresh: () -> Unit,
    onUseDeviceLocation: () -> Unit,
    onSelectLocation: (WeatherLocation) -> Unit,
    onSearchLocations: (String) -> Unit,
    onMoveLocation: (WeatherLocation, Int) -> Unit,
    onDeleteLocation: (WeatherLocation) -> Unit,
    onUpdateNotificationSettings: (NotificationSettings) -> Unit,
    onCheckUpdate: () -> Unit,
    onDismissUpdateCheckMessage: () -> Unit,
    onDismissError: () -> Unit,
) {
    var showLocations by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf<DailyWeather?>(null) }
    val snapshot = state.snapshot

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            RedesignHeader(
                state = state,
                onRefresh = onRefresh,
                onUseDeviceLocation = onUseDeviceLocation,
                onSelectLocation = onSelectLocation,
                onLocations = { showLocations = true },
                onSettings = { showSettings = true },
            )
        }

        state.errorMessage?.let { message ->
            item {
                Snackbar(
                    containerColor = Color(0xFFFFEEEE),
                    contentColor = RedesignText,
                    action = { TextButton(onClick = onDismissError) { Text("閉じる") } },
                ) { Text(message) }
            }
        }

        if (snapshot == null) {
            item {
                Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = RedesignBlue)
                        Spacer(Modifier.height(14.dp))
                        Text("天気を取得しています", color = RedesignMuted)
                    }
                }
            }
        } else {
            state.disasterSummary
                ?.takeIf { it.appliesTo(state.selectedLocation) && it.hasImportantInfo }
                ?.let { disaster ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0ED)),
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("重要な気象情報", color = RedesignRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    disaster.warningHeadline ?: disaster.activeWarnings.firstOrNull() ?: "台風・防災情報があります",
                                    color = RedesignText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                )
                            }
                        }
                    }
                }

            item { RedesignHero(snapshot) }
            item { RedesignHourly(snapshot) }
            item { RedesignTodayTomorrow(snapshot) { selectedDay = it } }
            item { RedesignMetrics(snapshot) }
            item { RedesignSources(snapshot) }
        }
    }

    if (showLocations) {
        RedesignLocationDialog(
            state = state,
            onDismiss = { showLocations = false },
            onSearchLocations = onSearchLocations,
            onSelectLocation = {
                onSelectLocation(it)
                showLocations = false
            },
            onMoveLocation = onMoveLocation,
            onDeleteLocation = onDeleteLocation,
            onUseDeviceLocation = {
                onUseDeviceLocation()
                showLocations = false
            },
        )
    }

    if (showSettings) {
        RedesignSettingsDialog(
            settings = state.notificationSettings,
            appVersionName = appVersionName,
            isCheckingUpdate = state.isCheckingUpdate,
            updateCheckMessage = state.updateCheckMessage,
            onCheckUpdate = onCheckUpdate,
            onDismiss = {
                showSettings = false
                onDismissUpdateCheckMessage()
            },
            onSave = {
                onUpdateNotificationSettings(it)
                showSettings = false
                onDismissUpdateCheckMessage()
            },
        )
    }

    selectedDay?.let { day ->
        AlertDialog(
            onDismissRequest = { selectedDay = null },
            title = { Text("${day.date} の予報") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${weatherIcon(day.weatherCode)} ${weatherLabel(day.weatherCode)}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("最高 ${day.maxTemperatureC.tempText()} / 最低 ${day.minTemperatureC.tempText()}")
                    Text("降水確率 ${day.maxPrecipitationProbability?.let { "$it%" } ?: "--"} / 雨量 ${day.precipitationSumMm.mmText()}")
                }
            },
            confirmButton = { TextButton(onClick = { selectedDay = null }) { Text("閉じる") } },
        )
    }
}

@Composable
private fun RedesignHeader(
    state: WeatherUiState,
    onRefresh: () -> Unit,
    onUseDeviceLocation: () -> Unit,
    onSelectLocation: (WeatherLocation) -> Unit,
    onLocations: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(RedesignSoftBlue),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.LocationOn, null, tint = RedesignBlue, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        state.selectedLocation.name,
                        color = RedesignText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text("更新 ${freshnessText(state.snapshot?.updatedAtMillis)}", color = RedesignMuted, fontSize = 11.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                HeaderButton(onUseDeviceLocation) { Icon(Icons.Outlined.MyLocation, "現在地") }
                HeaderButton(onRefresh, enabled = !state.isRefreshing) {
                    if (state.isRefreshing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Outlined.Refresh, "更新")
                }
                HeaderButton(onSettings) { Icon(Icons.Outlined.Settings, "設定") }
                HeaderButton(onLocations) { Icon(Icons.Outlined.Add, "地点") }
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.savedLocations) { location ->
                val selected = location.sameSavedPlaceAs(state.selectedLocation)
                Box(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) RedesignBlue else RedesignSoftBlue)
                        .clickable { onSelectLocation(location) }
                        .padding(horizontal = 13.dp, vertical = 8.dp),
                ) {
                    Text(
                        location.name,
                        color = if (selected) Color.White else RedesignText,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderButton(onClick: () -> Unit, enabled: Boolean = true, content: @Composable () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(38.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = Color.Transparent,
            contentColor = RedesignText,
        ),
        content = content,
    )
}

@Composable
private fun RedesignHero(snapshot: WeatherSnapshot) {
    val radar = snapshot.freshRadarPrecipitation()
    val nextRain = snapshot.nextExpectedPrecipitation(maxHours = 24)
    val code = snapshot.effectiveCurrentWeatherCode()
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            Modifier
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF65B4F2), Color(0xFFD8ECFF)),
                        start = Offset.Zero,
                        end = Offset(900f, 1200f),
                    ),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        snapshot.current.temperatureC?.roundToInt()?.let { "$it°" } ?: "--°",
                        color = RedesignText,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Light,
                    )
                    Text("体感 ${snapshot.current.apparentTemperatureC.tempText()}", color = Color.White.copy(alpha = 0.92f), fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(weatherIcon(code), fontSize = 52.sp)
                    Text(snapshot.effectiveCurrentWeatherLabel(), color = RedesignText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            StatusCard(
                background = if (radar?.isRaining() == true) Color(0xFFFFF1EA) else RedesignSoftGreen,
                icon = if (radar?.isRaining() == true) "☔" else "✓",
                iconColor = if (radar?.isRaining() == true) RedesignRed else RedesignGreen,
                title = when {
                    radar?.isRaining() == true -> "現在 ${radar.intensityLabel()}"
                    radar != null -> "現在 雨は降っていません"
                    snapshot.location.isInJapan() -> "現在の雨を確認できません"
                    else -> "現在 ${snapshot.effectiveCurrentWeatherLabel()}"
                },
                detail = when {
                    radar != null -> "降水強度 ${formatNumber(radar.intensityLowerBoundMmPerHour)}mm/h（気象庁レーダー）"
                    else -> "実況レーダー未取得・モデル予報を表示"
                },
            )

            if (nextRain?.isCurrent != true) {
                StatusCard(
                    background = RedesignSoftPurple,
                    icon = "☂",
                    iconColor = RedesignPurple,
                    title = if (nextRain == null) "24時間以内に雨量予報なし" else "${clockText(nextRain.time)}ごろ ${rainLabel(nextRain.amountMm)}の可能性",
                    detail = if (nextRain == null) "急な変化は雨雲レーダーで確認" else buildString {
                        nextRain.probability?.let { append("降水確率 $it% / ") }
                        append("降水量 ${formatNumber(nextRain.amountMm)}mm")
                    },
                )
            }
        }
    }
}

@Composable
private fun StatusCard(background: Color, icon: String, iconColor: Color, title: String, detail: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(background).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.72f)), contentAlignment = Alignment.Center) {
            Text(icon, color = iconColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = RedesignText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(detail, color = RedesignMuted, fontSize = 10.sp, maxLines = 2)
        }
    }
}

@Composable
private fun RedesignHourly(snapshot: WeatherSnapshot) {
    val hours = snapshot.hourly
        .filter { hour ->
            runCatching {
                !LocalDateTime.parse(hour.time).isBefore(LocalDateTime.now(snapshot.forecastZoneId()).withMinute(0).withSecond(0).withNano(0))
            }.getOrDefault(false)
        }
        .take(8)
    Card(
        colors = CardDefaults.cardColors(containerColor = RedesignCard),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(vertical = 15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("1時間ごとの予報", Modifier.padding(horizontal = 15.dp), color = RedesignText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            if (hours.isEmpty()) {
                Text("時間予報を取得できません", Modifier.padding(horizontal = 15.dp), color = RedesignMuted)
            } else {
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(hours) { hour -> HourCell(hour, hours.firstOrNull() == hour) }
                }
                TemperatureLine(hours.mapNotNull { it.temperatureC }, Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 20.dp))
            }
        }
    }
}

@Composable
private fun HourCell(hour: com.example.weather.data.model.HourlyWeather, current: Boolean) {
    Column(Modifier.width(58.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(if (current) "現在" else clockText(hour.time), color = if (current) RedesignBlue else RedesignMuted, fontSize = 10.sp, fontWeight = if (current) FontWeight.Bold else FontWeight.Normal)
        Text(weatherIcon(hour.weatherCode), fontSize = 23.sp)
        Text(hour.temperatureC.tempText(), color = RedesignText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(hour.precipitationProbability?.let { "$it%" } ?: "--", color = RedesignBlue, fontSize = 9.sp)
    }
}

@Composable
private fun TemperatureLine(values: List<Double>, modifier: Modifier) {
    if (values.size < 2) return
    val min = values.minOrNull() ?: return
    val max = values.maxOrNull() ?: return
    val range = (max - min).takeIf { it > 0.1 } ?: 1.0
    Canvas(modifier) {
        val step = size.width / (values.size - 1)
        var previous: Offset? = null
        values.forEachIndexed { index, value ->
            val y = size.height - 4.dp.toPx() - (((value - min) / range).toFloat() * (size.height - 8.dp.toPx()))
            val point = Offset(index * step, y)
            previous?.let { drawLine(RedesignBlue, it, point, 2.dp.toPx(), cap = StrokeCap.Round) }
            drawCircle(RedesignBlue, radius = 3.dp.toPx(), center = point)
            drawCircle(Color.White, radius = 1.2.dp.toPx(), center = point)
            previous = point
        }
    }
}

@Composable
private fun RedesignTodayTomorrow(snapshot: WeatherSnapshot, onClick: (DailyWeather) -> Unit) {
    val days = snapshot.forecastDays().take(2)
    if (days.isEmpty()) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        days.forEachIndexed { index, day ->
            val dayHours = snapshot.hourly.filter { it.time.startsWith(day.date) }
            Card(
                modifier = Modifier.weight(1f).clickable { onClick(day) },
                colors = CardDefaults.cardColors(containerColor = RedesignCard),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (index == 0) "今日" else "明日", color = RedesignText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(weatherIcon(day.weatherCode), fontSize = 28.sp)
                    }
                    Text(weatherLabel(day.weatherCode), color = RedesignMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(day.maxTemperatureC.tempText(), color = RedesignRed, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                        Text(" / ", color = RedesignMuted)
                        Text(day.minTemperatureC.tempText(), color = RedesignLow, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    Text("💧 ${day.effectiveMaxProbability(dayHours)?.let { "$it%" } ?: "--"}   ☔ ${day.effectivePrecipitationSum(dayHours).mmText()}", color = RedesignBlue, fontSize = 10.sp)
                }
            }
        }
        if (days.size == 1) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun RedesignMetrics(snapshot: WeatherSnapshot) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Metric("💧", "湿度", snapshot.current.humidityPercent?.let { "$it%" } ?: "--", Modifier.weight(1f))
        Metric("↗", "風", snapshot.current.windSpeedKmh?.let { "${formatNumber(it / 3.6)}m/s" } ?: "--", Modifier.weight(1f))
        Metric("◉", "気圧", snapshot.current.pressureHpa?.roundToInt()?.let { "${it}hPa" } ?: "--", Modifier.weight(1f))
    }
}

@Composable
private fun Metric(icon: String, label: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = RedesignCard), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$icon  $label", color = RedesignMuted, fontSize = 10.sp)
            Text(value, color = RedesignText, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
        }
    }
}

@Composable
private fun RedesignSources(snapshot: WeatherSnapshot) {
    Card(colors = CardDefaults.cardColors(containerColor = RedesignSoftBlue), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("データについて", color = RedesignText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            SourceRow("◉", "現在の雨", if (snapshot.location.isInJapan()) "気象庁レーダー（実況）" else "モデル予報")
            SourceRow("▦", "予報", snapshot.forecastSource)
            SourceRow("↻", "最終更新", Instant.ofEpochMilli(snapshot.updatedAtMillis).atZone(snapshot.forecastZoneId()).format(DateTimeFormatter.ofPattern("M/d H:mm")))
        }
    }
}

@Composable
private fun SourceRow(icon: String, title: String, detail: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
            Text(icon, color = RedesignBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = RedesignText, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            Text(detail, color = RedesignMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun RedesignLocationDialog(
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
            LazyColumn(Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("都市を検索") },
                        placeholder = { Text("Heidelberg, Berlin, 東京") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            FilledIconButton(onClick = { onSearchLocations(query) }, modifier = Modifier.size(38.dp)) {
                                Icon(Icons.Outlined.Search, "検索")
                            }
                        },
                    )
                }
                if (state.isSearchingLocation) item { CircularProgressIndicator(Modifier.size(20.dp)) }
                state.locationSearchMessage?.let { item { Text(it, color = RedesignMuted, fontSize = 11.sp) } }
                items(state.searchResults.take(8)) { location ->
                    Text(
                        location.name,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(RedesignSoftBlue).clickable { onSelectLocation(location) }.padding(12.dp),
                        color = RedesignText,
                    )
                }
                item { HorizontalDivider() }
                item { TextButton(onClick = onUseDeviceLocation) { Text("現在地を使う") } }
                items(state.savedLocations) { location ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(location.name, Modifier.weight(1f).clickable { onSelectLocation(location) }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        TextButton(onClick = { onMoveLocation(location, -1) }) { Text("↑") }
                        TextButton(onClick = { onMoveLocation(location, 1) }) { Text("↓") }
                        TextButton(onClick = { onDeleteLocation(location) }) { Text("削除") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("閉じる") } },
    )
}

@Composable
private fun RedesignSettingsDialog(
    settings: NotificationSettings,
    appVersionName: String,
    isCheckingUpdate: Boolean,
    updateCheckMessage: String?,
    onCheckUpdate: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (NotificationSettings) -> Unit,
) {
    var draft by remember(settings) { mutableStateOf(settings) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("設定") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SettingSwitch("雨の接近通知", draft.rainNotificationsEnabled) { draft = draft.copy(rainNotificationsEnabled = it) }
                SettingSwitch("重要な気象情報", draft.disasterNotificationsEnabled) { draft = draft.copy(disasterNotificationsEnabled = it) }
                HorizontalDivider()
                Text("雨の通知条件", color = RedesignMuted, fontSize = 11.sp)
                Text("${draft.rainLookAheadHours}時間以内 / ${draft.rainProbabilityThreshold}%以上 / ${formatNumber(draft.rainAmountThresholdMm)}mm以上", fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    TextButton(onClick = { draft = draft.copy(rainLookAheadHours = (draft.rainLookAheadHours - 1).coerceAtLeast(1)) }) { Text("時間−") }
                    TextButton(onClick = { draft = draft.copy(rainLookAheadHours = (draft.rainLookAheadHours + 1).coerceAtMost(12)) }) { Text("時間＋") }
                    TextButton(onClick = { draft = draft.copy(rainProbabilityThreshold = (draft.rainProbabilityThreshold + 10).coerceAtMost(100)) }) { Text("確率＋") }
                }
                HorizontalDivider()
                Text("v$appVersionName", color = RedesignMuted, fontSize = 11.sp)
                OutlinedButton(onClick = onCheckUpdate, enabled = !isCheckingUpdate) {
                    Text(if (isCheckingUpdate) "確認中" else "アップデートを確認")
                }
                updateCheckMessage?.let { Text(it, color = RedesignBlue, fontSize = 11.sp) }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(draft) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("閉じる") } },
    )
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = RedesignText, fontWeight = FontWeight.Medium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun clockText(value: String): String = runCatching {
    LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("H:mm"))
}.getOrElse { value.takeLast(5) }

private fun freshnessText(value: Long?): String {
    if (value == null) return "--"
    val minutes = ((System.currentTimeMillis() - value).coerceAtLeast(0L) / 60_000L)
    return when {
        minutes < 1 -> "たった今"
        minutes < 60 -> "${minutes}分前"
        else -> "${minutes / 60}時間前"
    }
}

private fun rainLabel(amountMm: Double): String = when {
    amountMm >= 10.0 -> "強い雨"
    amountMm >= 1.0 -> "雨"
    else -> "弱い雨"
}

private fun formatNumber(value: Double): String = if (abs(value - value.roundToInt()) < 0.05) {
    value.roundToInt().toString()
} else {
    String.format(Locale.US, "%.1f", value)
}

private fun Double?.tempText(): String = this?.roundToInt()?.let { "$it°" } ?: "--°"
private fun Double?.mmText(): String = this?.let { "${formatNumber(it)}mm" } ?: "--mm"
