package com.example.weather.data.model

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ForecastPresentationPolicyTest {
    @Test
    fun eveningTodayCardIgnoresRainThatOnlyHappenedEarlier() {
        val now = LocalDateTime.parse("2026-08-07T18:05")
        val snapshot = snapshot(
            hourly = listOf(
                hour("2026-08-07T10:00", 61, 76, 1.2),
                hour("2026-08-07T18:00", 2, 29, 0.0),
                hour("2026-08-07T19:00", 2, 17, 0.0),
                hour("2026-08-07T20:00", 2, 7, 0.0),
                hour("2026-08-07T21:00", 0, 0, 0.0),
                hour("2026-08-07T22:00", 2, 0, 0.0),
            ),
            daily = listOf(
                DailyWeather("2026-08-07", 51, 33.0, 27.0, 76, precipitationSumMm = 1.2),
            ),
        )

        val projected = snapshot.applyConsumerForecastProjection(now)
        val today = projected.daily.single()

        assertEquals(2, today.weatherCode)
        assertEquals(29, today.maxPrecipitationProbability)
        assertEquals(0.0, today.precipitationSumMm)
        assertTrue(projected.hourly.all { it.time >= "2026-08-07T18:00" })
    }

    @Test
    fun oneBriefDrizzleDoesNotTurnWholeDayIntoRainCard() {
        val now = LocalDateTime.parse("2026-08-07T08:00")
        val day = DailyWeather("2026-08-08", 53, 34.0, 26.0, 40, precipitationSumMm = 0.04)
        val snapshot = snapshot(
            hourly = listOf(
                hour("2026-08-08T09:00", 2, 10, 0.0),
                hour("2026-08-08T10:00", 2, 20, 0.0),
                hour("2026-08-08T11:00", 51, 40, 0.04),
                hour("2026-08-08T12:00", 2, 20, 0.0),
                hour("2026-08-08T13:00", 2, 10, 0.0),
            ),
            daily = listOf(day),
        )

        val presentation = snapshot.presentationForDay(day, now)

        assertEquals(2, presentation.weatherCode)
        assertTrue(presentation.hasBriefPrecipitation)
        assertTrue(presentation.weatherLabel.contains("一時霧雨"))
    }

    @Test
    fun twoBriefRainHoursInMostlyDryDayStaySecondary() {
        val now = LocalDateTime.parse("2026-08-07T08:00")
        val day = DailyWeather("2026-08-08", 63, 33.0, 26.0, 80, precipitationSumMm = 0.6)
        val hours = (9..20).map { hourOfDay ->
            val rainy = hourOfDay == 13 || hourOfDay == 14
            hour(
                time = "2026-08-08T${hourOfDay.toString().padStart(2, '0')}:00",
                code = if (rainy) 61 else 2,
                probability = if (rainy) 80 else 20,
                amount = if (rainy) 0.3 else 0.0,
            )
        }
        val snapshot = snapshot(hourly = hours, daily = listOf(day))

        val presentation = snapshot.presentationForDay(day, now)

        assertEquals(2, presentation.weatherCode)
        assertTrue(presentation.hasBriefPrecipitation)
        assertTrue(presentation.weatherLabel.contains("一時雨"))
    }

    @Test
    fun sustainedRainRemainsProminent() {
        val now = LocalDateTime.parse("2026-08-07T08:00")
        val day = DailyWeather("2026-08-08", 63, 30.0, 24.0, 70, precipitationSumMm = 2.0)
        val snapshot = snapshot(
            hourly = listOf(
                hour("2026-08-08T09:00", 2, 20, 0.0),
                hour("2026-08-08T10:00", 61, 70, 0.8),
                hour("2026-08-08T11:00", 63, 70, 1.2),
                hour("2026-08-08T12:00", 3, 30, 0.0),
            ),
            daily = listOf(day),
        )

        val presentation = snapshot.presentationForDay(day, now)

        assertEquals(63, presentation.weatherCode)
        assertFalse(presentation.hasBriefPrecipitation)
    }

    @Test
    fun thunderstormIsNeverHiddenByDominantDryHours() {
        val now = LocalDateTime.parse("2026-08-07T08:00")
        val day = DailyWeather("2026-08-08", 95, 31.0, 25.0, 30, precipitationSumMm = 0.0)
        val snapshot = snapshot(
            hourly = listOf(
                hour("2026-08-08T09:00", 0, 0, 0.0),
                hour("2026-08-08T10:00", 0, 0, 0.0),
                hour("2026-08-08T11:00", 95, 30, 0.0),
                hour("2026-08-08T12:00", 2, 10, 0.0),
            ),
            daily = listOf(day),
        )

        assertEquals(95, snapshot.presentationForDay(day, now).weatherCode)
    }

    @Test
    fun futureDailyProbabilityUsesRepresentativeHoursInsteadOfSingleSpike() {
        val now = LocalDateTime.parse("2026-08-07T08:00")
        val day = DailyWeather("2026-08-08", 2, 33.0, 26.0, 95, precipitationSumMm = 0.2)
        val hours = (9..20).map { hourOfDay ->
  val spike = hourOfDay == 15
  hour(
      time = "2026-08-08T${hourOfDay.toString().padStart(2, '0')}:00",
      code = if (spike) 51 else 2,
      probability = if (spike) 95 else 20,
      amount = if (spike) 0.2 else 0.0,
  )
        }
        val presentation = snapshot(hourly = hours, daily = listOf(day)).presentationForDay(day, now)

        assertEquals(20, presentation.precipitationProbability)
        assertEquals(2, presentation.weatherCode)
        assertTrue(presentation.hasBriefPrecipitation)
    }

    @Test
    fun twoShortRainHoursDoNotBecomeAllDayRainJustBecauseTotalExceedsTwoMillimeters() {
        val now = LocalDateTime.parse("2026-08-07T08:00")
        val day = DailyWeather("2026-08-08", 63, 33.0, 26.0, 90, precipitationSumMm = 2.4)
        val hours = (9..20).map { hourOfDay ->
  val rainy = hourOfDay == 13 || hourOfDay == 14
  hour(
      time = "2026-08-08T${hourOfDay.toString().padStart(2, '0')}:00",
      code = if (rainy) 61 else 2,
      probability = if (rainy) 90 else 20,
      amount = if (rainy) 1.2 else 0.0,
  )
        }
        val presentation = snapshot(hourly = hours, daily = listOf(day)).presentationForDay(day, now)

        assertEquals(2, presentation.weatherCode)
        assertTrue(presentation.hasBriefPrecipitation)
    }

    private fun snapshot(
        hourly: List<HourlyWeather>,
        daily: List<DailyWeather>,
    ) = WeatherSnapshot(
        location = WeatherLocation("東京", 35.6764, 139.6500, "JP"),
        current = CurrentWeather(
            temperatureC = 30.0,
            weatherCode = 2,
            precipitationMm = 0.0,
            time = "2026-08-07T18:00",
        ),
        hourly = hourly,
        daily = daily,
        updatedAtMillis = 0L,
        timezone = "Asia/Tokyo",
    )

    private fun hour(time: String, code: Int, probability: Int, amount: Double) =
        HourlyWeather(
            time = time,
            temperatureC = 30.0,
            precipitationProbability = probability,
            weatherCode = code,
            precipitationMm = amount,
        )
}
