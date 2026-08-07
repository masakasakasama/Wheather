package com.example.weather.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrecipitationPolicyTest {
    @Test
    fun providerConditionIsNotRewrittenFromAccumulation() {
        listOf(51, 61, 71, 80, 95).forEach { code ->
            assertEquals(code, PrecipitationPolicy.normalizeWeatherCode(code, 0.0), "code=$code")
            assertEquals(code, PrecipitationPolicy.normalizeWeatherCode(code, 0.04), "code=$code")
        }
    }

    @Test
    fun tracePrecipitationIsPreservedInsteadOfRoundedAway() {
        assertEquals(0.0, PrecipitationPolicy.normalizeAmount(0.0))
        assertEquals(0.04, PrecipitationPolicy.normalizeAmount(0.04))
        assertEquals(0.0999, PrecipitationPolicy.normalizeAmount(0.0999))
        assertEquals(0.1, PrecipitationPolicy.normalizeAmount(0.1))
        assertNull(PrecipitationPolicy.normalizeAmount(-1.0))
        assertNull(PrecipitationPolicy.normalizeAmount(Double.NaN))
    }

    @Test
    fun probabilityAmountAndConditionRemainIndependentSignals() {
        val probabilityOnly = PrecipitationPolicy.assess(
            probabilityPercent = 80,
            amountMm = 0.0,
            weatherCode = 61,
        )
        assertEquals(PrecipitationState.PROBABILITY_ONLY, probabilityOnly.state)
        assertEquals(61, probabilityOnly.weatherCode)
        assertFalse(probabilityOnly.isMeasurable)

        val trace = PrecipitationPolicy.assess(
            probabilityPercent = 30,
            amountMm = 0.04,
            weatherCode = 51,
        )
        assertEquals(PrecipitationState.TRACE, trace.state)
        assertEquals(51, trace.weatherCode)
        assertEquals(0.04, trace.amountMm)

        val measurable = PrecipitationPolicy.assess(
            probabilityPercent = 20,
            amountMm = 0.1,
            weatherCode = 61,
        )
        assertEquals(PrecipitationState.MEASURABLE, measurable.state)
        assertTrue(measurable.isMeasurable)
    }

    @Test
    fun snapshotGateRepairsOnlyInvalidAmountsAndPreservesConditions() {
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
                HourlyWeather("2026-08-07T14:00", 31.0, 20, 3, -1.0),
            ),
            daily = listOf(
                DailyWeather("2026-08-07", 82, 33.0, 26.0, 90, precipitationSumMm = 0.0),
            ),
            updatedAtMillis = 0L,
        )

        assertTrue(raw.precipitationInvariantViolations().isNotEmpty())

        val normalized = raw.enforcePrecipitationConsistency()

        assertEquals(61, normalized.current.weatherCode)
        assertEquals(0.04, normalized.current.precipitationMm)
        assertEquals(51, normalized.minutely15.single().weatherCode)
        assertEquals(0.0, normalized.minutely15.single().precipitationMm)
        assertEquals(85, normalized.hourly[0].weatherCode)
        assertEquals(0.09, normalized.hourly[0].precipitationMm)
        assertNull(normalized.hourly[1].precipitationMm)
        assertEquals(82, normalized.daily.single().weatherCode)
        assertTrue(normalized.precipitationInvariantViolations().isEmpty())
    }
}
