package com.example.weather.data.model

import java.time.LocalDateTime
import java.time.ZoneId

private val TOKYO_ZONE = ZoneId.of("Asia/Tokyo")

data class ExpectedPrecipitation(
    val time: String,
    val probability: Int?,
    val amountMm: Double,
    val periodMinutes: Int,
    val isCurrent: Boolean = false,
    val radarPrecipitation: RadarPrecipitation? = null,
)

fun HourlyWeather.hasMeasurablePrecipitation(
    thresholdMm: Double = MEASURABLE_PRECIPITATION_MM,
): Boolean = PrecipitationPolicy.isMeasurable(precipitationMm, thresholdMm)

fun MinutelyWeather.hasMeasurablePrecipitation(
    thresholdMm: Double = MEASURABLE_PRECIPITATION_MM,
): Boolean = PrecipitationPolicy.isMeasurable(precipitationMm, thresholdMm)

fun WeatherSnapshot.nextExpectedPrecipitation(
    maxHours: Int = 48,
    now: LocalDateTime = LocalDateTime.now(forecastZoneId()),
    nowMillis: Long = System.currentTimeMillis(),
): ExpectedPrecipitation? {
    val radar = freshRadarPrecipitation(nowMillis)
    if (radar?.isRaining() == true) {
        return ExpectedPrecipitation(
            time = current.time ?: now.toString(),
            probability = null,
            amountMm = radar.intensityLowerBoundMmPerHour,
            periodMinutes = 60,
            isCurrent = true,
            radarPrecipitation = radar,
        )
    }
    if (radar == null && PrecipitationPolicy.isMeasurable(current.precipitationMm)) {
        return ExpectedPrecipitation(
            time = current.time ?: now.toString(),
            probability = null,
            amountMm = PrecipitationPolicy.normalizeAmount(current.precipitationMm) ?: 0.0,
            periodMinutes = 15,
            isCurrent = true,
        )
    }

    val limit = now.plusHours(maxHours.toLong())
    val activeMinutely = minutely15.mapNotNull { minute ->
        parseForecastTime(minute.time)?.let { it to minute }
    }.filter { (time) ->
        time.plusMinutes(15).isAfter(now) && !time.isAfter(limit)
    }
    activeMinutely.firstOrNull { (_, minute) ->
        minute.hasMeasurablePrecipitation()
    }?.let { (_, minute) ->
        return ExpectedPrecipitation(
            time = minute.time,
            probability = minute.precipitationProbability,
            amountMm = PrecipitationPolicy.normalizeAmount(minute.precipitationMm) ?: 0.0,
            periodMinutes = 15,
        )
    }

    // Within the 15-minute forecast range, prefer the finer data and do not let
    // an overlapping hourly value claim an earlier onset.
    val minutelyCoverageEnd = activeMinutely.maxOfOrNull { (time) -> time.plusMinutes(15) }
    return hourly.asSequence()
        .mapNotNull { hour -> parseForecastTime(hour.time)?.let { it to hour } }
        .filter { (time) ->
            time.plusHours(1).isAfter(now) &&
                !time.isAfter(limit) &&
                (minutelyCoverageEnd == null || !time.isBefore(minutelyCoverageEnd))
        }
        .firstOrNull { (_, hour) -> hour.hasMeasurablePrecipitation() }
        ?.let { (_, hour) ->
            ExpectedPrecipitation(
                time = hour.time,
                probability = hour.precipitationProbability,
                amountMm = PrecipitationPolicy.normalizeAmount(hour.precipitationMm) ?: 0.0,
                periodMinutes = 60,
            )
        }
}

fun WeatherSnapshot.maxPrecipitationProbabilityFromNow(
    maxHours: Int = 48,
    now: LocalDateTime = LocalDateTime.now(forecastZoneId()),
): Int? {
    val limit = now.plusHours(maxHours.toLong())
    val minutelyProbabilities = minutely15.mapNotNull { minute ->
        val time = parseForecastTime(minute.time) ?: return@mapNotNull null
        minute.precipitationProbability?.takeIf {
            time.plusMinutes(15).isAfter(now) && !time.isAfter(limit)
        }
    }
    val hourlyProbabilities = hourly.mapNotNull { hour ->
        val time = parseForecastTime(hour.time) ?: return@mapNotNull null
        hour.precipitationProbability?.takeIf {
            time.plusHours(1).isAfter(now) && !time.isAfter(limit)
        }
    }
    return (minutelyProbabilities + hourlyProbabilities).maxOrNull()
}

private fun parseForecastTime(value: String): LocalDateTime? =
    runCatching { LocalDateTime.parse(value) }.getOrNull()

fun WeatherSnapshot.forecastZoneId(): ZoneId =
    runCatching { ZoneId.of(timezone) }.getOrDefault(TOKYO_ZONE)
