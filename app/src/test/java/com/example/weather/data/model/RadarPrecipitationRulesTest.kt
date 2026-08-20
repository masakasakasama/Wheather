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
    fun oneWeakRadarPixelDoesNotTurnLocationRainy() {
        val samples = neighborhood(center = 0.1)

        assertEquals(0.0, representativeRadarIntensity(samples))
    }

    @Test
    fun strongCenterEchoRequiresAdjacentAgreementAndUsesRepresentativeIntensity() {
        val samples = neighborhood(center = 5.0).map {
            if (it.dx == 1 && it.dy == 0) it.copy(intensityLowerBoundMmPerHour = 5.0) else it
        }

        assertEquals(5.0, representativeRadarIntensity(samples))
    }

    @Test
    fun tooManyUnknownRadarPixelsAreObservationFailureNotDryWeather() {
        val samples = listOf(
            RadarPixelSample(0, 0, 0.0),
            RadarPixelSample(1, 0, 0.0),
            RadarPixelSample(-1, 0, null),
            RadarPixelSample(0, 1, null),
            RadarPixelSample(0, -1, null),
        )

        assertNull(representativeRadarIntensity(samples))
    }

    @Test
    fun freshHeavyRadarOverridesSunnyModelCurrentWeather() {
        val observedAt = 1_000_000L
        val snapshot = snapshot(RadarPrecipitation(80.0, observedAt))

        assertEquals(65, snapshot.effectiveCurrentWeatherCode(observedAt + 60_000))
        assertEquals("猛烈な雨", snapshot.effectiveCurrentWeatherLabel(observedAt + 60_000))
        assertEquals(
            "現在降雨: 気象庁レーダー 猛烈な雨 80mm/h以上",
            snapshot.radarObservationStatus(observedAt + 60_000),
        )
    }

    @Test
    fun freshDryRadarSuppressesRainyModelCurrentCondition() {
        val observedAt = 1_000_000L
        val snapshot = snapshot(
            radar = RadarPrecipitation(0.0, observedAt),
            currentCode = 61,
            currentPrecipitationMm = 0.2,
        )

        assertEquals(3, snapshot.effectiveCurrentWeatherCode(observedAt + 60_000))
        assertEquals("くもり", snapshot.effectiveCurrentWeatherLabel(observedAt + 60_000))
        assertEquals(
            "現在降雨: 気象庁レーダー 降雨なし",
            snapshot.radarObservationStatus(observedAt + 60_000),
        )
    }

    @Test
    fun failedRadarIsNotReportedAsNoRain() {
        val snapshot = snapshot(null)

        assertEquals(0, snapshot.effectiveCurrentWeatherCode())
        assertEquals("快晴", snapshot.effectiveCurrentWeatherLabel())
        assertEquals("現在降雨: レーダー取得失敗・モデル予報を表示", snapshot.radarObservationStatus())
    }

    @Test
    fun overseasLocationIsNotReportedAsAJmaRadarFailure() {
        val snapshot = snapshot(null).copy(
            location = WeatherLocation("シドニー", -33.86785, 151.20732, countryCode = "AU"),
        )

        assertEquals("現在降雨: レーダー対象外・モデル予報を表示", snapshot.radarObservationStatus())
    }

    @Test
    fun staleRadarDoesNotOverrideCurrentModel() {
        val observedAt = 1_000_000L
        val snapshot = snapshot(RadarPrecipitation(80.0, observedAt))

        assertEquals(0, snapshot.effectiveCurrentWeatherCode(observedAt + 16 * 60_000))
        assertEquals(
            "現在降雨: レーダー観測が古い・モデル予報を表示",
            snapshot.radarObservationStatus(observedAt + 16 * 60_000),
        )
    }

    private fun neighborhood(center: Double): List<RadarPixelSample> = buildList {
        for (dy in -1..1) {
            for (dx in -1..1) {
                add(
                    RadarPixelSample(
                        dx = dx,
                        dy = dy,
                        intensityLowerBoundMmPerHour = if (dx == 0 && dy == 0) center else 0.0,
                    ),
                )
            }
        }
    }

    private fun snapshot(
        radar: RadarPrecipitation?,
        currentCode: Int = 0,
        currentPrecipitationMm: Double = 0.0,
    ) = WeatherSnapshot(
        location = WeatherLocation("現在地", 35.71, 139.78),
        current = CurrentWeather(
            30.0,
            weatherCode = currentCode,
            precipitationMm = currentPrecipitationMm,
            time = "2026-07-30T18:30",
        ),
        hourly = emptyList(),
        daily = emptyList(),
        updatedAtMillis = 0L,
        radarPrecipitation = radar,
    )
}
