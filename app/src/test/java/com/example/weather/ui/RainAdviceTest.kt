package com.example.weather.ui

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
}
