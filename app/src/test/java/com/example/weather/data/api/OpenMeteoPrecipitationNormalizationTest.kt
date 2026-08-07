package com.example.weather.data.api

import kotlin.test.Test
import kotlin.test.assertEquals

class OpenMeteoPrecipitationNormalizationTest {
    @Test
    fun subThresholdRainIsDisplayedAsNoMeasurableRain() {
        assertEquals(0.0, normalizedPrecipitationMm(0.04))
        assertEquals(3, weatherCodeConsistentWithPrecipitation(51, 0.04))
        assertEquals(3, weatherCodeConsistentWithPrecipitation(61, 0.0))
        assertEquals(3, weatherCodeConsistentWithPrecipitation(80, 0.09))
    }

    @Test
    fun measurableRainKeepsRainCodeAndAmount() {
        assertEquals(0.1, normalizedPrecipitationMm(0.1))
        assertEquals(61, weatherCodeConsistentWithPrecipitation(61, 0.1))
    }

    @Test
    fun missingAmountDoesNotHideRainCode() {
        assertEquals(null, normalizedPrecipitationMm(null))
        assertEquals(61, weatherCodeConsistentWithPrecipitation(61, null))
    }

    @Test
    fun thunderstormCodeIsNotSuppressedByRainAmountNormalization() {
        assertEquals(95, weatherCodeConsistentWithPrecipitation(95, 0.0))
    }
}
