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
}
