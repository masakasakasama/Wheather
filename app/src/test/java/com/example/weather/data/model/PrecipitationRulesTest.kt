package com.example.weather.data.model

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        assertEquals(60, result?.periodMinutes)
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
        assertEquals(15, result?.periodMinutes)
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

    @Test
    fun freshRadarRainOverridesDryForecastCurrentValue() {
        val observedAt = 1_000_000L
        val snapshot = snapshot(
            minutely = emptyList(),
            hourly = emptyList(),
            radar = RadarPrecipitation(50.0, observedAt),
        )

        val radar = snapshot.freshRadarPrecipitation(nowMillis = observedAt + 60_000)
        val result = snapshot.nextExpectedPrecipitation(now = now, nowMillis = observedAt + 60_000)

        assertEquals(50.0, radar?.intensityLowerBoundMmPerHour)
        assertTrue(radar?.isRaining() == true)
        assertEquals("非常に激しい雨", radar?.intensityLabel())
        assertEquals(50.0, result?.radarPrecipitation?.intensityLowerBoundMmPerHour)
        assertTrue(result?.isCurrent == true)
    }

    @Test
    fun freshRadarNoRainOverridesModelCurrentRain() {
        val observedAt = 1_000_000L
        val snapshot = snapshot(
            minutely = emptyList(),
            hourly = emptyList(),
            radar = RadarPrecipitation(0.0, observedAt),
            currentAmount = 5.0,
        )

        assertNull(
            snapshot.nextExpectedPrecipitation(
                now = now,
                nowMillis = observedAt + 60_000,
            ),
        )
    }

    @Test
    fun staleRadarFallsBackToModelCurrentRain() {
        val observedAt = 1_000_000L
        val snapshot = snapshot(
            minutely = emptyList(),
            hourly = emptyList(),
            radar = RadarPrecipitation(0.0, observedAt),
            currentAmount = 5.0,
        )

        val result = snapshot.nextExpectedPrecipitation(
            now = now,
            nowMillis = observedAt + 16 * 60_000,
        )

        assertTrue(result?.isCurrent == true)
        assertEquals(5.0, result?.amountMm)
    }

    private fun snapshot(
        minutely: List<MinutelyWeather>,
        hourly: List<HourlyWeather>,
        radar: RadarPrecipitation? = null,
        currentAmount: Double = 0.0,
    ) = WeatherSnapshot(
        location = WeatherLocation("テスト", 35.0, 139.0),
        current = CurrentWeather(null, weatherCode = 0, precipitationMm = currentAmount, time = "2026-07-28T10:15"),
        minutely15 = minutely,
        hourly = hourly,
        daily = emptyList(),
        updatedAtMillis = 0L,
        radarPrecipitation = radar,
    )

    private fun minute(time: String, probability: Int, amount: Double) =
        MinutelyWeather(time, null, probability, 3, amount)

    private fun hour(time: String, probability: Int, amount: Double) =
        HourlyWeather(time, null, probability, 3, amount)
}
