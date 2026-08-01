package com.example.weather.data.model

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class RadarTimeTest {
    @Test
    fun convertsJmaUtcTargetTimeToJapanDisplayTime() {
        assertEquals("08/01 10:15", "20260801011500".toRadarDisplayTime())
    }

    @Test
    fun convertsJmaTargetTimeToUtcEpochForFreshnessChecks() {
        assertEquals(
            Instant.parse("2026-08-01T01:15:00Z").toEpochMilli(),
            "20260801011500".toRadarEpochMillis(),
        )
    }
}
