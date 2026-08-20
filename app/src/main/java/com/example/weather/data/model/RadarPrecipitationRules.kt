package com.example.weather.data.model

import kotlin.math.abs

private const val RadarFreshnessMillis = 15 * 60 * 1000L
private const val MinimumRecognizedRadarPixels = 5

data class RadarPixelSample(
    val dx: Int,
    val dy: Int,
    val intensityLowerBoundMmPerHour: Double?,
)

fun radarIntensityLowerBound(
    alpha: Int,
    red: Int,
    green: Int,
    blue: Int,
): Double? {
    if (alpha == 0) return 0.0
    return when (Triple(red, green, blue)) {
        Triple(242, 242, 255) -> 0.1
        Triple(160, 210, 255) -> 1.0
        Triple(33, 140, 255) -> 5.0
        Triple(0, 65, 255) -> 10.0
        Triple(250, 245, 0) -> 20.0
        Triple(255, 153, 0) -> 30.0
        Triple(255, 40, 0) -> 50.0
        Triple(180, 0, 104) -> 80.0
        else -> null
    }
}

/**
 * Converts the 3x3 radar neighbourhood around the requested coordinate into a
 * representative intensity. A single coloured pixel is deliberately not enough to
 * call the user's exact location rainy: weak echoes need spatial agreement, and even
 * stronger centre echoes need at least one adjacent wet pixel. This avoids the old
 * max-of-7x7 behaviour where one noisy/edge pixel could turn the whole location rainy.
 *
 * Returns null when too few pixels use a recognised JMA palette colour. Callers must
 * treat that as an observation failure, not as "no rain".
 */
fun representativeRadarIntensity(samples: List<RadarPixelSample>): Double? {
    val neighbourhood = samples.filter { abs(it.dx) <= 1 && abs(it.dy) <= 1 }
    val recognized = neighbourhood.filter { it.intensityLowerBoundMmPerHour != null }
    if (recognized.size < MINIMUM_RECOGNIZED_RADAR_PIXELS) return null

    val wet = recognized.filter {
        PrecipitationPolicy.isMeasurable(it.intensityLowerBoundMmPerHour)
    }
    if (wet.isEmpty()) return 0.0

    val centerIntensity = recognized
        .firstOrNull { it.dx == 0 && it.dy == 0 }
        ?.intensityLowerBoundMmPerHour
        ?: 0.0
    val centerWet = PrecipitationPolicy.isMeasurable(centerIntensity)
    val adjacentWetCount = wet.count { it.dx != 0 || it.dy != 0 }

    val confirmed = when {
        centerWet && centerIntensity >= 1.0 && adjacentWetCount >= 1 -> true
        centerWet && wet.size >= 3 -> true
        !centerWet && wet.size >= 4 -> true
        else -> false
    }
    if (!confirmed) return 0.0

    val sortedWet = wet.mapNotNull { it.intensityLowerBoundMmPerHour }.sorted()
    return sortedWet[(sortedWet.size - 1) / 2]
}

fun RadarPrecipitation.isRaining(): Boolean =
    PrecipitationPolicy.isMeasurable(intensityLowerBoundMmPerHour)

fun RadarPrecipitation.intensityLabel(): String = when {
    intensityLowerBoundMmPerHour >= 80.0 -> "猛烈な雨"
    intensityLowerBoundMmPerHour >= 50.0 -> "非常に激しい雨"
    intensityLowerBoundMmPerHour >= 30.0 -> "激しい雨"
    intensityLowerBoundMmPerHour >= 20.0 -> "強い雨"
    intensityLowerBoundMmPerHour >= 10.0 -> "やや強い雨"
    intensityLowerBoundMmPerHour >= 5.0 -> "雨"
    intensityLowerBoundMmPerHour >= 1.0 -> "弱い雨"
    intensityLowerBoundMmPerHour >= MEASURABLE_PRECIPITATION_MM -> "ごく弱い雨"
    else -> "降雨なし"
}

fun WeatherSnapshot.freshRadarPrecipitation(
    nowMillis: Long = System.currentTimeMillis(),
): RadarPrecipitation? {
    return radarPrecipitation?.takeIf {
        nowMillis - it.observedAtMillis in 0..RadarFreshnessMillis
    }
}

private fun Int?.isWetConditionCode(): Boolean = when (this) {
    51, 53, 55, 56, 57,
    61, 63, 65, 66, 67,
    71, 73, 75, 77, 80, 81, 82, 85, 86,
    95, 96, 99,
    -> true
    else -> false
}

private fun WeatherSnapshot.dryCurrentFallbackCode(): Int? {
    val currentHourPrefix = current.time?.take(13)
    val sameHourCode = currentHourPrefix
        ?.let { prefix -> hourly.firstOrNull { it.time.take(13) == prefix } }
        ?.weatherCode
    return sameHourCode?.takeUnless { it.isWetConditionCode() } ?: 3
}

fun WeatherSnapshot.effectiveCurrentWeatherCode(
    nowMillis: Long = System.currentTimeMillis(),
): Int? {
    val radar = freshRadarPrecipitation(nowMillis)
    return when {
        radar?.isRaining() == true -> 65
        radar != null && current.weatherCode.isWetConditionCode() -> dryCurrentFallbackCode()
        else -> current.weatherCode
    }
}

fun WeatherSnapshot.effectiveCurrentWeatherLabel(
    nowMillis: Long = System.currentTimeMillis(),
): String {
    val radar = freshRadarPrecipitation(nowMillis)
    return radar
        ?.takeIf { it.isRaining() }
        ?.intensityLabel()
        ?: weatherLabel(effectiveCurrentWeatherCode(nowMillis))
}

fun WeatherSnapshot.radarObservationStatus(
    nowMillis: Long = System.currentTimeMillis(),
): String {
    val radar = freshRadarPrecipitation(nowMillis)
    return when {
        radar?.isRaining() == true ->
            "現在降雨: 気象庁レーダー ${radar.intensityLabel()} ${radar.intensityLowerBoundMmPerHour.toCompactRadarNumber()}mm/h以上"
        radar != null -> "現在降雨: 気象庁レーダー 降雨なし"
        radarPrecipitation != null -> "現在降雨: レーダー観測が古い・モデル予報を表示"
        location.isInJapan() -> "現在降雨: レーダー取得失敗・モデル予報を表示"
        else -> "現在降雨: レーダー対象外・モデル予報を表示"
    }
}

private fun Double.toCompactRadarNumber(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
