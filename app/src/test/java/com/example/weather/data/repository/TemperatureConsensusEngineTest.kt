package com.example.weather.data.repository

import com.example.weather.data.model.CurrentTemperatureKind
import com.example.weather.data.model.CurrentWeather
import com.example.weather.data.model.DailyWeather
import com.example.weather.data.model.HourlyWeather
import com.example.weather.data.model.ModelTemperaturePoint
import com.example.weather.data.model.ModelTemperatureSeries
import com.example.weather.data.model.ModelTemperatureSkill
import com.example.weather.data.model.MultiModelTemperatureForecast
import com.example.weather.data.model.PendingTemperaturePrediction
import com.example.weather.data.model.TemperatureAccuracyState
import com.example.weather.data.model.TemperatureObservation
import com.example.weather.data.model.WeatherLocation
import com.example.weather.data.model.WeatherSnapshot
import com.example.weather.data.model.forecastAreaKey
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemperatureConsensusEngineTest {
    private val zone = ZoneId.of("Asia/Tokyo")
    private val now = LocalDateTime.parse("2026-08-01T09:46").atZone(zone).toInstant().toEpochMilli()
    private val engine = TemperatureConsensusEngine { now }

    @Test
    fun observationOverridesCurrentAndDailyUsesMergedHourlyValues() {
        val result = engine.apply(
            base = baseSnapshot(),
            modelForecast = forecast(
                jma = listOf(32.0, 35.0),
                ecmwf = listOf(34.0, 37.0),
                gfs = listOf(50.0, 53.0),
            ),
            observation = TemperatureObservation(31.8, "44132", "東京", 3.2, now - 6 * 60 * 1000L),
            previousAccuracyState = TemperatureAccuracyState(),
        )

        assertEquals(31.8, result.snapshot.current.temperatureC)
        assertEquals(CurrentTemperatureKind.OBSERVATION, result.snapshot.currentTemperatureSource.kind)
        assertEquals("東京", result.snapshot.currentTemperatureSource.stationName)
        assertTrue(result.snapshot.hourly.first().temperatureC!! in 31.7..31.9)
        assertTrue(result.snapshot.hourly.first().temperatureHighC!! - result.snapshot.hourly.first().temperatureLowC!! < 0.2)
        assertTrue(result.snapshot.daily.single().maxTemperatureC!! in 34.8..35.0)
        assertEquals(result.snapshot.hourly.maxOf { it.temperatureC!! }, result.snapshot.daily.single().maxTemperatureC)
    }

    @Test
    fun withoutObservationMarksCurrentAsModelEstimate() {
        val result = engine.apply(
            base = baseSnapshot(),
            modelForecast = forecast(
                jma = listOf(32.0, 35.0),
                ecmwf = listOf(34.0, 37.0),
                gfs = listOf(33.0, 36.0),
            ),
            observation = null,
            previousAccuracyState = TemperatureAccuracyState(),
        )

        assertEquals(CurrentTemperatureKind.MODEL_ESTIMATE, result.snapshot.currentTemperatureSource.kind)
        assertEquals(3, result.snapshot.currentTemperatureSource.modelCount)
        assertTrue(result.snapshot.forecastSource.contains("JMA・ECMWF・GFS"))
    }

    @Test
    fun learnedAccuracyGivesMoreWeightToHistoricallyBetterModel() {
        val learned = TemperatureAccuracyState(
            skills = listOf(
                skill("jma_seamless", mae = 0.5),
                skill("ecmwf_ifs025", mae = 3.0),
                skill("gfs_seamless", mae = 3.0),
            ),
        )

        val result = engine.apply(
            base = baseSnapshot(),
            modelForecast = forecast(
                jma = listOf(30.0, 31.0),
                ecmwf = listOf(34.0, 35.0),
                gfs = listOf(35.0, 36.0),
            ),
            observation = null,
            previousAccuracyState = learned,
        )

        assertEquals(30.0, result.snapshot.hourly.first().temperatureC)
        assertTrue(result.snapshot.temperatureForecast.hasAdaptiveWeights)
    }

    @Test
    fun evaluatesMaturedPredictionAgainstMatchingStationObservation() {
        val validTime = now - 5 * 60 * 1000L
        val pending = PendingTemperaturePrediction(
            verificationKey = "44132",
            modelId = "jma_seamless",
            validTimeMillis = validTime,
            issuedAtMillis = validTime - 2 * 60 * 60 * 1000L,
            leadBucket = "0-24h",
            temperatureC = 33.0,
        )

        val result = engine.apply(
            base = baseSnapshot(),
            modelForecast = null,
            observation = TemperatureObservation(31.5, "44132", "東京", 3.2, validTime),
            previousAccuracyState = TemperatureAccuracyState(pendingPredictions = listOf(pending)),
        )

        val evaluated = result.accuracyState.skills.single()
        assertEquals(1, evaluated.sampleCount)
        assertEquals(1.5, evaluated.meanAbsoluteErrorC)
        assertFalse(result.accuracyState.pendingPredictions.contains(pending))
    }

    @Test
    fun keepsIndependentPredictionsForEachLeadBucket() {
        val shortTime = "2026-08-03T12:00"
        val longTime = "2026-08-05T12:00"
        val forecast = MultiModelTemperatureForecast(
            models = listOf(
                ModelTemperatureSeries(
                    modelId = "jma_seamless",
                    displayName = "JMA",
                    points = listOf(
                        ModelTemperaturePoint(shortTime, 31.0),
                        ModelTemperaturePoint(longTime, 30.0),
                    ),
                ),
            ),
            fetchedAtMillis = now,
        )
        val first = engine.apply(
            base = baseSnapshot(),
            modelForecast = forecast,
            observation = null,
            previousAccuracyState = TemperatureAccuracyState(),
        ).accuracyState

        val later = TemperatureConsensusEngine { now + 30 * 60 * 60 * 1000L }.apply(
            base = baseSnapshot(),
            modelForecast = forecast,
            observation = null,
            previousAccuracyState = first,
        ).accuracyState

        val shortValidTime = LocalDateTime.parse(shortTime).atZone(zone).toInstant().toEpochMilli()
        val longValidTime = LocalDateTime.parse(longTime).atZone(zone).toInstant().toEpochMilli()
        assertEquals(
            setOf("24-72h", "0-24h"),
            later.pendingPredictions.filter { it.validTimeMillis == shortValidTime }.map { it.leadBucket }.toSet(),
        )
        assertEquals(
            setOf("72h+", "24-72h"),
            later.pendingPredictions.filter { it.validTimeMillis == longValidTime }.map { it.leadBucket }.toSet(),
        )
    }

    private fun baseSnapshot() = WeatherSnapshot(
        location = WeatherLocation("現在地", 35.68, 139.76),
        current = CurrentWeather(30.0, weatherCode = 1, precipitationMm = 0.0, time = "2026-08-01T09:45"),
        hourly = listOf(
            HourlyWeather("2026-08-01T10:00", 30.0, 10, 1, 0.0),
            HourlyWeather("2026-08-01T11:00", 31.0, 10, 1, 0.0),
        ),
        daily = listOf(DailyWeather("2026-08-01", 1, 33.0, 25.0, 10)),
        updatedAtMillis = now,
        timezone = "Asia/Tokyo",
    )

    private fun forecast(jma: List<Double>, ecmwf: List<Double>, gfs: List<Double>) =
        MultiModelTemperatureForecast(
            models = listOf(
                series("jma_seamless", "JMA", jma),
                series("ecmwf_ifs025", "ECMWF", ecmwf),
                series("gfs_seamless", "GFS", gfs),
            ),
            fetchedAtMillis = now,
        )

    private fun series(id: String, name: String, temperatures: List<Double>) =
        ModelTemperatureSeries(
            modelId = id,
            displayName = name,
            points = listOf("2026-08-01T10:00", "2026-08-01T11:00").mapIndexed { index, time ->
                ModelTemperaturePoint(time, temperatures[index])
            },
        )

    private fun skill(modelId: String, mae: Double) = ModelTemperatureSkill(
        verificationKey = WeatherLocation("test", 35.68, 139.76).forecastAreaKey(),
        modelId = modelId,
        leadBucket = "0-24h",
        sampleCount = 3,
        meanAbsoluteErrorC = mae,
    )
}
