package com.example.weather.ui

import com.example.weather.data.model.CurrentWeather
import com.example.weather.data.model.DailyWeather
import com.example.weather.data.model.ExpectedPrecipitation
import com.example.weather.data.model.HourlyWeather
import com.example.weather.data.model.WeatherLocation
import com.example.weather.data.model.WeatherSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RainAdviceTest {
    @Test
    fun highProbabilityWithoutRainAmountDoesNotRecommendUmbrella() {
        val signal = rainSignal(probability = 100, precipitationMm = 0.0)

        assertEquals("予報不一致", signal.label)
        assertEquals("雨量予測なし", signal.action)
        assertFalse(signal.action.contains("折りたたみ"))
    }

    @Test
    fun measurableRainStillRecommendsUmbrella() {
        val signal = rainSignal(probability = 20, precipitationMm = 0.1)

        assertEquals("傘を持つ", signal.action)
    }

    @Test
    fun missingRainAmountIsNotPresentedAsZero() {
        val signal = rainSignal(probability = 100, precipitationMm = null)

        assertEquals("雨量データなし", signal.label)
        assertEquals("判断不可", signal.action)
        assertFalse(signal.detail.contains("0.0mm"))
    }

    @Test
    fun onsetAmountAndDailyTotalAreExplicitlySeparated() {
        val hour = HourlyWeather("2026-07-31T17:00", 30.0, 76, 61, 0.1)
        val snapshot = WeatherSnapshot(
            location = WeatherLocation("テスト", 35.0, 139.0),
            current = CurrentWeather(null, weatherCode = 3, precipitationMm = 0.0, time = null),
            hourly = listOf(hour),
            daily = listOf(
                DailyWeather("2026-07-31", 61, 35.0, 27.0, 76, precipitationSumMm = 0.3),
            ),
            updatedAtMillis = 0L,
        )
        val expected = ExpectedPrecipitation(
            time = hour.time,
            probability = hour.precipitationProbability,
            amountMm = 0.1,
            periodMinutes = 60,
        )

        assertEquals(
            "7/31 PM 5:00ごろから雨予報（その1時間 0.1mm / 7/31一日合計 0.3mm）",
            expectedPrecipitationText(snapshot, expected),
        )
    }

    @Test
    fun temperatureDifferenceMatchesDisplayedRoundedValues() {
        assertEquals("[+2]", temperatureDifferenceText(current = 30.6, previous = 28.6))
        assertEquals("[0]", temperatureDifferenceText(current = 30.4, previous = 30.2))
        assertEquals("[-2]", temperatureDifferenceText(current = 27.4, previous = 29.3))
        assertEquals(null, temperatureDifferenceText(current = 30.0, previous = null))
    }

    @Test
    fun temperatureRangeDoesNotRepeatTheSameDisplayedValue() {
        assertEquals(null, temperatureRangeText(low = 27.5, high = 28.4))
        assertEquals("28〜29°", temperatureRangeText(low = 28.4, high = 28.6))
        assertEquals(null, temperatureRangeText(low = 29.0, high = 28.0))
    }

    @Test
    fun radarRateKeepsSubMillimeterIntensityVisible() {
        assertEquals("0.1", radarRateText(0.1))
        assertEquals("80", radarRateText(80.0))
        assertEquals("--", radarRateText(null))
    }
}
