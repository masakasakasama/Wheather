package com.example.weather.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weather.WeatherUiState
import com.example.weather.data.model.DailyWeather
import com.example.weather.data.model.ExpectedPrecipitation
import com.example.weather.data.model.HourlyWeather
import com.example.weather.data.model.NotificationSettings
import com.example.weather.data.model.RadarPrecipitation
import com.example.weather.data.model.WeatherLocation
import com.example.weather.data.model.WeatherSnapshot
import com.example.weather.data.model.effectiveMaxProbability
import com.example.weather.data.model.effectivePrecipitationSum
import com.example.weather.data.model.intensityLabel
import com.example.weather.data.model.isRaining
import com.example.weather.data.model.maxPrecipitationProbabilityFromNow
import com.example.weather.data.model.nextExpectedPrecipitation
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

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
    RedesignedHomeScreen(
        state = state,
        appVersionName = appVersionName,
        onRefresh = onRefresh,
        onUseDeviceLocation = onUseDeviceLocation,
        onSelectLocation = onSelectLocation,
        onSearchLocations = onSearchLocations,
        onMoveLocation = onMoveLocation,
        onDeleteLocation = onDeleteLocation,
        onUpdateNotificationSettings = onUpdateNotificationSettings,
        onCheckUpdate = onCheckUpdate,
        onDismissUpdateCheckMessage = onDismissUpdateCheckMessage,
        onDismissError = onDismissError,
    )
}

