package com.example.weather.ui

import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weather.WeatherUiState
import com.example.weather.data.model.AirQuality
import com.example.weather.data.model.CurrentTemperatureKind
import com.example.weather.data.model.DailyWeather
import com.example.weather.data.model.DisasterSummary
import com.example.weather.data.model.ExpectedPrecipitation
import com.example.weather.data.model.HourlyWeather
import com.example.weather.data.model.MinutelyWeather
import com.example.weather.data.model.NotificationSettings
import com.example.weather.data.model.PresetLocations
import com.example.weather.data.model.RadarPrecipitation
import com.example.weather.data.model.WeatherLocation
import com.example.weather.data.model.sameSavedPlaceAs
import com.example.weather.data.model.WeatherSnapshot
import com.example.weather.data.model.freshRadarPrecipitation
import com.example.weather.data.model.effectiveMaxProbability
import com.example.weather.data.model.effectivePrecipitationSum
import com.example.weather.data.model.effectiveCurrentWeatherCode
import com.example.weather.data.model.effectiveCurrentWeatherLabel
import com.example.weather.data.model.forecastDays
import com.example.weather.data.model.forecastZoneId
import com.example.weather.data.model.hasMeasurablePrecipitation
import com.example.weather.data.model.hasFreshObservation
import com.example.weather.data.model.intensityLabel
import com.example.weather.data.model.isRaining
import com.example.weather.data.model.maxPrecipitationProbabilityFromNow
import com.example.weather.data.model.nextExpectedPrecipitation
import com.example.weather.data.model.radarObservationStatus
import com.example.weather.data.model.today
import com.example.weather.data.model.weatherIcon
import com.example.weather.data.model.weatherLabel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
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
    var showLocationDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedDayDate by remember { mutableStateOf<String?>(null) }
    val snapshot = state.snapshot
    val disasterSummary = state.disasterSummary
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
    ) {
        item {
            HomeHeader(
                selectedLocation = state.selectedLocation,
                savedLocations = state.savedLocations,
                freshness = formatFreshness(snapshot?.updatedAtMillis),
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                onUseDeviceLocation = onUseDeviceLocation,
                onLocation = { showLocationDialog = true },
                onSettings = { showSettingsDialog = true },
                onSelectLocation = onSelectLocation,
            )
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
            val displayDays = snapshot.forecastDays().take(14)
            if (disasterSummary?.hasImportantInfo == true) {
                item {
                    DisasterSummaryCard(
                        summary = disasterSummary,
                        onClick = {
                            uriHandler.openUri(googleWeatherSearchUrl(disasterSummary))
                        },
                    )
                }
            }
            item { DailyForecastPanel(snapshot, displayDays) }
            item { CurrentConditionsPanel(snapshot) }
            item {
                HomeWeeklySection(
                    days = displayDays,
                    hourly = snapshot.hourly,
                    onDayClick = { selectedDayDate = it.date },
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        snapshot.forecastSource,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                    if (snapshot.temperatureForecast.modelNames.isNotEmpty()) {
                        Text(
                            temperatureVerificationText(snapshot),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                    }
                }
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

    if (showSettingsDialog) {
        NotificationSettingsDialog(
            settings = state.notificationSettings,
            appVersionName = appVersionName,
            isCheckingUpdate = state.isCheckingUpdate,
            updateCheckMessage = state.updateCheckMessage,
            onCheckUpdate = onCheckUpdate,
            onDismiss = {
                showSettingsDialog = false
                onDismissUpdateCheckMessage()
            },
            onSave = {
                onUpdateNotificationSettings(it)
                showSettingsDialog = false
                onDismissUpdateCheckMessage()
            },
        )
    }

    selectedDayDate
        ?.let { date -> snapshot?.forecastDays()?.firstOrNull { it.date == date } }
        ?.let { day ->
        DayDetailDialog(
            day = day,
            dayHours = snapshot?.hourly?.forDate(day.date).orEmpty(),
            timezone = snapshot?.timezone ?: "Asia/Tokyo",
            radarPrecipitation = snapshot?.freshRadarPrecipitation(),
            onDismiss = { selectedDayDate = null },
        )
    }
}

private fun temperatureVerificationText(snapshot: WeatherSnapshot): String {
    val metadata = snapshot.temperatureForecast
    return when {
        metadata.hasAdaptiveWeights ->
            "気温モデル: 実況との比較${metadata.verificationSampleCount}件を重みに反映"
        metadata.verificationSampleCount > 0 ->
            "気温モデル: 実況との比較${metadata.verificationSampleCount}件・重み調整用データを蓄積中"
        else -> "気温モデル: 実況との比較データを蓄積中"
    }
}

@Composable
private fun HomeHeader(
    selectedLocation: WeatherLocation,
    savedLocations: List<WeatherLocation>,
    freshness: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onUseDeviceLocation: () -> Unit,
    onLocation: () -> Unit,
    onSettings: () -> Unit,
    onSelectLocation: (WeatherLocation) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(WeatherPalette.Header),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 5.dp, top = 6.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = WeatherPalette.Rain,
                    modifier = Modifier.size(20.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        selectedLocation.name,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        "表示中・$freshness",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.62f),
                        maxLines = 1,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(
                    onClick = onUseDeviceLocation,
                    modifier = Modifier.size(38.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = WeatherPalette.Rain,
                    ),
                ) {
                    Icon(Icons.Outlined.MyLocation, contentDescription = "現在地に戻る", modifier = Modifier.size(21.dp))
                }
                FilledTonalIconButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    modifier = Modifier.size(38.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                    ),
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Icon(Icons.Outlined.Refresh, contentDescription = "更新", modifier = Modifier.size(21.dp))
                    }
                }
                FilledTonalIconButton(
                    onClick = onSettings,
                    modifier = Modifier.size(38.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Outlined.Tune, contentDescription = "設定", modifier = Modifier.size(21.dp))
                }
                FilledTonalIconButton(
                    onClick = onLocation,
                    modifier = Modifier.size(38.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "地点を追加", modifier = Modifier.size(21.dp))
                }
            }
        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            items(savedLocations) { location ->
                val selected = location.sameSavedPlaceAs(selectedLocation)
                Row(
                    Modifier
                        .widthIn(min = 58.dp, max = 112.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (selected) Color.White else Color.White.copy(alpha = 0.07f))
                        .clickable { onSelectLocation(location) }
                        .padding(horizontal = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (selected) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(WeatherPalette.Rain),
                        )
                        Spacer(Modifier.width(5.dp))
                    }
                    Text(
                        location.name,
                        maxLines = 1,
                        color = if (selected) WeatherPalette.Header else Color.White.copy(alpha = 0.72f),
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyForecastPanel(snapshot: WeatherSnapshot, displayDays: List<DailyWeather>) {
    val days = displayDays.take(2)
    val hours = snapshot.hourly.nextHours(snapshot.hourly.size, snapshot.timezone)
    val today = snapshot.today()
    val radar = snapshot.freshRadarPrecipitation()
    val isJapanRadarArea = snapshot.location.latitude in 20.0..48.0 &&
        snapshot.location.longitude in 118.0..150.0
    val showRadarStatus = radar?.isRaining() == true ||
        (isJapanRadarArea && radar == null)
    SectionCard(containerColor = WeatherPalette.ForecastSurface) {
        Column {
            if (showRadarStatus) {
                CurrentRadarStatusBanner(snapshot)
                HorizontalDivider(color = WeatherPalette.Outline)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                days.forEachIndexed { index, day ->
                    val previousDate = runCatching { LocalDate.parse(day.date).minusDays(1).toString() }.getOrNull()
                    val previousDay = snapshot.daily.firstOrNull { it.date == previousDate }
                    DailyForecastColumn(
                        day = day,
                        previousDay = previousDay,
                        dayHours = snapshot.hourly.forDate(day.date),
                        relativeLabel = relativeDayLabel(day.date, snapshot.timezone),
                        currentRadar = radar,
                        modifier = Modifier.weight(1f),
                    )
                    if (index == 0 && days.size > 1) {
                        Box(
                            Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(WeatherPalette.Outline),
                        )
                    }
                }
            }
            ForecastTip(snapshot)
            HorizontalDivider(color = WeatherPalette.Outline)
            HourlyForecastTable(hours, snapshot.timezone, radar)
            SunTimesRow(sunrise = today?.sunrise, sunset = today?.sunset)
        }
    }
}

@Composable
private fun CurrentRadarStatusBanner(snapshot: WeatherSnapshot) {
    val isRaining = snapshot.freshRadarPrecipitation()?.isRaining() == true
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (isRaining) Color(0xFF381A1D) else Color(0xFF332B18))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = if (isRaining) Color(0xFFFF8A80) else Color(0xFFFFC857),
            modifier = Modifier.size(22.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (isRaining) snapshot.effectiveCurrentWeatherLabel() else "現在の雨を確認できません",
                color = if (isRaining) Color(0xFFFFB4AB) else Color(0xFFFFDDA1),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                snapshot.radarObservationStatus(),
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun DailyForecastColumn(
    day: DailyWeather,
    previousDay: DailyWeather?,
    dayHours: List<HourlyWeather>,
    relativeLabel: String?,
    currentRadar: RadarPrecipitation?,
    modifier: Modifier = Modifier,
) {
    val probability = day.effectiveMaxProbability(dayHours)
    val isObservedRain = relativeLabel == "今日" && currentRadar?.isRaining() == true
    val displayWeatherCode = if (isObservedRain) 65 else day.weatherCode
    val displayWeatherLabel = if (isObservedRain) {
        "現在 ${currentRadar?.intensityLabel()}"
    } else {
        weatherLabel(day.weatherCode)
    }
    Column(
        modifier.padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        WeekendDateLabel(
            date = day.date,
            prefix = relativeLabel,
            fontSizeSp = 17,
            fontWeight = FontWeight.Bold,
        )
        WeatherGlyph(code = displayWeatherCode, size = 80.dp)
        Text(
            displayWeatherLabel,
            fontSize = if (displayWeatherLabel.length >= 7) 15.sp else 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(
                Icons.Outlined.WaterDrop,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = WeatherPalette.Rain,
            )
            Text(probability.percentText(), fontSize = 15.sp, maxLines = 1)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
            TemperatureWithPreviousDay(
                temperature = day.maxTemperatureC,
                previousTemperature = previousDay?.maxTemperatureC,
                color = WeatherPalette.HighTemperature,
            )
            TemperatureWithPreviousDay(
                temperature = day.minTemperatureC,
                previousTemperature = previousDay?.minTemperatureC,
                color = WeatherPalette.LowTemperature,
            )
        }
        dailyTemperatureRangeText(day)?.let { range ->
            Text(
                range,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (isObservedRain) {
            Text(
                "一日予報 ${weatherLabel(day.weatherCode)}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("最高・最低とも前日比", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TemperatureWithPreviousDay(
    temperature: Double?,
    previousTemperature: Double?,
    color: Color,
) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            "${temperature?.roundText() ?: "--"}°",
            color = color,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )
        temperatureDifferenceText(temperature, previousTemperature)?.let { difference ->
            Text(
                difference,
                color = color.copy(alpha = 0.78f),
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
    }
}

fun temperatureDifferenceText(current: Double?, previous: Double?): String? {
    if (current == null || previous == null) return null
    val difference = current.roundToInt() - previous.roundToInt()
    return "[${if (difference > 0) "+" else ""}$difference]"
}

fun temperatureRangeText(low: Double?, high: Double?): String? {
    if (low == null || high == null || high - low < 0.5) return null
    return "${low.roundText()}〜${high.roundText()}°"
}

private fun dailyTemperatureRangeText(day: DailyWeather): String? {
    val highRange = temperatureRangeText(day.maxTemperatureLowC, day.maxTemperatureHighC)
    val lowRange = temperatureRangeText(day.minTemperatureLowC, day.minTemperatureHighC)
    val parts = buildList {
        highRange?.let { add("最高 $it") }
        lowRange?.let { add("最低 $it") }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" / ")?.let { "予報幅 $it" }
}

@Composable
private fun ForecastTip(snapshot: WeatherSnapshot) {
    val today = snapshot.today()
    val remainingToday = today?.let { day ->
        snapshot.hourly.nextHours(snapshot.hourly.size, snapshot.timezone).filter { it.time.take(10) == day.date }
    }.orEmpty()
    val remainingProbability = remainingToday.mapNotNull { it.precipitationProbability }.maxOrNull()
    val remainingAmount = remainingToday.mapNotNull { it.precipitationMm }.takeIf { it.isNotEmpty() }?.sum()
    val signal = rainSignal(remainingProbability, remainingAmount)
    val expected = snapshot.nextExpectedPrecipitation(maxHours = 48)
    val expectedDate = expected?.time?.let { runCatching { LocalDateTime.parse(it).toLocalDate() }.getOrNull() }
    val todayDate = LocalDate.now(snapshot.forecastZoneId())
    val action = if (expected != null && !expected.isCurrent && expectedDate != todayDate) {
        "今日は傘不要寄り"
    } else {
        signal.action
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(WeatherPalette.SurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(if (action == "傘を持つ") "☂️" else "💡", fontSize = 20.sp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(action, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = signal.color)
            Text(nextRainText(snapshot), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun HourlyForecastTable(
    hours: List<HourlyWeather>,
    timezone: String = "Asia/Tokyo",
    radarPrecipitation: RadarPrecipitation? = null,
) {
    if (hours.isEmpty()) {
        Text(
            "時間別予報を取得できません",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val listState = rememberLazyListState()
    val visibleDate by remember(hours, listState) {
        derivedStateOf {
            hours.getOrNull(listState.firstVisibleItemIndex)
                ?.time
                ?.let { runCatching { LocalDateTime.parse(it).toLocalDate() }.getOrNull() }
        }
    }
    val lastTime = hours.lastOrNull()?.time?.let(::formatDateHourLabel) ?: "不明"
    Column {
        Text(
            "表示中 ${visibleDate?.let { formatDateWithWeekday(it.toString()) } ?: "--"} ・ ${hours.size}時間分・$lastTime まで",
            modifier = Modifier.padding(start = 48.dp, top = 8.dp, bottom = 2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
        ) {
            Column(Modifier.width(48.dp)) {
                ForecastTableLabel("日時", 42)
                ForecastTableLabel("", 48)
                ForecastTableLabel("気温", 40)
                ForecastTableLabel("降水", 30)
                ForecastTableLabel("雨量", 30)
                ForecastTableLabel("湿度", 30)
                ForecastTableLabel("風", 52)
            }
            LazyRow(
                modifier = Modifier.weight(1f),
                state = listState,
            ) {
                items(hours, key = { it.time }) { hour ->
                    val isObservedRain = isCurrentHour(hour.time, timezone) &&
                        radarPrecipitation?.isRaining() == true
                    Column(
                        Modifier.width(72.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ForecastTimeValue(hour.time, timezone)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            WeatherGlyph(
                                code = if (isObservedRain) 65 else hour.weatherCode,
                                size = 36.dp,
                            )
                        }
                        ForecastTemperatureValue(hour)
                        ForecastTableValue(
                            if (isObservedRain) "観測" else hour.precipitationProbability.percentText(),
                            30,
                            13,
                            isObservedRain,
                            WeatherPalette.Rain,
                        )
                        ForecastTableValue(
                            if (isObservedRain) {
                                "${radarRateText(radarPrecipitation?.intensityLowerBoundMmPerHour)}+mm/h"
                            } else {
                                hour.precipitationMm.mmText()
                            },
                            30,
                            if (isObservedRain) 10 else 12,
                            isObservedRain,
                        )
                        ForecastTableValue(hour.humidityPercent.percentText(), 30, 12, false)
                        ForecastWindValue(hour)
                    }
                }
            }
        }
    }
}

fun radarRateText(value: Double?): String = when {
    value == null -> "--"
    value < 1.0 -> value.oneDecimal()
    else -> value.roundText()
}

@Composable
private fun ForecastTimeValue(time: String, timezone: String) {
    val parsed = runCatching { LocalDateTime.parse(time) }.getOrNull()
    val dateColor = when (parsed?.dayOfWeek) {
        java.time.DayOfWeek.SATURDAY -> WeatherPalette.LowTemperature
        java.time.DayOfWeek.SUNDAY -> WeatherPalette.HighTemperature
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        Modifier
            .fillMaxWidth()
            .height(42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            parsed?.format(DateTimeFormatter.ofPattern("M/d")) ?: "--",
            color = dateColor,
            fontSize = 10.sp,
            maxLines = 1,
        )
        Text(
            if (isCurrentHour(time, timezone)) "今" else formatHourOnly(time),
            fontSize = 12.sp,
            fontWeight = if (isCurrentHour(time, timezone)) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@Composable
private fun ForecastWindValue(hour: HourlyWeather) {
    val rotation = ((hour.windDirectionDeg ?: 0) + 180).toFloat()
    val speedMs = hour.windSpeedKmh?.div(3.6)
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Icon(
                Icons.Filled.Navigation,
                contentDescription = null,
                tint = WeatherPalette.Rain,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(rotation),
            )
            Text(
                "${windDirectionText(hour.windDirectionDeg)} ${speedMs?.oneDecimal() ?: "--"}m/s",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SunTimesRow(sunrise: String?, sunset: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(WeatherPalette.SurfaceVariant)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SunTime("日の出", sunrise, WeatherPalette.Tertiary)
        Box(
            Modifier
                .width(1.dp)
                .height(24.dp)
                .background(WeatherPalette.Outline),
        )
        SunTime("日の入", sunset, WeatherPalette.LowTemperature)
    }
}

@Composable
private fun SunTime(label: String, time: String?, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(
            Icons.Filled.WbSunny,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Text("$label ${formatTimeOnly(time)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ForecastTableLabel(label: String, heightDp: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(heightDp.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ForecastTableValue(
    value: String,
    heightDp: Int,
    fontSizeSp: Int,
    bold: Boolean,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(heightDp.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            value,
            fontSize = fontSizeSp.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun ForecastTemperatureValue(hour: HourlyWeather) {
    val range = temperatureRangeText(hour.temperatureLowC, hour.temperatureHighC)
    Column(
        Modifier
            .fillMaxWidth()
            .height(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "${hour.temperatureC?.roundText() ?: "--"}°",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        if (range != null) {
            Text(
                range,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CurrentConditionsPanel(snapshot: WeatherSnapshot) {
    val today = snapshot.today()
    val currentWeatherCode = snapshot.effectiveCurrentWeatherCode()
    val currentWeatherLabel = snapshot.effectiveCurrentWeatherLabel()
    val radar = snapshot.freshRadarPrecipitation()
    SectionCard(containerColor = WeatherPalette.ForecastSurface) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("いまの天気", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        currentWeatherLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
                WeatherGlyph(code = currentWeatherCode, size = 48.dp)
            }
            CurrentTemperatureSourceRow(snapshot)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        if (radar?.isRaining() == true) Color(0xFF381A1D) else WeatherPalette.SurfaceVariant,
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    if (radar?.isRaining() == true) Icons.Filled.WarningAmber else Icons.Outlined.WaterDrop,
                    contentDescription = null,
                    tint = if (radar?.isRaining() == true) Color(0xFFFF8A80) else WeatherPalette.Rain,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    snapshot.radarObservationStatus(),
                    color = if (radar?.isRaining() == true) Color(0xFFFFB4AB) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = if (radar?.isRaining() == true) FontWeight.Bold else FontWeight.Normal,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CurrentMetric(
                    when {
                        snapshot.currentTemperatureSource.hasFreshObservation() -> "実況気温"
                        snapshot.currentTemperatureSource.kind == CurrentTemperatureKind.OBSERVATION -> "前回気温"
                        else -> "推定気温"
                    },
                    snapshot.current.temperatureC.temperatureText(),
                    Modifier.weight(1f),
                    29,
                )
                CurrentMetric("気圧", snapshot.current.pressureHpa.pressureText(), Modifier.weight(1f), 22)
                CurrentMetric("湿度", snapshot.current.humidityPercent.percentText(), Modifier.weight(1f), 22)
            }
            HorizontalDivider(color = WeatherPalette.Outline)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CurrentMetric("体感", snapshot.current.apparentTemperatureC.temperatureText(), Modifier.weight(1f), 15)
                CurrentMetric("風", windText(snapshot.current.windSpeedKmh, snapshot.current.windDirectionDeg), Modifier.weight(1f), 15)
                CurrentMetric("AQI", snapshot.airQuality?.europeanAqi?.toString() ?: "--", Modifier.weight(1f), 15)
                CurrentMetric("UV", today?.uvIndexMax.uvText(), Modifier.weight(1f), 15)
            }
            RadarPreview(
                location = snapshot.location,
                refreshKey = snapshot.updatedAtMillis,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(5.dp)),
            )
        }
    }
}

@Composable
private fun CurrentTemperatureSourceRow(snapshot: WeatherSnapshot) {
    val source = snapshot.currentTemperatureSource
    val isFreshObservation = source.hasFreshObservation()
    val isStoredObservation = source.kind == CurrentTemperatureKind.OBSERVATION
    val detail = if (isStoredObservation) {
        buildList {
            source.stationName?.let(::add)
            source.dataTimeMillis?.let { add("${formatHourMinute(it)}観測") }
            source.distanceKm?.let { add("約${it.oneDecimal()}km") }
        }.joinToString(" ・ ")
    } else {
        buildList {
            add(source.provider)
            temperatureRangeText(source.rangeLowC, source.rangeHighC)?.let { add("予報幅 $it") }
        }.joinToString(" ・ ")
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(5.dp))
            .background(if (isFreshObservation) Color(0xFF123247) else WeatherPalette.SurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isFreshObservation) WeatherPalette.Rain else WeatherPalette.Tertiary),
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                when {
                    isFreshObservation -> "実況値"
                    isStoredObservation -> "前回の実況値（20分超）"
                    else -> "予報モデルによる推定値"
                },
                color = if (isFreshObservation) WeatherPalette.Rain else WeatherPalette.Tertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                detail.ifBlank { "取得元を確認できません" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun CurrentMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    fontSizeSp: Int,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = fontSizeSp.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun WeatherGlyph(code: Int?, size: Dp, modifier: Modifier = Modifier) {
    val cloudColor = Color(0xFFB8B9BD)
    val sunColor = Color(0xFFFF8A34)
    val rainColor = Color(0xFF4F8DFF)
    val lightningColor = Color(0xFFFFC928)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        when (code) {
            0 -> Icon(
                Icons.Filled.WbSunny,
                contentDescription = weatherLabel(code),
                tint = sunColor,
                modifier = Modifier.size(size * 0.82f),
            )

            1, 2 -> {
                Icon(
                    Icons.Filled.WbSunny,
                    contentDescription = null,
                    tint = sunColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(size * 0.62f),
                )
                Icon(
                    Icons.Filled.Cloud,
                    contentDescription = weatherLabel(code),
                    tint = cloudColor,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(size * 0.78f),
                )
            }

            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> {
                Icon(
                    Icons.Filled.Cloud,
                    contentDescription = weatherLabel(code),
                    tint = cloudColor,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(size * 0.8f),
                )
                Icon(
                    Icons.Filled.WaterDrop,
                    contentDescription = null,
                    tint = rainColor,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(size * 0.43f),
                )
            }

            71, 73, 75, 77, 85, 86 -> {
                Icon(
                    Icons.Filled.Cloud,
                    contentDescription = weatherLabel(code),
                    tint = cloudColor,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(size * 0.8f),
                )
                Icon(
                    Icons.Filled.AcUnit,
                    contentDescription = null,
                    tint = Color(0xFF8CCBFF),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(size * 0.46f),
                )
            }

            95, 96, 99 -> {
                Icon(
                    Icons.Filled.Cloud,
                    contentDescription = weatherLabel(code),
                    tint = cloudColor,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(size * 0.8f),
                )
                Icon(
                    Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = lightningColor,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(size * 0.5f),
                )
            }

            else -> Icon(
                Icons.Filled.Cloud,
                contentDescription = weatherLabel(code),
                tint = cloudColor,
                modifier = Modifier.size(size * 0.82f),
            )
        }
    }
}

@Composable
private fun NotificationSettingsDialog(
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
                SettingSwitchRow(
                    title = "雨の接近",
                    subtitle = "指定時間内に雨が強まりそうな時だけ通知",
                    checked = draft.rainNotificationsEnabled,
                    onCheckedChange = { draft = draft.copy(rainNotificationsEnabled = it) },
                )
                SettingStepperRow(
                    label = "判定時間",
                    value = "${draft.rainLookAheadHours}時間以内",
                    onMinus = { draft = draft.copy(rainLookAheadHours = (draft.rainLookAheadHours - 1).coerceAtLeast(1)) },
                    onPlus = { draft = draft.copy(rainLookAheadHours = (draft.rainLookAheadHours + 1).coerceAtMost(12)) },
                )
                SettingStepperRow(
                    label = "降水確率",
                    value = "${draft.rainProbabilityThreshold}%",
                    onMinus = { draft = draft.copy(rainProbabilityThreshold = (draft.rainProbabilityThreshold - 10).coerceAtLeast(10)) },
                    onPlus = { draft = draft.copy(rainProbabilityThreshold = (draft.rainProbabilityThreshold + 10).coerceAtMost(100)) },
                )
                SettingStepperRow(
                    label = "雨量",
                    value = "${draft.rainAmountThresholdMm.oneDecimal()}mm以上",
                    onMinus = { draft = draft.copy(rainAmountThresholdMm = (draft.rainAmountThresholdMm - 0.1).coerceAtLeast(0.1)) },
                    onPlus = { draft = draft.copy(rainAmountThresholdMm = (draft.rainAmountThresholdMm + 0.1).coerceAtMost(10.0)) },
                )
                HorizontalDivider(color = Color(0xFF2C3447))
                SettingSwitchRow(
                    title = "重要な気象情報",
                    subtitle = "警報・注意報、台風情報を通知",
                    checked = draft.disasterNotificationsEnabled,
                    onCheckedChange = { draft = draft.copy(disasterNotificationsEnabled = it) },
                )
                HorizontalDivider(color = WeatherPalette.Outline)
                AppUpdateRow(
                    appVersionName = appVersionName,
                    isCheckingUpdate = isCheckingUpdate,
                    updateCheckMessage = updateCheckMessage,
                    onCheckUpdate = onCheckUpdate,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        },
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingStepperRow(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = onMinus) { Text("-") }
            TextButton(onClick = onPlus) { Text("+") }
        }
    }
}

@Composable
private fun AppUpdateRow(
    appVersionName: String,
    isCheckingUpdate: Boolean,
    updateCheckMessage: String?,
    onCheckUpdate: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("アプリのアップデート", fontWeight = FontWeight.SemiBold)
                Text("現在のバージョン v$appVersionName", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            OutlinedButton(onClick = onCheckUpdate, enabled = !isCheckingUpdate) {
                if (isCheckingUpdate) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("確認中")
                } else {
                    Icon(Icons.Outlined.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("確認")
                }
            }
        }
        if (updateCheckMessage != null) {
            Text(updateCheckMessage, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DisasterSummaryCard(summary: DisasterSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, Color(0xFF5A2A2A), MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1717)),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFFFB4AB),
                    modifier = Modifier.size(18.dp),
                )
                Text("重要な気象情報", fontSize = 13.sp, color = Color(0xFFFFB4AB), fontWeight = FontWeight.SemiBold)
            }
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
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "気象庁発表。最新情報をGoogleで確認",
                    color = Color(0xFFFFDAD6),
                    fontSize = 11.sp,
                )
                Icon(
                    Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = "Google検索を開く",
                    tint = Color(0xFFFFDAD6),
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

fun googleWeatherSearchUrl(summary: DisasterSummary): String {
    val typhoons = summary.typhoons.joinToString(" ") { "台風${it.number}号 ${it.category}" }
    val query = listOf(
        summary.officeName,
        summary.activeWarnings.joinToString(" "),
        typhoons,
        summary.warningHeadline,
        "気象庁 最新",
    ).filterNot { it.isNullOrBlank() }.joinToString(" ")
    return "https://www.google.com/search?q=${Uri.encode(query)}"
}

@Composable
private fun CurrentSummary(snapshot: WeatherSnapshot) {
    val today = snapshot.today()
    val todayHours = today?.let { snapshot.hourly.forDate(it.date) }.orEmpty()
    val next48Hours = snapshot.hourly.nextHours(48, snapshot.timezone)
    val remainingToday = today?.let { day -> next48Hours.filter { it.time.take(10) == day.date } }.orEmpty()
    val remainingProbability = remainingToday.mapNotNull { it.precipitationProbability }.maxOrNull()
    val remainingAmount = remainingToday.mapNotNull { it.precipitationMm }.takeIf { it.isNotEmpty() }?.sum()
    val radarPrecipitation = snapshot.freshRadarPrecipitation()
    val radarIsRaining = radarPrecipitation?.isRaining() == true
    val currentWeatherCode = snapshot.effectiveCurrentWeatherCode()
    val rainSignal = if (radarIsRaining) {
        rainSignal(100, radarPrecipitation?.intensityLowerBoundMmPerHour)
    } else {
        rainSignal(remainingProbability, remainingAmount)
    }
    val comfort = comfortSignal(snapshot, today, next48Hours)
    val trend = temperatureTrendLabel(next48Hours)
    val isNight = remember(snapshot) { isNightNow(snapshot) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(skyGradient(currentWeatherCode, isNight))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${snapshot.current.temperatureC?.roundText() ?: "--"}°",
                    fontSize = 92.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    "体感 ${snapshot.current.apparentTemperatureC.temperatureText()}",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 14.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(weatherIcon(currentWeatherCode), fontSize = 56.sp)
                Text(
                    snapshot.effectiveCurrentWeatherLabel(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Text(
                    currentPrecipitationText(snapshot),
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 13.sp,
                )
            }
        }
        HeroJudgmentCard(rainSignal = rainSignal, comfort = comfort)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroMetricTile("最高", "${today?.maxTemperatureC?.roundText() ?: "--"}°", Modifier.weight(1f))
            HeroMetricTile("最低", "${today?.minTemperatureC?.roundText() ?: "--"}°", Modifier.weight(1f))
            HeroMetricTile("降水", today.effectiveMaxProbability(todayHours).percentText(), Modifier.weight(1f))
            HeroMetricTile("雨量", today.effectivePrecipitationSum(todayHours).mmText(), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroInsightTile("次の雨", nextRainShortText(snapshot), Modifier.weight(1f))
            HeroInsightTile("気温", trend, Modifier.weight(1f))
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.16f))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HeroDetailMetric("体感", snapshot.current.apparentTemperatureC.temperatureText(), Modifier.weight(1f))
                HeroDetailMetric("湿度", snapshot.current.humidityPercent.percentText(), Modifier.weight(1f))
                HeroDetailMetric("風", windText(snapshot.current.windSpeedKmh, snapshot.current.windDirectionDeg), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HeroDetailMetric("気圧", snapshot.current.pressureHpa.pressureText(), Modifier.weight(1f))
                HeroDetailMetric("UV", today?.uvIndexMax.uvText(), Modifier.weight(1f))
                HeroDetailMetric("日の出/入", "${formatTimeOnly(today?.sunrise)} / ${formatTimeOnly(today?.sunset)}", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroJudgmentCard(rainSignal: RainSignal, comfort: ComfortSignal) {
    Column(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text("今すぐの判断", fontSize = 12.sp, color = Color.White.copy(alpha = 0.72f))
        Text("${rainSignal.action}・${comfort.action}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("${rainSignal.label}。${comfort.detail}", fontSize = 13.sp, color = Color.White.copy(alpha = 0.78f))
    }
}

@Composable
private fun HeroInsightTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(vertical = 10.dp, horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.68f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun HeroMetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(vertical = 11.dp, horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.72f))
    }
}

@Composable
private fun HeroDetailMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.68f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

private fun isNightNow(snapshot: WeatherSnapshot): Boolean {
    val now = LocalDateTime.now(snapshot.forecastZoneId())
    val today = snapshot.today()
    val sunrise = today?.sunrise?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
    val sunset = today?.sunset?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
    if (sunrise != null && sunset != null) {
        return now.isBefore(sunrise) || now.isAfter(sunset)
    }
    return now.hour < 6 || now.hour >= 18
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyAdviceSection(snapshot: WeatherSnapshot, next48Hours: List<HourlyWeather>) {
    val items = remember(snapshot, next48Hours) { buildDailyAdvice(snapshot, next48Hours) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("今日の判断", "傘・洗濯・服装・外出")
        items.chunked(2).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { item ->
                    AdviceCard(item, Modifier.weight(1f))
                }
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AdviceCard(item: DailyAdvice, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.heightIn(min = 102.dp),
        colors = CardDefaults.cardColors(containerColor = item.color),
        shape = MaterialTheme.shapes.small,
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(item.value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(item.detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AirQualityCard(airQuality: AirQuality?) {
    SectionCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(
                            Icons.Outlined.Air,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp),
                        )
                        Text("空気質", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
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
    val measurableHours = next48Hours.filter { it.hasMeasurablePrecipitation() }
    val peak = measurableHours.maxByOrNull { it.precipitationMm ?: 0.0 }
        ?: next48Hours.maxByOrNull { it.precipitationProbability ?: -1 }
    val signal = rainSignal(peak?.precipitationProbability, peak?.precipitationMm)
    SectionCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(
                    Icons.Outlined.WaterDrop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(15.dp),
                )
                Text("雨の見通し", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            Text(signal.label, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = signal.color)
            Text("${signal.action}。${nextRainText(snapshot)}", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            RainRiskBar(peak?.precipitationProbability, peak?.precipitationMm)
            LabelValueRow(
                label = if (measurableHours.isNotEmpty()) "48時間の雨量ピーク" else "48時間の確率ピーク",
                value = peak?.let { "${formatDateHourLabel(it.time)} ${it.precipitationProbability.percentText()} / ${it.precipitationMm.mmText()}" }
                    ?: "降水データなし",
                color = MaterialTheme.colorScheme.secondary,
            )
            val today = snapshot.today()
            val todayHours = today?.let { snapshot.hourly.forDate(it.date) }.orEmpty()
            LabelValueRow("今日の予想降水量", today.effectivePrecipitationSum(todayHours).mmText(), MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun NowcastRainSection(minutes: List<MinutelyWeather>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("直近3時間", "15分ごとの雨")
        if (minutes.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2030)),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    "短時間予報を取得できません",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                minutes.forEach { minute -> MinutelyRainCard(minute) }
            }
        }
    }
}

@Composable
private fun MinutelyRainCard(minute: MinutelyWeather) {
    val probability = (minute.precipitationProbability ?: 0).coerceIn(0, 100)
    val rain = minute.precipitationMm ?: 0.0
    val active = probability >= 30 || rain >= 0.1
    val signal = rainSignal(minute.precipitationProbability, minute.precipitationMm)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (active) Color(0xFF1B3346) else Color(0xFF171D2A),
        ),
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            Modifier
                .width(92.dp)
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(formatMinuteLabel(minute.time), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            WeatherGlyph(code = minute.weatherCode, size = 30.dp)
            Text(signal.action, color = signal.color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(probability.percentText(), color = MaterialTheme.colorScheme.secondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            ProbabilityBar(probability)
            Text(minute.precipitationMm.mmText(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

/**
 * A slim horizontal bar where the FILLED WIDTH equals the chance of rain.
 * Width (left→right) is far more intuitive than encoding probability as a
 * block's height, which several users found confusing.
 */
@Composable
fun ProbabilityBar(probability: Int, modifier: Modifier = Modifier) {
    val fraction = (probability.coerceIn(0, 100)) / 100f
    Box(
        modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF2C3447)),
    ) {
        if (probability > 0) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.secondary),
            )
        }
    }
}

@Composable
private fun HomeHourlySection(hours: List<HourlyWeather>) {
    val grouped = remember(hours) { groupHoursByDate(hours) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("時間ごとの予報", "降水確率・雨量・気温を同じ時刻で確認")
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            grouped.forEach { (date, dayHours) ->
                HourlyDayTimeline(date = date, dayHours = dayHours)
            }
        }
    }
}

@Composable
fun HourlyDayTimeline(date: LocalDate, dayHours: List<HourlyWeather>) {
    SectionCard(containerColor = Color(0xFF202124)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            DayBadge(date)
            HourlyForecastTable(dayHours)
        }
    }
}

@Composable
fun DayBadge(date: LocalDate) {
    val today = LocalDate.now(ZoneId.of("Asia/Tokyo"))
    val offset = ChronoUnit.DAYS.between(today, date)
    val relative = when (offset) {
        0L -> "今日"
        1L -> "明日"
        2L -> "明後日"
        else -> null
    }
    val accent = when (offset) {
        0L -> MaterialTheme.colorScheme.primary
        1L -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.16f))
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (relative != null) {
            Text(relative, color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        WeekendDateLabel(
            date = date.toString(),
            fontSizeSp = 17,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun HourlyRainTimeline(hours: List<HourlyWeather>) {
    val density = LocalDensity.current
    val temps = hours.mapNotNull { it.temperatureC }
    val minTemp = temps.minOrNull() ?: 0.0
    val maxTemp = temps.maxOrNull() ?: 1.0
    val tempColor = Color(0xFFFF7A1A)
    val rainColor = MaterialTheme.colorScheme.secondary
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val gridColor = Color(0xFF3A3A3E)
    val now = remember { LocalDateTime.now(ZoneId.of("Asia/Tokyo")) }
    val maxRain = hours.mapNotNull { it.precipitationMm }.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val timelineHeight = 286.dp
    val timeTextSize = with(density) { 14.sp.toPx() }
    val iconTextSize = with(density) { 25.sp.toPx() }
    val tempTextSize = with(density) { 15.sp.toPx() }
    val rainTextSize = with(density) { 16.sp.toPx() }
    val amountTextSize = with(density) { 14.sp.toPx() }
    val amountSmallTextSize = with(density) { 13.sp.toPx() }

    Canvas(
        Modifier
            .width((hours.size.coerceAtLeast(1) * 88).dp)
            .height(timelineHeight),
    ) {
        if (hours.isEmpty()) return@Canvas
        val topY = 32f
        val iconY = 78f
        val tempGraphTop = 108f
        val tempGraphBottom = 168f
        val probabilityY = 202f
        val amountBarBase = 246f
        val amountTextY = 272f
        val graphHeight = tempGraphBottom - tempGraphTop
        val columnWidth = size.width / hours.size.coerceAtLeast(1)
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textSize = timeTextSize
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = iconTextSize
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tempColor.toArgb()
            textSize = tempTextSize
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = rainColor.toArgb()
            textSize = rainTextSize
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0xFF202124).toArgb()
            textSize = amountTextSize
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val amountSmallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textSize = amountSmallTextSize
            textAlign = Paint.Align.CENTER
        }

        drawLine(gridColor, Offset(0f, 96f), Offset(size.width, 96f), strokeWidth = 1f)
        drawLine(gridColor, Offset(0f, probabilityY + 10f), Offset(size.width, probabilityY + 10f), strokeWidth = 1f)

        hours.forEachIndexed { index, hour ->
            val parsed = runCatching { LocalDateTime.parse(hour.time) }.getOrNull()
            val isNow = parsed?.let { it.toLocalDate() == now.toLocalDate() && it.hour == now.hour } == true
            val x = index * columnWidth + columnWidth / 2f
            if (isNow) {
                drawRoundRect(
                    color = rainColor.copy(alpha = 0.14f),
                    topLeft = Offset(index * columnWidth + 5f, 0f),
                    size = Size(columnWidth - 10f, size.height),
                    cornerRadius = CornerRadius(18f, 18f),
                )
            }
            drawContext.canvas.nativeCanvas.drawText(
                if (isNow) "今" else formatHourOnly(hour.time),
                x,
                topY,
                timePaint,
            )
            drawContext.canvas.nativeCanvas.drawText(weatherIcon(hour.weatherCode), x, iconY, iconPaint)
        }

        val points = hours.mapIndexedNotNull { index, hour ->
            val temp = hour.temperatureC ?: return@mapIndexedNotNull null
            val range = (maxTemp - minTemp).takeIf { it > 0.1 } ?: 1.0
            val x = index * columnWidth + columnWidth / 2f
            val y = tempGraphTop + graphHeight * (1f - ((temp - minTemp) / range).toFloat())
            IndexedPoint(index, Offset(x, y), temp)
        }
        points.zipWithNext().forEach { (a, b) ->
            drawLine(tempColor, a.offset, b.offset, strokeWidth = 5f, cap = StrokeCap.Round)
        }
        points.forEach { point ->
            drawCircle(tempColor, radius = 5f, center = point.offset)
            drawContext.canvas.nativeCanvas.drawText(
                point.temperature.roundText(),
                point.offset.x,
                (point.offset.y + 30f).coerceAtMost(tempGraphBottom + 28f),
                tempPaint,
            )
        }

        hours.forEachIndexed { index, hour ->
            val x = index * columnWidth + columnWidth / 2f
            val probability = hour.precipitationProbability ?: 0
            val amount = hour.precipitationMm ?: 0.0
            drawContext.canvas.nativeCanvas.drawText("${probability}%", x, probabilityY, rainPaint)

            val barHeight = (amount / maxRain).toFloat().coerceIn(0f, 1f) * 34f
            drawRoundRect(
                color = Color.White.copy(alpha = 0.95f),
                topLeft = Offset(x - 22f, amountBarBase - 24f - barHeight),
                size = Size(44f, 24f + barHeight),
                cornerRadius = CornerRadius(4f, 4f),
            )
            drawContext.canvas.nativeCanvas.drawText(
                if (amount >= 10.0) amount.oneDecimal() else amount.roundText(),
                x,
                amountBarBase - 7f,
                amountPaint,
            )
            drawContext.canvas.nativeCanvas.drawText(
                "${hour.precipitationMm.mmText()}",
                x,
                amountTextY,
                amountSmallPaint,
            )
        }
    }
}

private data class IndexedPoint(
    val index: Int,
    val offset: Offset,
    val temperature: Double,
)

@Composable
fun HourCompactCard(hour: HourlyWeather) {
    val probability = (hour.precipitationProbability ?: 0).coerceIn(0, 100)
    val isNow = remember(hour.time) { isCurrentHour(hour.time) }
    val signal = rainSignal(hour.precipitationProbability, hour.precipitationMm)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (probability > 0) Color(0xFF1B3346) else Color(0xFF171D2A),
        ),
        shape = MaterialTheme.shapes.small,
        modifier = if (isNow) {
            Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
        } else {
            Modifier
        },
    ) {
        Column(
            Modifier
                .width(92.dp)
                .padding(vertical = 12.dp, horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (isNow) "今" else formatHourOnly(hour.time),
                color = if (isNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal,
            )
            Text(weatherIcon(hour.weatherCode), fontSize = 20.sp)
            Text(signal.action, color = signal.color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("${hour.temperatureC?.roundText() ?: "--"}°", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(probability.percentText(), color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            ProbabilityBar(probability)
            Text(hour.precipitationMm.mmText(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

/** Groups a flat hourly list into ordered (date, hours) buckets for day-by-day display. */
fun groupHoursByDate(hours: List<HourlyWeather>): List<Pair<LocalDate, List<HourlyWeather>>> {
    return hours
        .mapNotNull { hour -> runCatching { LocalDateTime.parse(hour.time).toLocalDate() }.getOrNull()?.let { it to hour } }
        .groupBy({ it.first }, { it.second })
        .toList()
        .sortedBy { it.first }
}

fun formatHourOnly(time: String): String {
    val hour = runCatching { LocalDateTime.parse(time).hour }.getOrNull() ?: return "--"
    return "${hour}時"
}

private fun isCurrentHour(time: String, timezone: String = "Asia/Tokyo"): Boolean {
    val parsed = runCatching { LocalDateTime.parse(time) }.getOrNull() ?: return false
    val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.of("Asia/Tokyo"))
    val now = LocalDateTime.now(zone)
    return parsed.toLocalDate() == now.toLocalDate() && parsed.hour == now.hour
}

@Composable
private fun HomeWeeklySection(days: List<DailyWeather>, hourly: List<HourlyWeather>, onDayClick: (DailyWeather) -> Unit) {
    val remainingDays = days.drop(2)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("2週間天気", "日付・天気・最高・最低・降水")
        SectionCard(containerColor = WeatherPalette.ForecastSurface) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                remainingDays.forEachIndexed { index, day ->
                    CompactWeeklyRow(day = day, dayHours = hourly.forDate(day.date), onClick = { onDayClick(day) })
                    if (index != remainingDays.lastIndex) {
                        HorizontalDivider(color = WeatherPalette.Outline)
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyRow(day: DailyWeather, dayHours: List<HourlyWeather>, onClick: () -> Unit) {
    CompactWeeklyRow(day = day, dayHours = dayHours, onClick = onClick)
}

@Composable
private fun CompactWeeklyRow(day: DailyWeather, dayHours: List<HourlyWeather>, onClick: () -> Unit) {
    val maxProbability = day.effectiveMaxProbability(dayHours)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WeekendDateLabel(
            date = day.date,
            fontSizeSp = 15,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.width(88.dp),
        )
        WeatherGlyph(
            code = day.weatherCode,
            size = 42.dp,
            modifier = Modifier.width(48.dp),
        )
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${day.maxTemperatureC?.roundText() ?: "--"}°",
                color = WeatherPalette.HighTemperature,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${day.minTemperatureC?.roundText() ?: "--"}°",
                color = WeatherPalette.LowTemperature,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(
                    Icons.Outlined.WaterDrop,
                    contentDescription = null,
                    tint = WeatherPalette.Rain,
                    modifier = Modifier.size(14.dp),
                )
                Text(maxProbability.percentText(), fontSize = 15.sp)
            }
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = "詳細",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun WeeklyTodayTomorrowStrip(days: List<DailyWeather>, hourly: List<HourlyWeather>) {
    if (days.isEmpty()) return
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF252525))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        days.take(2).forEachIndexed { index, day ->
            val dayHours = hourly.forDate(day.date)
            val signal = rainSignal(day.effectiveMaxProbability(dayHours), day.effectivePrecipitationSum(dayHours))
            Column(
                Modifier.weight(1f),
                horizontalAlignment = if (index == 0) Alignment.Start else Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(if (index == 0) "今日 ${formatDateWithWeekday(day.date)}" else "明日 ${formatDateWithWeekday(day.date)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Text("${weatherIcon(day.weatherCode)} ${weatherLabel(day.weatherCode)}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${day.maxTemperatureC?.roundText() ?: "--"}° / ${day.minTemperatureC?.roundText() ?: "--"}°  ${signal.action}", color = signal.color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            if (index == 0 && days.size > 1) {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(54.dp)
                        .background(Color(0xFF3A3A3E)),
                )
            }
        }
    }
}

@Composable
private fun PeriodChip(summary: DayPeriodSummary, modifier: Modifier = Modifier) {
    val signal = rainSignal(summary.maxProbability, summary.precipitationSum)
    Column(
        modifier
            .background(Color(0xFF10141F), MaterialTheme.shapes.small)
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(summary.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(signal.action, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = signal.color)
        Text("${weatherIcon(summary.weatherCode)} ${signal.label}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        Text("${summary.maxTemp?.roundText() ?: "--"}° / ${summary.maxProbability.percentText()} / ${summary.precipitationSum.mmText()}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RainImpactRow(signal: RainSignal, probability: Int?, precipitation: Double?) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(signal.label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = signal.color)
                Text(signal.detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(probability.percentText(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Text(precipitation.mmText(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        RainRiskBar(probability, precipitation)
    }
}

@Composable
private fun RainRiskBar(probability: Int?, precipitation: Double?) {
    val risk = rainRiskScore(probability, precipitation)
    Canvas(Modifier.fillMaxWidth().height(8.dp)) {
        drawRoundRect(
            color = Color(0xFF2C3447),
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(10f, 10f),
        )
        drawRoundRect(
            color = rainColor(probability, precipitation),
            size = Size(size.width * risk, size.height),
            cornerRadius = CornerRadius(10f, 10f),
        )
    }
}

@Composable
fun DayDetailDialog(
    day: DailyWeather,
    dayHours: List<HourlyWeather>,
    timezone: String = "Asia/Tokyo",
    radarPrecipitation: RadarPrecipitation? = null,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${formatDateLong(day.date)}の時間予報") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "横にスクロールして1時間ごとの変化を確認",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                if (dayHours.isEmpty()) {
                    Text("この日の時間予報を取得できません")
                } else {
                    HourlyForecastTable(dayHours, timezone, radarPrecipitation)
                }
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
    val focusManager = LocalFocusManager.current
    val submitSearch = {
        onSearchLocations(query)
        focusManager.clearFocus()
    }
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
                        onValueChange = { query = it },
                        singleLine = true,
                        label = { Text("世界中の都市を検索") },
                        placeholder = { Text("例: Heidelberg, Berlin, London") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Button(
                        onClick = submitSearch,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSearchingLocation,
                    ) {
                        if (state.isSearchingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("検索中")
                        } else {
                            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("検索")
                        }
                    }
                }
                state.locationSearchMessage?.let { message ->
                    item {
                        Text(
                            message,
                            color = if (state.searchResults.isEmpty()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            fontSize = 12.sp,
                        )
                    }
                }
                if (state.searchResults.isNotEmpty()) {
                    item { Text("検索結果", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
                    items(state.searchResults.take(8)) { location ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectLocation(location) }
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(location.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${location.latitude.oneDecimal()}, ${location.longitude.oneDecimal()}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                )
                            }
                            Icon(
                                Icons.Outlined.ChevronRight,
                                contentDescription = "この地点を追加",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                item {
                    HorizontalDivider()
                    TextButton(onClick = onUseDeviceLocation) { Text("現在地を使う") }
                }
                item {
                    Text("保存地点", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                items(state.savedLocations) { location ->
                    LocationRow(
                        location = location,
                        selected = location.sameSavedPlaceAs(state.selectedLocation),
                        onSelect = { onSelectLocation(location) },
                        onMoveUp = { onMoveLocation(location, -1) },
                        onMoveDown = { onMoveLocation(location, 1) },
                        onDelete = { onDeleteLocation(location) },
                    )
                }
                item {
                    HorizontalDivider()
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .size(width = 4.dp, height = 18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun SectionCard(
    containerColor: Color = WeatherPalette.SurfaceElevated,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WeatherPalette.Outline.copy(alpha = 0.6f), MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.medium,
    ) {
        content()
    }
}

@Composable
private fun AirMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LabelValueRow(label: String, value: String, color: Color) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

fun nextRainText(snapshot: WeatherSnapshot): String {
    val expected = snapshot.nextExpectedPrecipitation(maxHours = 48)
    if (expected != null) {
        return expectedPrecipitationText(snapshot, expected)
    }
    val maxProbability = snapshot.maxPrecipitationProbabilityFromNow(maxHours = 48)
    val hasAmountData = snapshot.minutely15.any { it.precipitationMm != null } ||
        snapshot.hourly.nextHours(48, snapshot.timezone).any { it.precipitationMm != null }
    return when {
        !hasAmountData -> "48時間の雨量データなし${maxProbability?.let { "（確率最大$it%）" }.orEmpty()}"
        maxProbability != null -> "48時間の予想雨量は0.0mm（確率最大$maxProbability%）"
        else -> "48時間の雨予報はありません"
    }
}

fun expectedPrecipitationText(snapshot: WeatherSnapshot, expected: ExpectedPrecipitation): String {
    if (expected.isCurrent) {
        return expected.radarPrecipitation?.let { radar ->
            "現在、${radar.intensityLabel()}（レーダー ${radar.intensityLowerBoundMmPerHour.oneDecimal()}mm/h以上）"
        } ?: "現在、雨が降っています（直近${expected.periodMinutes}分 ${expected.amountMm.mmText()}）"
    }
    val date = expected.time.take(10)
    val dailyTotal = snapshot.daily
        .firstOrNull { it.date == date }
        .effectivePrecipitationSum(snapshot.hourly.forDate(date))
    val period = if (expected.periodMinutes == 60) "その1時間" else "その${expected.periodMinutes}分"
    val dailyDetail = dailyTotal?.let { " / ${formatDateShort(date)}一日合計 ${it.mmText()}" }.orEmpty()
    return "${formatDateMinuteLabel(expected.time)}ごろから雨予報（$period ${expected.amountMm.mmText()}$dailyDetail）"
}

fun relativeDayLabel(date: String, timezone: String): String? {
    val target = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.of("Asia/Tokyo"))
    val today = LocalDate.now(zone)
    return when (target) {
        today -> "今日"
        today.plusDays(1) -> "明日"
        else -> null
    }
}

fun currentPrecipitationText(snapshot: WeatherSnapshot): String {
    val radar = snapshot.freshRadarPrecipitation()
    return when {
        radar?.isRaining() == true ->
            "レーダー ${radar.intensityLowerBoundMmPerHour.oneDecimal()}mm/h以上・${formatHourMinute(radar.observedAtMillis)}"
        radar != null -> "レーダー 降雨なし・${formatHourMinute(radar.observedAtMillis)}"
        else -> "予報 ${snapshot.current.precipitationMm?.oneDecimal() ?: "--"}mm"
    }
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

fun formatMinuteLabel(time: String): String {
    val parsed = runCatching { LocalDateTime.parse(time) }.getOrNull() ?: return "--"
    val hour = parsed.hour
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "${if (hour < 12) "AM" else "PM"} $displayHour:${parsed.minute.toString().padStart(2, '0')}"
}

fun formatDateMinuteLabel(time: String): String {
    val parsed = runCatching { LocalDateTime.parse(time) }.getOrNull() ?: return "--"
    return "${parsed.format(DateTimeFormatter.ofPattern("M/d"))} ${formatMinuteLabel(time)}"
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

fun formatDateWithWeekday(date: String): String {
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
    return parsed?.format(DateTimeFormatter.ofPattern("MM/dd(E)", Locale.JAPANESE)) ?: date
}

@Composable
private fun WeekendDateLabel(
    date: String,
    prefix: String? = null,
    fontSizeSp: Int,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
) {
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
    if (parsed == null) {
        Text(
            listOfNotNull(prefix, date).joinToString(" "),
            modifier = modifier,
            fontSize = fontSizeSp.sp,
            fontWeight = fontWeight,
        )
        return
    }
    val weekday = parsed.format(DateTimeFormatter.ofPattern("E", Locale.JAPANESE))
    val weekdayColor = when (parsed.dayOfWeek) {
        java.time.DayOfWeek.SATURDAY -> WeatherPalette.LowTemperature
        java.time.DayOfWeek.SUNDAY -> WeatherPalette.HighTemperature
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (prefix != null) {
            Text(
                "$prefix ",
                fontSize = fontSizeSp.sp,
                fontWeight = fontWeight,
            )
        }
        Text(
            parsed.format(DateTimeFormatter.ofPattern("M/d")),
            fontSize = fontSizeSp.sp,
            fontWeight = fontWeight,
        )
        Text(
            "($weekday)",
            color = weekdayColor,
            fontSize = fontSizeSp.sp,
            fontWeight = fontWeight,
        )
    }
}

fun formatDateLong(date: String): String {
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
    return parsed?.format(DateTimeFormatter.ofPattern("yyyy年M月d日")) ?: date
}

fun List<HourlyWeather>.nextHours(count: Int, timezone: String = "Asia/Tokyo"): List<HourlyWeather> {
    val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.of("Asia/Tokyo"))
    val now = LocalDateTime.now(zone).withMinute(0).withSecond(0).withNano(0)
    return filter { hour ->
        runCatching { !LocalDateTime.parse(hour.time).isBefore(now) }.getOrDefault(false)
    }.take(count)
}

fun List<MinutelyWeather>.nextMinutely15(count: Int, timezone: String = "Asia/Tokyo"): List<MinutelyWeather> {
    val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.of("Asia/Tokyo"))
    val now = LocalDateTime.now(zone)
    return filter { minute ->
        runCatching { LocalDateTime.parse(minute.time).plusMinutes(15).isAfter(now) }.getOrDefault(false)
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

data class DailyAdvice(
    val label: String,
    val value: String,
    val detail: String,
    val color: Color,
)

data class RainSignal(
    val label: String,
    val action: String,
    val detail: String,
    val color: Color,
)

data class ComfortSignal(
    val action: String,
    val detail: String,
)

fun buildDailyAdvice(snapshot: WeatherSnapshot, next48Hours: List<HourlyWeather>): List<DailyAdvice> {
    val today = snapshot.today()
    val next24Hours = next48Hours.take(24)
    val maxProbability = next24Hours.mapNotNull { it.precipitationProbability }.maxOrNull()
    val precipitationSum = next24Hours.mapNotNull { it.precipitationMm }.takeIf { it.isNotEmpty() }?.sum()
    val hasPrecipitationAmountData = next24Hours.any { it.precipitationMm != null }
    val nextRain = snapshot.nextExpectedPrecipitation(maxHours = 24)
    val maxTemp = today?.maxTemperatureC ?: next24Hours.mapNotNull { it.temperatureC }.maxOrNull()
    val minTemp = today?.minTemperatureC ?: next24Hours.mapNotNull { it.temperatureC }.minOrNull()
    val apparent = snapshot.current.apparentTemperatureC ?: snapshot.current.temperatureC
    val humidity = snapshot.current.humidityPercent
    val wind = snapshot.current.windSpeedKmh
    val uv = today?.uvIndexMax
    val aqi = snapshot.airQuality?.europeanAqi

    val umbrella = when {
        nextRain != null -> DailyAdvice(
            label = "傘",
            value = "持つ",
            detail = if (nextRain.isCurrent) {
                nextRain.radarPrecipitation?.let {
                    "現在 ${it.intensityLabel()} ${it.intensityLowerBoundMmPerHour.oneDecimal()}mm/h以上"
                } ?: "現在降水 ${nextRain.amountMm.mmText()}"
            } else {
                "${formatDateMinuteLabel(nextRain.time)}ごろ ${nextRain.probability.percentText()} / ${nextRain.amountMm.mmText()}"
            },
            color = Color(0xFF26313A),
        )
        (precipitationSum ?: 0.0) >= 0.1 -> DailyAdvice(
            label = "傘",
            value = "持つ",
            detail = "24h ${maxProbability.percentText()} / ${precipitationSum.mmText()}",
            color = Color(0xFF222831),
        )
        !hasPrecipitationAmountData -> DailyAdvice(
            label = "傘",
            value = "判断不可",
            detail = "雨量データなし / 確率最大 ${maxProbability.percentText()}",
            color = Color(0xFF272624),
        )
        else -> DailyAdvice(
            label = "傘",
            value = "不要寄り",
            detail = "予想雨量 ${precipitationSum.mmText()} / 確率最大 ${maxProbability.percentText()}",
            color = Color(0xFF1D241E),
        )
    }

    val laundry = when {
        (precipitationSum ?: 0.0) >= 0.1 -> DailyAdvice(
            label = "洗濯",
            value = "部屋干し",
            detail = "降水 ${maxProbability.percentText()} / ${precipitationSum.mmText()}",
            color = Color(0xFF2B2327),
        )
        !hasPrecipitationAmountData -> DailyAdvice(
            label = "洗濯",
            value = "雨量待ち",
            detail = "確率 ${maxProbability.percentText()} / 雨量データなし",
            color = Color(0xFF272624),
        )
        (humidity ?: 0) >= 75 -> DailyAdvice(
            label = "洗濯",
            value = "乾きにくい",
            detail = "湿度 ${humidity.percentText()}。外干しは短時間向き",
            color = Color(0xFF272624),
        )
        (wind ?: 0.0) >= 35.0 -> DailyAdvice(
            label = "洗濯",
            value = "強風注意",
            detail = "風 ${windText(wind, snapshot.current.windDirectionDeg)}",
            color = Color(0xFF2B261D),
        )
        else -> DailyAdvice(
            label = "洗濯",
            value = "外干しOK",
            detail = "降水 ${maxProbability.percentText()} / 湿度 ${humidity.percentText()}",
            color = Color(0xFF1D241E),
        )
    }

    val clothes = when {
        (maxTemp ?: 0.0) >= 30.0 || (apparent ?: 0.0) >= 30.0 -> DailyAdvice(
            label = "服装",
            value = "暑さ対策",
            detail = "最高 ${maxTemp.temperatureText()} / 体感 ${apparent.temperatureText()}",
            color = Color(0xFF302315),
        )
        (minTemp ?: 99.0) <= 10.0 -> DailyAdvice(
            label = "服装",
            value = "防寒",
            detail = "最低 ${minTemp.temperatureText()} / 最高 ${maxTemp.temperatureText()}",
            color = Color(0xFF1D2633),
        )
        (minTemp ?: 99.0) <= 16.0 -> DailyAdvice(
            label = "服装",
            value = "羽織り",
            detail = "最低 ${minTemp.temperatureText()}。朝晩は冷えやすい",
            color = Color(0xFF222831),
        )
        else -> DailyAdvice(
            label = "服装",
            value = "軽め",
            detail = "最高 ${maxTemp.temperatureText()} / 最低 ${minTemp.temperatureText()}",
            color = Color(0xFF1E2422),
        )
    }

    val outdoor = when {
        (uv ?: 0.0) >= 6.0 -> DailyAdvice(
            label = "外出",
            value = "UV強め",
            detail = "UV ${uv.uvText()}。日焼け止め推奨",
            color = Color(0xFF2B2817),
        )
        (aqi ?: 0) >= 61 -> DailyAdvice(
            label = "外出",
            value = "空気注意",
            detail = "AQI ${aqi ?: "--"} ${aqiLabel(aqi)}",
            color = Color(0xFF2B2020),
        )
        (wind ?: 0.0) >= 35.0 -> DailyAdvice(
            label = "外出",
            value = "風強め",
            detail = windText(wind, snapshot.current.windDirectionDeg),
            color = Color(0xFF25252B),
        )
        else -> DailyAdvice(
            label = "外出",
            value = "動きやすい",
            detail = "UV ${uv.uvText()} / AQI ${aqi ?: "--"}",
            color = Color(0xFF1E2422),
        )
    }

    return listOf(umbrella, laundry, clothes, outdoor)
}

fun dayPeriodSummaries(hours: List<HourlyWeather>): Pair<DayPeriodSummary, DayPeriodSummary> {
    return summarizePeriod("AM", hours.filter { runCatching { LocalDateTime.parse(it.time).hour < 12 }.getOrDefault(false) }) to
        summarizePeriod("PM", hours.filter { runCatching { LocalDateTime.parse(it.time).hour >= 12 }.getOrDefault(false) })
}

private fun summarizePeriod(label: String, hours: List<HourlyWeather>): DayPeriodSummary {
    val maxRainHour = hours.filter { it.hasMeasurablePrecipitation() }
        .maxByOrNull { it.precipitationMm ?: 0.0 }
        ?: hours.maxByOrNull { it.precipitationProbability ?: -1 }
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

fun rainSignal(probability: Int?, precipitationMm: Double?): RainSignal {
    val probabilityValue = probability ?: 0
    val rain = precipitationMm ?: 0.0
    return when {
        rain >= 100.0 -> RainSignal(
            label = "災害級の大雨",
            action = "外出は控えめ",
            detail = "道路冠水や交通乱れに注意",
            color = Color(0xFFFF8A80),
        )
        rain >= 50.0 -> RainSignal(
            label = "大雨警戒",
            action = "予定見直し",
            detail = "強い雨が長く続く可能性",
            color = Color(0xFFFFB74D),
        )
        rain >= 10.0 -> RainSignal(
            label = "しっかり雨",
            action = "雨具必須",
            detail = "傘だけでなく靴も注意",
            color = Color(0xFF64D2FF),
        )
        rain >= 1.0 -> RainSignal(
            label = "雨具必要",
            action = "傘を持つ",
            detail = "予想雨量を確認",
            color = Color(0xFF64D2FF),
        )
        rain >= 0.1 -> RainSignal(
            label = "雨予報あり",
            action = "傘を持つ",
            detail = "降り出す時刻を確認",
            color = Color(0xFF64D2FF),
        )
        precipitationMm == null -> RainSignal(
            label = "雨量データなし",
            action = "判断不可",
            detail = "降水確率 ${probability.percentText()}",
            color = Color(0xFFC7C7CC),
        )
        probabilityValue >= 70 -> RainSignal(
            label = "予報不一致",
            action = "雨量予測なし",
            detail = "確率は高いが予想雨量0.0mm",
            color = Color(0xFFBFFF3C),
        )
        probabilityValue >= 40 -> RainSignal(
            label = "確率のみ",
            action = "判断保留",
            detail = "予想雨量0.0mm",
            color = Color(0xFFBFFF3C),
        )
        else -> RainSignal(
            label = "雨の心配低め",
            action = "身軽でOK",
            detail = "急な雨だけ注意",
            color = Color(0xFFC7C7CC),
        )
    }
}

fun comfortSignal(snapshot: WeatherSnapshot, today: DailyWeather?, next48Hours: List<HourlyWeather>): ComfortSignal {
    val maxTemp = today?.maxTemperatureC ?: next48Hours.take(24).mapNotNull { it.temperatureC }.maxOrNull()
    val minTemp = today?.minTemperatureC ?: next48Hours.take(24).mapNotNull { it.temperatureC }.minOrNull()
    val apparent = snapshot.current.apparentTemperatureC ?: snapshot.current.temperatureC
    val humidity = snapshot.current.humidityPercent
    val wind = snapshot.current.windSpeedKmh
    return when {
        (apparent ?: 0.0) >= 33.0 || (maxTemp ?: 0.0) >= 33.0 -> ComfortSignal(
            action = "暑さ対策",
            detail = "体感 ${apparent.temperatureText()}。水分と日差し対策を優先",
        )
        (maxTemp ?: 0.0) >= 30.0 -> ComfortSignal(
            action = "薄着でOK",
            detail = "最高 ${maxTemp.temperatureText()}。日中は暑め",
        )
        (minTemp ?: 99.0) <= 10.0 -> ComfortSignal(
            action = "防寒",
            detail = "最低 ${minTemp.temperatureText()}。朝晩は冷える",
        )
        (minTemp ?: 99.0) <= 16.0 -> ComfortSignal(
            action = "羽織り",
            detail = "最低 ${minTemp.temperatureText()}。朝晩だけ冷えやすい",
        )
        (wind ?: 0.0) >= 35.0 -> ComfortSignal(
            action = "風に注意",
            detail = "風 ${windText(wind, snapshot.current.windDirectionDeg)}。軽い物は飛びやすい",
        )
        (humidity ?: 0) >= 80 -> ComfortSignal(
            action = "蒸れ対策",
            detail = "湿度 ${humidity.percentText()}。汗が乾きにくい",
        )
        else -> ComfortSignal(
            action = "過ごしやすい",
            detail = "気温差は大きくなりにくい",
        )
    }
}

fun temperatureTrendLabel(hours: List<HourlyWeather>): String {
    val first = hours.firstOrNull()?.temperatureC
    val later = hours.drop(1).take(12).lastOrNull()?.temperatureC ?: hours.lastOrNull()?.temperatureC
    if (first == null || later == null) return "見通しなし"
    val diff = later - first
    return when {
        diff >= 5.0 -> "上がる ${first.roundText()}°→${later.roundText()}°"
        diff <= -5.0 -> "下がる ${first.roundText()}°→${later.roundText()}°"
        diff >= 2.0 -> "少し上がる"
        diff <= -2.0 -> "少し下がる"
        else -> "ほぼ横ばい"
    }
}

fun nextRainShortText(snapshot: WeatherSnapshot): String {
    val expected = snapshot.nextExpectedPrecipitation(maxHours = 48)
    return when {
        expected?.isCurrent == true -> "現在降水中"
        expected != null -> formatDateMinuteLabel(expected.time)
        else -> "雨量予測なし"
    }
}

fun rainColor(probability: Int?, precipitationMm: Double?): Color = rainSignal(probability, precipitationMm).color

fun rainRiskScore(probability: Int?, precipitationMm: Double?): Float {
    val probabilityScore = ((probability ?: 0) / 100f).coerceIn(0f, 1f)
    val rainScore = ((precipitationMm ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
    return maxOf(probabilityScore, rainScore).coerceIn(0.08f, 1f)
}

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
