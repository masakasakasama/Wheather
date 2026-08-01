package com.example.weather

import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class ForegroundRefreshTimingTest {
    private val zone = ZoneId.of("Asia/Tokyo")

    @Test
    fun alignsRefreshToFiveMinutesPastTenMinuteBoundary() {
        val now = epoch("2026-08-01T10:32:30")

        assertEquals(2 * 60 * 1000L + 30 * 1000L, nextAlignedWeatherRefreshDelay(now))
    }

    @Test
    fun skipsBoundaryWhenItWouldImmediatelyDuplicateLaunchRefresh() {
        val now = epoch("2026-08-01T10:34:30")

        assertEquals(10 * 60 * 1000L + 30 * 1000L, nextAlignedWeatherRefreshDelay(now))
    }

    private fun epoch(value: String): Long = LocalDateTime.parse(value).atZone(zone).toInstant().toEpochMilli()
}