@Composable
fun HourlyForecastTable(
    hours: List<HourlyWeather>,
    timezone: String = "Asia/Tokyo",
    radarPrecipitation: RadarPrecipitation? = null,
) {
    if (hours.isEmpty()) {
        Text("時間別予報を取得できません", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(hours, key = { it.time }) { hour ->
            val current = isCurrentHour(hour.time, timezone)
            val observedRain = current && radarPrecipitation?.isRaining() == true
            Column(
                Modifier.width(72.dp).padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    if (current) "今" else formatHourOnly(hour.time),
                    color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                )
                Text(if (observedRain) "🌧️" else weatherIconCompat(hour.weatherCode), fontSize = 26.sp)
                Text(hour.temperatureC.temperatureText(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (observedRain) "観測" else hour.precipitationProbability.percentText(),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                )
                Text(
                    if (observedRain) "${radarRateText(radarPrecipitation?.intensityLowerBoundMmPerHour)}+mm/h" else hour.precipitationMm.legacyMmText(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                )
                hour.windSpeedKmh?.let {
                    Text("${(it / 3.6).oneDecimal()}m/s", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
fun WeeklyRow(day: DailyWeather, dayHours: List<HourlyWeather>, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = WeatherPalette.ForecastSurface),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(formatDateWithWeekday(day.date), modifier = Modifier.width(86.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(weatherIconCompat(day.weatherCode), fontSize = 26.sp)
            Column(Modifier.weight(1f)) {
                Text(weatherLabelCompat(day.weatherCode), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "降水 ${day.effectiveMaxProbability(dayHours).percentText()} / ${day.effectivePrecipitationSum(dayHours).legacyMmText()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                )
            }
            Text(day.maxTemperatureC.temperatureText(), color = WeatherPalette.HighTemperature, fontWeight = FontWeight.Bold)
            Text(day.minTemperatureC.temperatureText(), color = WeatherPalette.LowTemperature, fontWeight = FontWeight.Bold)
        }
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "${weatherIconCompat(day.weatherCode)} ${weatherLabelCompat(day.weatherCode)}  ${day.maxTemperatureC.temperatureText()} / ${day.minTemperatureC.temperatureText()}",
                    fontWeight = FontWeight.SemiBold,
                )
                if (dayHours.isEmpty()) Text("この日の時間予報を取得できません")
                else HourlyForecastTable(dayHours, timezone, radarPrecipitation)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("閉じる") } },
    )
}

fun List<HourlyWeather>.nextHours(count: Int, timezone: String = "Asia/Tokyo"): List<HourlyWeather> {
    val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.of("Asia/Tokyo"))
    val now = LocalDateTime.now(zone).withMinute(0).withSecond(0).withNano(0)
    return filter { hour -> runCatching { !LocalDateTime.parse(hour.time).isBefore(now) }.getOrDefault(false) }.take(count)
}

fun List<HourlyWeather>.forDate(date: String): List<HourlyWeather> = filter { hour ->
    runCatching { LocalDateTime.parse(hour.time).toLocalDate().toString() == date }.getOrDefault(false)
}

data class RainSignal(val label: String, val action: String, val detail: String, val color: Color)

fun rainSignal(probability: Int?, precipitationMm: Double?): RainSignal {
    val probabilityValue = probability ?: 0
    val rain = precipitationMm ?: 0.0
    return when {
        rain >= 100.0 -> RainSignal("災害級の大雨", "外出は控えめ", "道路冠水や交通乱れに注意", Color(0xFFD94B4B))
        rain >= 50.0 -> RainSignal("大雨警戒", "予定見直し", "強い雨が長く続く可能性", Color(0xFFE88A22))
        rain >= 10.0 -> RainSignal("しっかり雨", "雨具必須", "傘だけでなく靴も注意", Color(0xFF2F7AF8))
        rain >= 1.0 -> RainSignal("雨具必要", "傘を持つ", "予想雨量を確認", Color(0xFF2F7AF8))
        rain >= 0.1 -> RainSignal("雨予報あり", "傘を持つ", "降り出す時刻を確認", Color(0xFF2F7AF8))
        precipitationMm == null -> RainSignal("雨量データなし", "判断不可", "降水確率 ${probability.percentText()}", Color(0xFF6C7C8E))
        probabilityValue >= 70 -> RainSignal("予報不一致", "雨量予測なし", "確率は高いが予想雨量0.0mm", Color(0xFF6B55DF))
        probabilityValue >= 40 -> RainSignal("確率のみ", "判断保留", "予想雨量0.0mm", Color(0xFF6B55DF))
        else -> RainSignal("雨の心配低め", "身軽でOK", "急な雨だけ注意", Color(0xFF6C7C8E))
    }
}

fun expectedPrecipitationText(snapshot: WeatherSnapshot, expected: ExpectedPrecipitation): String {
    if (expected.isCurrent) {
        return expected.radarPrecipitation?.let { radar ->
            "現在、${radar.intensityLabel()}（レーダー ${radar.intensityLowerBoundMmPerHour.oneDecimal()}mm/h以上）"
        } ?: "現在、雨が降っています（直近${expected.periodMinutes}分 ${expected.amountMm.legacyMmText()}）"
    }
    val date = expected.time.take(10)
    val dailyTotal = snapshot.daily.firstOrNull { it.date == date }.effectivePrecipitationSum(snapshot.hourly.forDate(date))
    val period = if (expected.periodMinutes == 60) "その1時間" else "その${expected.periodMinutes}分"
    val dailyDetail = dailyTotal?.let { " / ${formatDateShort(date)}一日合計 ${it.legacyMmText()}" }.orEmpty()
    return "${formatDateMinuteLabel(expected.time)}ごろから雨予報（$period ${expected.amountMm.legacyMmText()}$dailyDetail）"
}

fun nextRainText(snapshot: WeatherSnapshot): String {
    snapshot.nextExpectedPrecipitation(maxHours = 48)?.let { return expectedPrecipitationText(snapshot, it) }
    val maxProbability = snapshot.maxPrecipitationProbabilityFromNow(maxHours = 48)
    val hasAmountData = snapshot.minutely15.any { it.precipitationMm != null } || snapshot.hourly.nextHours(48, snapshot.timezone).any { it.precipitationMm != null }
    return when {
        !hasAmountData -> "48時間の雨量データなし${maxProbability?.let { "（確率最大$it%）" }.orEmpty()}"
        maxProbability != null -> "48時間の予想雨量は0.0mm（確率最大$maxProbability%）"
        else -> "48時間の雨予報はありません"
    }
}

fun formatHourMinute(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
    .atZone(ZoneId.of("Asia/Tokyo"))
    .format(DateTimeFormatter.ofPattern("HH:mm"))

fun temperatureDifferenceText(current: Double?, previous: Double?): String? {
    if (current == null || previous == null) return null
    val difference = current.roundToInt() - previous.roundToInt()
    return "[${if (difference > 0) "+" else ""}$difference]"
}

fun temperatureRangeText(low: Double?, high: Double?): String? {
    if (low == null || high == null || !low.isFinite() || !high.isFinite() || low >= high) return null
    val lowText = low.roundText()
    val highText = high.roundText()
    return if (lowText == highText) null else "$lowText〜$highText°"
}

fun radarRateText(value: Double?): String = when {
    value == null -> "--"
    value < 1.0 -> value.oneDecimal()
    else -> value.roundText()
}

fun formatHourOnly(time: String): String = runCatching { "${LocalDateTime.parse(time).hour}時" }.getOrDefault("--")

fun formatDateWithWeekday(date: String): String = runCatching {
    java.time.LocalDate.parse(date).format(DateTimeFormatter.ofPattern("MM/dd(E)", Locale.JAPANESE))
}.getOrDefault(date)

fun formatDateLong(date: String): String = runCatching {
    java.time.LocalDate.parse(date).format(DateTimeFormatter.ofPattern("yyyy年M月d日"))
}.getOrDefault(date)

fun formatDateShort(date: String): String = runCatching {
    java.time.LocalDate.parse(date).format(DateTimeFormatter.ofPattern("M/d"))
}.getOrDefault(date)

fun formatDateMinuteLabel(time: String): String {
    val parsed = runCatching { LocalDateTime.parse(time) }.getOrNull() ?: return "--"
    val displayHour = when {
        parsed.hour == 0 -> 12
        parsed.hour > 12 -> parsed.hour - 12
        else -> parsed.hour
    }
    return "${parsed.format(DateTimeFormatter.ofPattern("M/d"))} ${if (parsed.hour < 12) "AM" else "PM"} $displayHour:${parsed.minute.toString().padStart(2, '0')}"
}

private fun isCurrentHour(time: String, timezone: String): Boolean {
    val parsed = runCatching { LocalDateTime.parse(time) }.getOrNull() ?: return false
    val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.of("Asia/Tokyo"))
    val now = LocalDateTime.now(zone)
    return parsed.toLocalDate() == now.toLocalDate() && parsed.hour == now.hour
}

private fun weatherIconCompat(code: Int?): String = when (code) {
    0 -> "☀️"
    1 -> "🌤️"
    2 -> "⛅"
    3 -> "☁️"
    45, 48 -> "🌫️"
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> "🌧️"
    71, 73, 75, 77, 85, 86 -> "🌨️"
    95, 96, 99 -> "⛈️"
    else -> "🌡️"
}

private fun weatherLabelCompat(code: Int?): String = when (code) {
    0 -> "快晴"
    1, 2 -> "晴れ時々くもり"
    3 -> "くもり"
    45, 48 -> "霧"
    51, 53, 55, 56, 57 -> "霧雨"
    61, 63, 65, 66, 67 -> "雨"
    71, 73, 75, 77 -> "雪"
    80, 81, 82 -> "にわか雨"
    85, 86 -> "にわか雪"
    95, 96, 99 -> "雷雨"
    else -> "不明"
}

fun Double.roundText(): String = "%.0f".format(this)
fun Double.oneDecimal(): String = "%.1f".format(this)
fun Int?.percentText(): String = this?.let { "$it%" } ?: "--%"
private fun Double?.legacyMmText(): String = this?.let { "${it.oneDecimal()}mm" } ?: "--mm"
fun Double?.temperatureText(): String = this?.let { "${it.roundText()}°" } ?: "--°"
