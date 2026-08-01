package com.example.weather.data.api

import com.example.weather.data.model.AmedasObservationValue
import com.example.weather.data.model.AmedasStation
import com.example.weather.data.model.WeatherLocation
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JmaAmedasClientTest {
    private val client = JmaAmedasClient(OkHttpClient(), Json { ignoreUnknownKeys = true })

    @Test
    fun selectsNearestNormalQualityTemperatureStation() {
        val stations = mapOf(
            "near" to station("近い観測所", 35, 41.0, 139, 46.0),
            "far" to station("遠い観測所", 35, 50.0, 139, 55.0),
        )
        val observations = mapOf(
            "near" to AmedasObservationValue(temp = listOf(31.8, 0.0)),
            "far" to AmedasObservationValue(temp = listOf(29.0, 0.0)),
        )

        val result = client.selectNearestTemperature(
            WeatherLocation("現在地", 35.6812, 139.7671),
            stations,
            observations,
            observedAtMillis = 123L,
        )

        assertEquals("near", result?.stationId)
        assertEquals(31.8, result?.temperatureC)
        assertEquals(123L, result?.observedAtMillis)
    }

    @Test
    fun rejectsQuestionableQualityAndDistantStations() {
        val stations = mapOf(
            "bad" to station("品質不良", 35, 41.0, 139, 46.0),
            "distant" to station("遠すぎる", 36, 30.0, 140, 40.0),
        )
        val observations = mapOf(
            "bad" to AmedasObservationValue(temp = listOf(31.8, 1.0)),
            "distant" to AmedasObservationValue(temp = listOf(30.0, 0.0)),
        )

        val result = client.selectNearestTemperature(
            WeatherLocation("現在地", 35.6812, 139.7671),
            stations,
            observations,
            observedAtMillis = 123L,
        )

        assertNull(result)
    }

    private fun station(name: String, latDegree: Int, latMinute: Double, lonDegree: Int, lonMinute: Double) =
        AmedasStation(
            lat = listOf(latDegree.toDouble(), latMinute),
            lon = listOf(lonDegree.toDouble(), lonMinute),
            name = name,
        )
}
