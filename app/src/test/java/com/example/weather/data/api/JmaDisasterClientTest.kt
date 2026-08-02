package com.example.weather.data.api

import com.example.weather.data.model.TyphoonSummary
import com.example.weather.data.model.WeatherLocation
import com.example.weather.data.model.displayLabel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class JmaDisasterClientTest {
    private val client = JmaDisasterClient(noNetworkHttpClient(), Json { ignoreUnknownKeys = true })

    @Test
    fun onlyNumericJmaTyphoonIdsBecomePublicTyphoonNumbers() {
        assertEquals("13", normalizeTyphoonNumber("2613"))
        assertEquals("1", normalizeTyphoonNumber("2601"))
        assertNull(normalizeTyphoonNumber("b"))
        assertNull(normalizeTyphoonNumber(""))
    }

    @Test
    fun internalIntensityCodesAreNotShownToUsers() {
        assertEquals("熱帯低気圧", typhoonCategoryLabel("TD"))
        assertEquals("台風", typhoonCategoryLabel("TS"))
        assertEquals("台風", typhoonCategoryLabel("STS"))
        assertEquals("台風", typhoonCategoryLabel("TY"))
        assertNull(typhoonCategoryLabel("LOW"))
        assertNull(typhoonCategoryLabel("UNKNOWN"))
    }

    @Test
    fun unnumberedTropicalDepressionHasNoFakeTyphoonNumber() {
        val depression = TyphoonSummary(
            number = normalizeTyphoonNumber("b"),
            category = typhoonCategoryLabel("TD")!!,
            issueTime = "2026-08-01T22:30:00+09:00",
        )
        val typhoon = TyphoonSummary(
            number = normalizeTyphoonNumber("2613"),
            category = typhoonCategoryLabel("TY")!!,
            issueTime = "2026-08-01T21:50:00+09:00",
        )

        assertEquals("熱帯低気圧", depression.displayLabel())
        assertEquals("台風第13号", typhoon.displayLabel())
    }

    @Test
    fun overseasLocationReturnsNoJmaAlertsWithoutChoosingANearestJapaneseOffice() = runBlocking {
        val summary = client.fetchSummary(
            WeatherLocation("シドニー", -33.86785, 151.20732, countryCode = "AU"),
        ).getOrThrow()

        assertFalse(summary.hasImportantInfo)
        assertNull(summary.officeName)
        assertEquals(emptyList(), summary.activeWarnings)
        assertEquals(emptyList(), summary.typhoons)
    }

    private fun noNetworkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { throw AssertionError("Overseas location must not call JMA disaster APIs") }
        .build()
}
