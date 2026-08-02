package com.example.weather.data.model

private const val RadarFreshnessMillis = 15 * 60 * 1000L

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

fun RadarPrecipitation.isRaining(): Boolean = intensityLowerBoundMmPerHour >= 0.1

fun RadarPrecipitation.intensityLabel(): String = when {
    intensityLowerBoundMmPerHour >= 80.0 -> "猛烈な雨"
    intensityLowerBoundMmPerHour >= 50.0 -> "非常に激しい雨"
    intensityLowerBoundMmPerHour >= 30.0 -> "激しい雨"
    intensityLowerBoundMmPerHour >= 20.0 -> "強い雨"
    intensityLowerBoundMmPerHour >= 10.0 -> "やや強い雨"
    intensityLowerBoundMmPerHour >= 5.0 -> "雨"
    intensityLowerBoundMmPerHour >= 1.0 -> "弱い雨"
    intensityLowerBoundMmPerHour >= 0.1 -> "ごく弱い雨"
    else -> "降雨なし"
}

fun WeatherSnapshot.freshRadarPrecipitation(
    nowMillis: Long = System.currentTimeMillis(),
): RadarPrecipitation? {
    return radarPrecipitation?.takeIf {
        nowMillis - it.observedAtMillis in 0..RadarFreshnessMillis
    }
}

fun WeatherSnapshot.effectiveCurrentWeatherCode(
    nowMillis: Long = System.currentTimeMillis(),
): Int? = if (freshRadarPrecipitation(nowMillis)?.isRaining() == true) {
    65
} else {
    current.weatherCode
}

fun WeatherSnapshot.effectiveCurrentWeatherLabel(
    nowMillis: Long = System.currentTimeMillis(),
): String = freshRadarPrecipitation(nowMillis)
    ?.takeIf { it.isRaining() }
    ?.intensityLabel()
    ?: weatherLabel(current.weatherCode)

fun WeatherSnapshot.radarObservationStatus(
    nowMillis: Long = System.currentTimeMillis(),
): String {
    val radar = freshRadarPrecipitation(nowMillis)
    return when {
        radar?.isRaining() == true ->
            "レーダー観測 ${radar.intensityLabel()} ${radar.intensityLowerBoundMmPerHour.toCompactRadarNumber()}mm/h以上"
        radar != null -> "レーダー観測 降雨なし"
        radarPrecipitation != null -> "レーダー観測が古いため予報値を表示"
        location.isInJapan() ->
            "レーダー取得失敗・予報値を表示"
        else -> "レーダー対象外・予報値を表示"
    }
}

private fun Double.toCompactRadarNumber(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
