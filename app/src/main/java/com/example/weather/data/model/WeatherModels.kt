package com.example.weather.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class WeatherSnapshot(
    val location: WeatherLocation,
    val current: CurrentWeather,
    val hourly: List<HourlyWeather>,
    val daily: List<DailyWeather>,
    val updatedAtMillis: Long,
    val usedFallbackModel: Boolean = false,
)

@Serializable
data class CurrentWeather(
    val temperatureC: Double?,
    val weatherCode: Int?,
    val precipitationMm: Double?,
    val time: String?,
)

@Serializable
data class HourlyWeather(
    val time: String,
    val temperatureC: Double?,
    val precipitationProbability: Int?,
    val weatherCode: Int?,
    val precipitationMm: Double?,
)

@Serializable
data class DailyWeather(
    val date: String,
    val weatherCode: Int?,
    val maxTemperatureC: Double?,
    val minTemperatureC: Double?,
    val maxPrecipitationProbability: Int?,
    val precipitationSumMm: Double? = null,
)

@Serializable
data class OpenMeteoResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val current: OpenMeteoCurrent? = null,
    val hourly: OpenMeteoHourly? = null,
    val daily: OpenMeteoDaily? = null,
)

@Serializable
data class OpenMeteoCurrent(
    val time: String? = null,
    @SerialName("temperature_2m") val temperature: Double? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
    val precipitation: Double? = null,
)

@Serializable
data class OpenMeteoHourly(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    val precipitation: List<Double?> = emptyList(),
)

@Serializable
data class OpenMeteoDaily(
    val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("temperature_2m_max") val maxTemperature: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val minTemperature: List<Double?> = emptyList(),
    @SerialName("precipitation_probability_max") val maxPrecipitationProbability: List<Int?> = emptyList(),
    @SerialName("precipitation_sum") val precipitationSum: List<Double?> = emptyList(),
)

@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResult> = emptyList(),
)

@Serializable
data class GeocodingResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    @SerialName("admin1") val admin1: String? = null,
    @SerialName("admin2") val admin2: String? = null,
)

@Serializable
data class RadarTargetTime(
    val basetime: String,
    val validtime: String,
    val elements: List<String> = emptyList(),
)

data class RadarFrame(
    val baseTime: String,
    val validTime: String,
    val tileTemplate: String,
)

val PresetLocations = listOf(
    WeatherLocation("東京駅", 35.681236, 139.767125),
    WeatherLocation("東京", 35.6764, 139.6500),
    WeatherLocation("横浜", 35.4437, 139.6380),
    WeatherLocation("大阪", 34.6937, 135.5023),
    WeatherLocation("名古屋", 35.1815, 136.9066),
    WeatherLocation("福岡", 33.5902, 130.4017),
    WeatherLocation("札幌", 43.0618, 141.3545),
)

fun WeatherSnapshot.today(): DailyWeather? = daily.firstOrNull()

fun weatherIcon(code: Int?): String = when (code) {
    0 -> "晴"
    1, 2 -> "晴曇"
    3 -> "曇"
    45, 48 -> "霧"
    51, 53, 55, 56, 57 -> "霧雨"
    61, 63, 65, 66, 67, 80, 81, 82 -> "雨"
    71, 73, 75, 77, 85, 86 -> "雪"
    95, 96, 99 -> "雷"
    else -> "--"
}

fun weatherLabel(code: Int?): String = when (code) {
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

fun GeocodingResult.toWeatherLocation(): WeatherLocation {
    val area = listOfNotNull(admin1, country).distinct().joinToString(" / ")
    val label = if (area.isBlank()) name else "$name ($area)"
    return WeatherLocation(label, latitude, longitude)
}
