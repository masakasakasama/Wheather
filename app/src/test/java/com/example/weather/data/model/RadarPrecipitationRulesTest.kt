package com.example.weather.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RadarPrecipitationRulesTest {
    @Test
    fun mapsAllJmaRadarPaletteColorsToIntensityLowerBounds() {
        assertEquals(0.0, radarIntensityLowerBound(0, 255, 255, 255))
        assertEquals(0.1, radarIntensityLowerBound(255, 242, 242, 255))
        assertEquals(1.0, radarIntensityLowerBound(255, 160, 210, 255))
        assertEquals(5.0, radarIntensityLowerBound(255, 33, 140, 255))
        assertEquals(10.0, radarIntensityLowerBound(255, 0, 65, 255))
        assertEquals(20.0, radarIntensityLowerBound(255, 250, 245, 0))
        assertEquals(30.0, radarIntensityLowerBound(255, 255, 153, 0))
        assertEquals(50.0, radarIntensityLowerBound(255, 255, 40, 0))
        assertEquals(80.0, radarIntensityLowerBound(255, 180, 0, 104))
    }

    @Test
    fun unknownOpaqueColorIsNotTreatedAsNoRain() {
        assertNull(radarIntensityLowerBound(255, 1, 2, 3))
    }

    @Test
    fun freshHeavyRadarOverridesSunnyModelCurrentWeather() {
        val observedAt = 1_000_000L
        val snapshot = snapshot(RadarPrecipitation(80.0, observedAt))

        assertEquals(65, snapshot.effectiveCurrentWeatherCode(observedAt + 60_000))
        assertEquals("猛烈な雨", snapshot.effectiveCurrentWeatherLabel(observedAt + 60_000))
        assertEquals(
            "レーダー観測 猛烈な雨 80mm/h以上",
            snapshot.radarObservationStatus(observedAt + 60_000),
        )
    }

    @Test
    fun failedRadarIsNotReportedAsNoRain() {
        val snapshot = snapshot(null)

        assertEquals(0, snapshot.effectiveCurrentWeatherCode())
        assertEquals("快晴", snapshot.effectiveCurrentWeatherLabel())
        assertEquals("レーダー取得失敗・予報値を表示", snapshot.radarObservationStatus())
    }

    @Test
    fun overseasLocationIsNotReportedAsAJmaRadarFailure() {
        val snapshot = snapshot(null).copy(
            location = WeatherLocation("シドニー", -33.86785, 151.20732, countryCode = "AU"),
        )

        assertEquals("レーダー対象外・予報値を表示", snapshot.radarObservationStatus())
    }

    @Test
    fun staleRadarDoesNotOverrideCurrentModel() {
        val observedAt = 1_000_000L
        val snapshot = snapshot(RadarPrecipitation(80.0, observedAt))

        assertEquals(0, snapshot.effectiveCurrentWeatherCode(observedAt + 16 * 60_000))
        assertEquals(
            "レーダー観測が古いため予報値を表示",
            snapshot.radarObservationStatus(observedAt + 16 * 60_000),
        )
    }

    private fun snapshot(radar: RadarPrecipitation?) = WeatherSnapshot(
        location = WeatherLocation("現在地", 35.71, 139.78),
        current = CurrentWeather(30.0, weatherCode = 0, precipitationMm = 0.0, time = "2026-07-30T18:30"),
        hourly = emptyList(),
        daily = emptyList(),
        updatedAtMillis = 0L,
        radarPrecipitation = radar,
    )
}
