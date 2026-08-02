package com.example.weather.data.api

import com.example.weather.data.model.WeatherLocation
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import kotlin.test.Test
import kotlin.test.assertNull

class JmaRadarClientTest {
    private val client = JmaRadarClient(noNetworkHttpClient(), Json { ignoreUnknownKeys = true })

    @Test
    fun overseasLocationDoesNotRequestJmaRadarTiles() = runBlocking {
        val result = client.latestPrecipitation(
            WeatherLocation("釜山", 35.1796, 129.0756, countryCode = "KR"),
        )

        assertNull(result)
    }

    private fun noNetworkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { throw AssertionError("Overseas location must not call JMA radar APIs") }
        .build()
}
