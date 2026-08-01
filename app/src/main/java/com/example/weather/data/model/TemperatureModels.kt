package com.example.weather.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CurrentTemperatureKind {
    OBSERVATION,
    MODEL_ESTIMATE,
}

@Serializable
data class CurrentTemperatureSource(
    val kind: CurrentTemperatureKind = CurrentTemperatureKind.MODEL_ESTIMATE,
    val provider: String = "予報モデル",
    val stationId: String? = null,
    val stationName: String? = null,
    val distanceKm: Double? = null,
    val dataTimeMillis: Long? = null,
    val modelCount: Int = 1,
    val rangeLowC: Double? = null,
    val rangeHighC: Double? = null,
)

fun CurrentTemperatureSource.hasFreshObservation(nowMillis: Long = System.currentTimeMillis()): Boolean {
    val observedAt = dataTimeMillis ?: return false
    val ageMillis = nowMillis - observedAt
    return kind == CurrentTemperatureKind.OBSERVATION && ageMillis in -5 * 60 * 1000L..20 * 60 * 1000L
}

@Serializable
data class TemperatureForecastMetadata(
    val modelNames: List<String> = emptyList(),
    val generatedAtMillis: Long? = null,
    val hasAdaptiveWeights: Boolean = false,
    val verificationSampleCount: Int = 0,
    val observationAnchored: Boolean = false,
)

data class TemperatureObservation(
    val temperatureC: Double,
    val stationId: String,
    val stationName: String,
    val distanceKm: Double,
    val observedAtMillis: Long,
)

data class ModelTemperaturePoint(
    val time: String,
    val temperatureC: Double?,
)

data class ModelTemperatureSeries(
    val modelId: String,
    val displayName: String,
    val points: List<ModelTemperaturePoint>,
)

data class MultiModelTemperatureForecast(
    val models: List<ModelTemperatureSeries>,
    val fetchedAtMillis: Long,
)

@Serializable
data class ModelTemperatureSkill(
    val verificationKey: String,
    val modelId: String,
    val leadBucket: String,
    val sampleCount: Int = 0,
    val meanAbsoluteErrorC: Double = 0.0,
    val meanBiasC: Double = 0.0,
    val lastEvaluatedAtMillis: Long = 0L,
)

@Serializable
data class PendingTemperaturePrediction(
    val verificationKey: String,
    val modelId: String,
    val validTimeMillis: Long,
    val issuedAtMillis: Long,
    val leadBucket: String,
    val temperatureC: Double,
)

@Serializable
data class TemperatureAccuracyState(
    val skills: List<ModelTemperatureSkill> = emptyList(),
    val pendingPredictions: List<PendingTemperaturePrediction> = emptyList(),
)

@Serializable
data class AmedasStation(
    val type: String? = null,
    val elems: String? = null,
    val lat: List<Double> = emptyList(),
    val lon: List<Double> = emptyList(),
    val alt: Double? = null,
    @SerialName("kjName") val name: String = "",
)

@Serializable
data class AmedasObservationValue(
    val temp: List<Double?>? = null,
)
