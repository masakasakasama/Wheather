package com.example.weather.data.model

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PrecipitationRulesTest {
    private val now = LocalDateTime.parse("2026-07-28T10:15")

    @Test
    fun probabilityWithoutAmountIsNotRainOnset() {
        val snapshot = snapshot(
            minutely = listOf(minute("2026-07-28T13:45", 93, 0.0)),
            hourly = listOf(
                hour("2026-07-28T15:00", 78, 0.0),
                hour("2026-07-28T17:00", 93, 0.4),
            ),
        )

        val result = snapshot.nextExpectedPrecipitation(now = now)

        assertEquals("2026-07-28T17:00", result?.time)
        assertEquals(0.4, result?.amountMm)
    }

    @Test
    fun minutelyForecastOverridesOverlappingHourlyOnset() {
        val snapshot = snapshot(
            minutely = listOf(
                minute("2026-07-28T10:15", 70, 0.0),
                minute("2026-07-28T10:30", 80, 0.2),
            ),
            hourly = listOf(hour("2026-07-28T10:00", 90, 0.8)),
        )

        val result = snapshot.nextExpectedPrecipitation(now = now)

        assertEquals("2026-07-28T10:30", result?.time)
    }

    @Test
    fun noMeasurableAmountReturnsNoOnset() {
        val snapshot = snapshot(
            minutely = listOf(minute("2026-07-28T10:30", 100, 0.0)),
            hourly = listOf(hour("2026-07-28T15:00", 100, 0.0)),
        )

        assertNull(snapshot.nextExpectedPrecipitation(now = now))
        assertEquals(100, snapshot.maxPrecipitationProbabilityFromNow(now = now))
    }

    private fun snapshot(
        minutely: List<MinutelyWeather>,
        hourly: List<HourlyWeather>,
    ) = WeatherSnapshot(
        location = WeatherLocation("テスト", 35.0, 139.0),
        current = CurrentWeather(null, weatherCode = 0, precipitationMm = 0.0, time = "2026-07-28T10:15"),
        minutely15 = minutely,
        hourly = hourly,
        daily = emptyList(),
        updatedAtMillis = 0L,
    )

    private fun minute(time: String, probability: Int, amount: Double) =
        MinutelyWeather(time, null, probability, 3, amount)

    private fun hour(time: String, probability: Int, amount: Double) =
        HourlyWeather(time, null, probability, 3, amount)
}
