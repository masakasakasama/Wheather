package com.example.weather.data.api

import com.example.weather.data.model.TyphoonSummary
import com.example.weather.data.model.displayLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JmaDisasterClientTest {
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
}
