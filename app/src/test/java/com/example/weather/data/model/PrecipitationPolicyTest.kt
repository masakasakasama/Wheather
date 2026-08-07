package com.example.weather.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrecipitationPolicyTest {
    private val amountBoundCodes = listOf(
        51, 53, 55, 56, 57,
        61, 63, 65, 66, 67,
        71, 73, 75, 77,
        80, 81, 82,
        85, 86,
    )

    @Test
    fun everyAmountBoundWeatherCodeIsDemotedWhenAmountIsZero() {
        amountBoundCodes.forEach { code ->
            assertEquals(3, PrecipitationPolicy.normalizeWeatherCode(code, 0.0), "code=$code")
            assertEquals(3, PrecipitationPolicy.normalizeWeatherCode(code, 0.09), "code=$code")
        }
    }

    @Test
    fun measurablePrecipitationKeepsProviderWeatherCode() {
        amountBoundCodes.forEach { code ->
            assertEquals(code, PrecipitationPolicy.normalizeWeatherCode(code, 0.1), "code=$code")
            assertEquals(code, PrecipitationPolicy.normalizeWeatherCode(code, 5.0), "code=$code")
        }
    }

    @Test
    fun unknownAmountDoesNotInventDryWeather() {
        assertEquals(61, PrecipitationPolicy.normalizeWeatherCode(61, null))
        assertEquals(61, PrecipitationPolicy.normalizeWeatherCode(61, Double.NaN))
        assertNull(PrecipitationPolicy.normalizeAmount(Double.NaN))
        assertNull(PrecipitationPolicy.normalizeAmount(Double.POSITIVE_INFINITY))
    }

    @Test
    fun thunderstormSignalIsPreservedEvenWithZeroReportedRain() {
        listOf(95, 96, 99).forEach { code ->
            assertEquals(code, PrecipitationPolicy.normalizeWeatherCode(code, 0.0))
        }
    }

    @Test
    fun amountNormalizationHasOneGlobalBoundary() {
        assertEquals(0.0, PrecipitationPolicy.normalizeAmount(-1.0))
        assertEquals(0.0, PrecipitationPolicy.normalizeAmount(0.0))
        assertEquals(0.0, PrecipitationPolicy.normalizeAmount(0.04))
        assertEquals(0.0, PrecipitationPolicy.normalizeAmount(0.0999))
        assertEquals(0.1, PrecipitationPolicy.normalizeAmount(0.1))
        assertEquals(1.2, PrecipitationPolicy.normalizeAmount(1.2))
    }

    @Test
    fun probabilityOnlyCanNeverBecomeMeasurableRain() {
        val assessment = PrecipitationPolicy.assess(
            probabilityPercent = 100,
            amountMm = 0.0,
            weatherCode = 61,
        )

        assertEquals(PrecipitationState.PROBABILITY_ONLY, assessment.state)
        assertFalse(assessment.isMeasurable)
        assertEquals(3, assessment.weatherCode)
        assertEquals(0.0, assessment.amountMm)
    }

    @Test
    fun snapshotGateRepairsCurrentMinutelyHourlyAndDailyTogether() {
        val raw = WeatherSnapshot(
            location = WeatherLocation("テスト", 35.0, 139.0),
            current = CurrentWeather(
                temperatureC = 30.0,
                weatherCode = 61,
                precipitationMm = 0.04,
                time = "2026-08-07T12:00",
            ),
            minutely15 = listOf(
                MinutelyWeather("2026-08-07T12:15", 30.0, 80, 51, 0.0),
            ),
            hourly = listOf(
                HourlyWeather("2026-08-07T13:00", 31.0, 90, 85, 0.09),
            ),
            daily = listOf(
                DailyWeather("2026-08-07", 82, 33.0, 26.0, 90, precipitationSumMm = 0.0),
            ),
            updatedAtMillis = 0L,
        )

        assertTrue(raw.precipitationInvariantViolations().isNotEmpty())

        val normalized = raw.enforcePrecipitationConsistency()

        assertEquals(3, normalized.current.weatherCode)
        assertEquals(0.0, normalized.current.precipitationMm)
        assertEquals(3, normalized.minutely15.single().weatherCode)
        assertEquals(0.0, normalized.minutely15.single().precipitationMm)
        assertEquals(3, normalized.hourly.single().weatherCode)
        assertEquals(0.0, normalized.hourly.single().precipitationMm)
        assertEquals(3, normalized.daily.single().weatherCode)
        assertEquals(0.0, normalized.daily.single().precipitationSumMm)
        assertTrue(normalized.precipitationInvariantViolations().isEmpty())
    }

    @Test
    fun invariantGateDoesNotChangeValidRain() {
        val raw = WeatherSnapshot(
            location = WeatherLocation("テスト", 35.0, 139.0),
            current = CurrentWeather(
                temperatureC = 30.0,
                weatherCode = 61,
                precipitationMm = 0.1,
                time = "2026-08-07T12:00",
            ),
            hourly = listOf(
                HourlyWeather("2026-08-07T13:00", 31.0, 50, 63, 2.5),
            ),
            daily = listOf(
                DailyWeather("2026-08-07", 63, 33.0, 26.0, 50, precipitationSumMm = 4.0),
            ),
            updatedAtMillis = 0L,
        )

        val normalized = raw.enforcePrecipitationConsistency()

        assertEquals(raw.current.weatherCode, normalized.current.weatherCode)
        assertEquals(raw.current.precipitationMm, normalized.current.precipitationMm)
        assertEquals(raw.hourly.single().weatherCode, normalized.hourly.single().weatherCode)
        assertEquals(raw.hourly.single().precipitationMm, normalized.hourly.single().precipitationMm)
        assertTrue(normalized.precipitationInvariantViolations().isEmpty())
    }
}
