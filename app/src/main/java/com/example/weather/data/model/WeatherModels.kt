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
    val airQuality: AirQuality? = null,
)

@Serializable
data class CurrentWeather(
    val temperatureC: Double?,
    val apparentTemperatureC: Double? = null,
    val humidityPercent: Int? = null,
    val weatherCode: Int?,
    val precipitationMm: Double?,
    val windSpeedKmh: Double? = null,
    val windDirectionDeg: Int? = null,
    val pressureHpa: Double? = null,
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
    val uvIndexMax: Double? = null,
    val sunrise: String? = null,
    val sunset: String? = null,
)

@Serializable
data class AirQuality(
    val time: String? = null,
    val europeanAqi: Int? = null,
    val usAqi: Int? = null,
    val pm10: Double? = null,
    val pm25: Double? = null,
    val nitrogenDioxide: Double? = null,
    val ozone: Double? = null,
    val hourly: List<HourlyAirQuality> = emptyList(),
)

@Serializable
data class HourlyAirQuality(
    val time: String,
    val europeanAqi: Int? = null,
    val pm25: Double? = null,
    val pm10: Double? = null,
    val uvIndex: Double? = null,
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
    @SerialName("apparent_temperature") val apparentTemperature: Double? = null,
    @SerialName("relative_humidity_2m") val humidity: Int? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
    val precipitation: Double? = null,
    @SerialName("wind_speed_10m") val windSpeed: Double? = null,
    @SerialName("wind_direction_10m") val windDirection: Int? = null,
    @SerialName("pressure_msl") val pressure: Double? = null,
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
    @SerialName("uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
    val sunrise: List<String?> = emptyList(),
    val sunset: List<String?> = emptyList(),
)

@Serializable
data class OpenMeteoAirQualityResponse(
    val current: OpenMeteoAirQualityCurrent? = null,
    val hourly: OpenMeteoAirQualityHourly? = null,
)

@Serializable
data class OpenMeteoAirQualityCurrent(
    val time: String? = null,
    @SerialName("european_aqi") val europeanAqi: Int? = null,
    @SerialName("us_aqi") val usAqi: Int? = null,
    val pm10: Double? = null,
    @SerialName("pm2_5") val pm25: Double? = null,
    @SerialName("nitrogen_dioxide") val nitrogenDioxide: Double? = null,
    val ozone: Double? = null,
)

@Serializable
data class OpenMeteoAirQualityHourly(
    val time: List<String> = emptyList(),
    @SerialName("european_aqi") val europeanAqi: List<Int?> = emptyList(),
    @SerialName("pm2_5") val pm25: List<Double?> = emptyList(),
    val pm10: List<Double?> = emptyList(),
    @SerialName("uv_index") val uvIndex: List<Double?> = emptyList(),
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
