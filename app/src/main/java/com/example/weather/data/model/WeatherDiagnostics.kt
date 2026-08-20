package com.example.weather.data.model

fun WeatherSnapshot.decisionDiagnostics(
    nowMillis: Long = System.currentTimeMillis(),
): String {
    val radar = freshRadarPrecipitation(nowMillis)
    val today = today()
    return buildString {
        append("source=").append(forecastSource)
        append("; tempSource=").append(currentTemperatureSource.provider)
        append("; rawCurrentCode=").append(current.weatherCode)
        append("; rawCurrentPrecipMm=").append(current.precipitationMm)
        append("; radarMmPerHour=").append(radar?.intensityLowerBoundMmPerHour)
        append("; effectiveCurrentCode=").append(effectiveCurrentWeatherCode(nowMillis))
        append("; effectiveCurrentLabel=").append(effectiveCurrentWeatherLabel(nowMillis))
        append("; todayCode=").append(today?.weatherCode)
        append("; todayProbability=").append(today?.maxPrecipitationProbability)
        append("; todayPrecipMm=").append(today?.precipitationSumMm)
    }
}
