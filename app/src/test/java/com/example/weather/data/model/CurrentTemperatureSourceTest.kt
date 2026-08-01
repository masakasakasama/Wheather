package com.example.weather.data.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CurrentTemperatureSourceTest {
    private val now = 1_000_000_000L

    @Test
    fun observationIsFreshOnlyForTwentyMinutes() {
        assertTrue(
            CurrentTemperatureSource(
                kind = CurrentTemperatureKind.OBSERVATION,
                dataTimeMillis = now - 20 * 60 * 1000L,
            ).hasFreshObservation(now),
        )
        assertFalse(
            CurrentTemperatureSource(
                kind = CurrentTemperatureKind.OBSERVATION,
                dataTimeMillis = now - 20 * 60 * 1000L - 1,
            ).hasFreshObservation(now),
        )
    }

    @Test
    fun modelEstimateNeverBecomesObservation() {
        assertFalse(
            CurrentTemperatureSource(
                kind = CurrentTemperatureKind.MODEL_ESTIMATE,
                dataTimeMillis = now,
            ).hasFreshObservation(now),
        )
    }
}
