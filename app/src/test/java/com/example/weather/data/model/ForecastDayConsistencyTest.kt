package com.example.weather.data.model

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ForecastDayConsistencyTest {
    @Test
    fun staleDayIsNeverReturnedAsToday() {
        val snapshot = snapshot(
            daily = listOf(
                day("2026-07-28"),
                day("2026-07-29"),
                day("2026-07-30"),
            ),
        )

        assertEquals("2026-07-29", snapshot.today(LocalDate.parse("2026-07-29"))?.date)
        assertEquals(
            listOf("2026-07-29", "2026-07-30"),
            snapshot.forecastDays(LocalDate.parse("2026-07-29")).map { it.date },
        )
    }

    @Test
    fun missingCurrentDateDoesNotRenameTomorrowAsToday() {
        val snapshot = snapshot(daily = listOf(day("2026-07-30")))

        assertNull(snapshot.today(LocalDate.parse("2026-07-29")))
        assertEquals("2026-07-30", snapshot.forecastDays(LocalDate.parse("2026-07-29")).single().date)
    }

    @Test
    fun forecastDaysAreSortedAndDeduplicatedByDate() {
        val snapshot = snapshot(
            daily = listOf(
                day("2026-07-31", max = 31.0),
                day("2026-07-30", max = 30.0),
                day("2026-07-31", max = 99.0),
            ),
        )

        val days = snapshot.forecastDays(LocalDate.parse("2026-07-30"))

        assertEquals(listOf("2026-07-30", "2026-07-31"), days.map { it.date })
        assertEquals(31.0, days.last().maxTemperatureC)
    }

    @Test
    fun zeroHourlyRainRemainsZeroAcrossAllConsumers() {
        val daily = day("2026-07-30").copy(precipitationSumMm = null)
        val hours = listOf(
            HourlyWeather("2026-07-30T10:00", 30.0, 20, 3, 0.0),
            HourlyWeather("2026-07-30T11:00", 31.0, 30, 3, 0.0),
        )

        assertEquals(0.0, daily.effectivePrecipitationSum(hours))
        assertEquals(50, daily.effectiveMaxProbability(hours))
    }

    private fun snapshot(daily: List<DailyWeather>) = WeatherSnapshot(
        location = WeatherLocation("テスト", 35.0, 139.0),
        current = CurrentWeather(null, weatherCode = null, precipitationMm = null, time = null),
        hourly = emptyList(),
        daily = daily,
        updatedAtMillis = 0L,
        timezone = "Asia/Tokyo",
    )

    private fun day(date: String, max: Double = 30.0) = DailyWeather(
        date = date,
        weatherCode = 3,
        maxTemperatureC = max,
        minTemperatureC = 20.0,
        maxPrecipitationProbability = 50,
        precipitationSumMm = 0.3,
    )
}
