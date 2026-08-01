package com.example.weather.data.repository

import com.example.weather.data.model.CurrentTemperatureKind
import com.example.weather.data.model.CurrentTemperatureSource
import com.example.weather.data.model.ModelTemperatureSeries
import com.example.weather.data.model.ModelTemperatureSkill
import com.example.weather.data.model.MultiModelTemperatureForecast
import com.example.weather.data.model.PendingTemperaturePrediction
import com.example.weather.data.model.TemperatureAccuracyState
import com.example.weather.data.model.TemperatureForecastMetadata
import com.example.weather.data.model.TemperatureObservation
import com.example.weather.data.model.WeatherSnapshot
import com.example.weather.data.model.forecastAreaKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class TemperatureConsensusResult(
    val snapshot: WeatherSnapshot,
    val accuracyState: TemperatureAccuracyState,
)

class TemperatureConsensusEngine(
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    fun apply(
        base: WeatherSnapshot,
        modelForecast: MultiModelTemperatureForecast?,
        observation: TemperatureObservation?,
        previousAccuracyState: TemperatureAccuracyState,
    ): TemperatureConsensusResult {
        val now = nowMillis()
        val zone = runCatching { ZoneId.of(base.timezone) }.getOrDefault(ZoneId.of("Asia/Tokyo"))
        val verificationKey = observation?.stationId ?: base.location.forecastAreaKey()
        val evaluatedState = evaluatePending(previousAccuracyState, observation)
        val modelMaps = modelForecast?.models.orEmpty().associateWith { series ->
            series.points.associate { it.time to it.temperatureC }
        }
        val liveCorrections = liveCorrections(
            series = modelForecast?.models.orEmpty(),
            observation = observation,
            verificationKey = verificationKey,
            accuracyState = evaluatedState,
            zone = zone,
        )

        val mergedHourly = base.hourly.map { hour ->
            val validTimeMillis = hour.time.toEpochMillis(zone) ?: return@map hour
            val consensus = consensusFor(
                series = modelMaps,
                time = hour.time,
                validTimeMillis = validTimeMillis,
                verificationKey = verificationKey,
                accuracyState = evaluatedState,
                nowMillis = now,
                liveCorrections = liveCorrections,
                observationTimeMillis = observation?.observedAtMillis,
            ) ?: return@map hour
            hour.copy(
                temperatureC = consensus.valueC,
                temperatureLowC = consensus.lowC,
                temperatureHighC = consensus.highC,
                temperatureModelCount = consensus.modelCount,
            )
        }

        val today = LocalDate.now(zone).toString()
        val mergedDaily = base.daily.map { day ->
            val dayHours = mergedHourly.filter { it.time.take(10) == day.date }
            if (dayHours.isEmpty()) return@map day
            val mergedTemperatures = dayHours.mapNotNull { it.temperatureC }
            if (mergedTemperatures.isEmpty()) return@map day
            var maxTemperature = mergedTemperatures.maxOrNull()
            var minTemperature = mergedTemperatures.minOrNull()
            if (day.date == today && observation != null) {
                maxTemperature = maxOfNullable(maxTemperature, observation.temperatureC)
                minTemperature = minOfNullable(minTemperature, observation.temperatureC)
            }
            day.copy(
                maxTemperatureC = maxTemperature,
                minTemperatureC = minTemperature,
                maxTemperatureLowC = dayHours.mapNotNull { it.temperatureLowC }.maxOrNull(),
                maxTemperatureHighC = dayHours.mapNotNull { it.temperatureHighC }.maxOrNull(),
                minTemperatureLowC = dayHours.mapNotNull { it.temperatureLowC }.minOrNull(),
                minTemperatureHighC = dayHours.mapNotNull { it.temperatureHighC }.minOrNull(),
                temperatureModelCount = dayHours.maxOfOrNull { it.temperatureModelCount } ?: 0,
            )
        }

        val nearestHour = mergedHourly.minByOrNull { hour ->
            val time = hour.time.toEpochMillis(zone) ?: Long.MAX_VALUE
            abs(time - now)
        }
        val baseTemperature = base.current.temperatureC
        val resolvedTemperature = observation?.temperatureC ?: nearestHour?.temperatureC ?: baseTemperature
        val apparentOffset = if (resolvedTemperature != null && baseTemperature != null) {
            resolvedTemperature - baseTemperature
        } else {
            0.0
        }
        val source = if (observation != null) {
            CurrentTemperatureSource(
                kind = CurrentTemperatureKind.OBSERVATION,
                provider = "気象庁アメダス",
                stationId = observation.stationId,
                stationName = observation.stationName,
                distanceKm = observation.distanceKm,
                dataTimeMillis = observation.observedAtMillis,
            )
        } else {
            CurrentTemperatureSource(
                kind = CurrentTemperatureKind.MODEL_ESTIMATE,
                provider = modelForecast?.models?.joinToString("・") { it.displayName }
                    ?.takeIf { it.isNotBlank() }
                    ?: "Open-Meteo",
                dataTimeMillis = modelForecast?.fetchedAtMillis ?: base.updatedAtMillis,
                modelCount = nearestHour?.temperatureModelCount?.takeIf { it > 0 }
                    ?: modelForecast?.models?.size
                    ?: 1,
                rangeLowC = nearestHour?.temperatureLowC,
                rangeHighC = nearestHour?.temperatureHighC,
            )
        }

        val withPending = addPendingPredictions(
            state = evaluatedState,
            forecast = modelForecast,
            verificationKey = verificationKey,
            zone = zone,
            nowMillis = now,
        )
        val relevantSkills = withPending.skills.filter { it.verificationKey == verificationKey }
        val modelNames = modelForecast?.models?.map { it.displayName }.orEmpty()
        val sourceLabel = if (modelNames.isEmpty()) {
            base.forecastSource
        } else {
            val adjustment = if (liveCorrections.isNotEmpty()) "・アメダス実況補正" else ""
            "気温: ${modelNames.joinToString("・")}統合$adjustment / 天気・降水: ${base.forecastSource}"
        }
        val snapshot = base.copy(
            current = base.current.copy(
                temperatureC = resolvedTemperature,
                apparentTemperatureC = base.current.apparentTemperatureC?.plus(apparentOffset),
                modelTemperatureC = baseTemperature,
            ),
            hourly = mergedHourly,
            daily = mergedDaily,
            updatedAtMillis = now,
            forecastSource = sourceLabel,
            currentTemperatureSource = source,
            temperatureForecast = TemperatureForecastMetadata(
                modelNames = modelNames,
                generatedAtMillis = modelForecast?.fetchedAtMillis,
                hasAdaptiveWeights = relevantSkills.any { it.sampleCount >= MIN_SKILL_SAMPLES },
                verificationSampleCount = relevantSkills.sumOf { it.sampleCount },
                observationAnchored = liveCorrections.isNotEmpty(),
            ),
        )
        return TemperatureConsensusResult(snapshot, withPending)
    }

    private fun consensusFor(
        series: Map<ModelTemperatureSeries, Map<String, Double?>>,
        time: String,
        validTimeMillis: Long,
        verificationKey: String,
        accuracyState: TemperatureAccuracyState,
        nowMillis: Long,
        liveCorrections: Map<String, Double>,
        observationTimeMillis: Long?,
    ): ConsensusTemperature? {
        val bucket = leadBucket(validTimeMillis - nowMillis)
        val values = series.mapNotNull { (model, points) ->
            val raw = points[time] ?: return@mapNotNull null
            val skill = accuracyState.skills.firstOrNull {
                it.verificationKey == verificationKey && it.modelId == model.modelId && it.leadBucket == bucket
            }
            val historicallyCorrected = if (skill != null && skill.sampleCount >= MIN_SKILL_SAMPLES) {
                raw - skill.meanBiasC
            } else {
                raw
            }
            val liveFactor = observationTimeMillis?.let {
                (1.0 - abs(validTimeMillis - it).toDouble() / LIVE_ANCHOR_DECAY_MILLIS).coerceIn(0.0, 1.0)
            } ?: 0.0
            val corrected = historicallyCorrected + (liveCorrections[model.modelId] ?: 0.0) * liveFactor
            val weight = if (skill != null && skill.sampleCount >= MIN_SKILL_SAMPLES) {
                (1.0 / (skill.meanAbsoluteErrorC * skill.meanAbsoluteErrorC + 0.25)).coerceIn(0.25, 4.0)
            } else {
                1.0
            }
            WeightedTemperature(corrected, weight)
        }
        if (values.isEmpty()) return null

        val ordinaryMedian = values.map { it.temperatureC }.sorted().let { sorted ->
            sorted[sorted.size / 2]
        }
        val inliers = values.filter { abs(it.temperatureC - ordinaryMedian) <= OUTLIER_THRESHOLD_C }
            .ifEmpty { values }
        val resolved = when {
            inliers.size == 1 -> inliers.first().temperatureC
            inliers.size == 2 -> inliers.sumOf { it.temperatureC * it.weight } / inliers.sumOf { it.weight }
            else -> weightedMedian(inliers)
        }
        return ConsensusTemperature(
            valueC = resolved,
            lowC = inliers.minOf { it.temperatureC },
            highC = inliers.maxOf { it.temperatureC },
            modelCount = inliers.size,
        )
    }

    private fun liveCorrections(
        series: List<ModelTemperatureSeries>,
        observation: TemperatureObservation?,
        verificationKey: String,
        accuracyState: TemperatureAccuracyState,
        zone: ZoneId,
    ): Map<String, Double> {
        if (observation == null) return emptyMap()
        return series.mapNotNull { model ->
            val nearest = model.points.mapNotNull { point ->
                val timeMillis = point.time.toEpochMillis(zone) ?: return@mapNotNull null
                val temperature = point.temperatureC ?: return@mapNotNull null
                Triple(timeMillis, temperature, abs(timeMillis - observation.observedAtMillis))
            }.minByOrNull { it.third } ?: return@mapNotNull null
            if (nearest.third > LIVE_ANCHOR_MATCH_WINDOW_MILLIS) return@mapNotNull null
            val skill = accuracyState.skills.firstOrNull {
                it.verificationKey == verificationKey &&
                    it.modelId == model.modelId &&
                    it.leadBucket == "0-24h"
            }
            val correctedAtObservation = if (skill != null && skill.sampleCount >= MIN_SKILL_SAMPLES) {
                nearest.second - skill.meanBiasC
            } else {
                nearest.second
            }
            model.modelId to (observation.temperatureC - correctedAtObservation).coerceIn(-MAX_LIVE_CORRECTION_C, MAX_LIVE_CORRECTION_C)
        }.toMap()
    }

    private fun evaluatePending(
        state: TemperatureAccuracyState,
        observation: TemperatureObservation?,
    ): TemperatureAccuracyState {
        if (observation == null) return state
        val matching = state.pendingPredictions.filter { prediction ->
            prediction.verificationKey == observation.stationId &&
                abs(prediction.validTimeMillis - observation.observedAtMillis) <= OBSERVATION_MATCH_WINDOW_MILLIS &&
                prediction.issuedAtMillis <= prediction.validTimeMillis - MIN_FORECAST_LEAD_MILLIS
        }
        if (matching.isEmpty()) return state

        val skills = state.skills.toMutableList()
        matching.forEach { prediction ->
            val error = prediction.temperatureC - observation.temperatureC
            val index = skills.indexOfFirst {
                it.verificationKey == prediction.verificationKey &&
                    it.modelId == prediction.modelId &&
                    it.leadBucket == prediction.leadBucket
            }
            val old = skills.getOrNull(index)
            val alpha = if (old == null || old.sampleCount < 30) {
                1.0 / ((old?.sampleCount ?: 0) + 1.0)
            } else {
                0.05
            }
            val updated = ModelTemperatureSkill(
                verificationKey = prediction.verificationKey,
                modelId = prediction.modelId,
                leadBucket = prediction.leadBucket,
                sampleCount = min((old?.sampleCount ?: 0) + 1, 200),
                meanAbsoluteErrorC = blend(old?.meanAbsoluteErrorC, abs(error), alpha),
                meanBiasC = blend(old?.meanBiasC, error, alpha),
                lastEvaluatedAtMillis = observation.observedAtMillis,
            )
            if (index >= 0) skills[index] = updated else skills += updated
        }
        val matchedKeys = matching.map { it.identityKey() }.toSet()
        return state.copy(
            skills = skills.sortedByDescending { it.lastEvaluatedAtMillis }.take(MAX_SKILL_RECORDS),
            pendingPredictions = state.pendingPredictions.filterNot { it.identityKey() in matchedKeys },
        )
    }

    private fun addPendingPredictions(
        state: TemperatureAccuracyState,
        forecast: MultiModelTemperatureForecast?,
        verificationKey: String,
        zone: ZoneId,
        nowMillis: Long,
    ): TemperatureAccuracyState {
        val retained = state.pendingPredictions.filter {
            it.validTimeMillis >= nowMillis - PENDING_RETENTION_PAST_MILLIS &&
                it.validTimeMillis <= nowMillis + PENDING_RETENTION_FUTURE_MILLIS
        }.toMutableList()
        if (forecast == null) return state.copy(pendingPredictions = retained)

        val existing = retained.map { it.identityKey() }.toMutableSet()
        forecast.models.forEach { model ->
            model.points.forEach pointLoop@{ point ->
                val validTime = point.time.toEpochMillis(zone) ?: return@pointLoop
                val temperature = point.temperatureC ?: return@pointLoop
                if (validTime !in (nowMillis + MIN_FORECAST_LEAD_MILLIS)..(nowMillis + PENDING_RETENTION_FUTURE_MILLIS)) {
                    return@pointLoop
                }
                val leadMillis = validTime - nowMillis
                if (leadMillis > 72 * HOUR_MILLIS && LocalDateTime.parse(point.time).hour % LONG_RANGE_SAMPLE_HOURS != 0) {
                    return@pointLoop
                }
                val prediction = PendingTemperaturePrediction(
                    verificationKey = verificationKey,
                    modelId = model.modelId,
                    validTimeMillis = validTime,
                    issuedAtMillis = nowMillis,
                    leadBucket = leadBucket(leadMillis),
                    temperatureC = temperature,
                )
                if (existing.add(prediction.identityKey())) retained += prediction
            }
        }
        return state.copy(pendingPredictions = retained.sortedBy { it.validTimeMillis }.take(MAX_PENDING_RECORDS))
    }

    private fun weightedMedian(values: List<WeightedTemperature>): Double {
        val sorted = values.sortedBy { it.temperatureC }
        val threshold = sorted.sumOf { it.weight } / 2.0
        var accumulated = 0.0
        sorted.forEach { value ->
            accumulated += value.weight
            if (accumulated >= threshold) return value.temperatureC
        }
        return sorted.last().temperatureC
    }

    private fun leadBucket(leadMillis: Long): String = when {
        leadMillis <= 24 * HOUR_MILLIS -> "0-24h"
        leadMillis <= 72 * HOUR_MILLIS -> "24-72h"
        else -> "72h+"
    }

    private fun blend(old: Double?, value: Double, alpha: Double): Double =
        if (old == null) value else old * (1.0 - alpha) + value * alpha

    private data class WeightedTemperature(val temperatureC: Double, val weight: Double)

    private data class ConsensusTemperature(
        val valueC: Double,
        val lowC: Double,
        val highC: Double,
        val modelCount: Int,
    )

    private companion object {
        const val MIN_SKILL_SAMPLES = 3
        const val OUTLIER_THRESHOLD_C = 5.0
        const val HOUR_MILLIS = 60 * 60 * 1000L
        const val MIN_FORECAST_LEAD_MILLIS = HOUR_MILLIS
        const val OBSERVATION_MATCH_WINDOW_MILLIS = 12 * 60 * 1000L
        const val LIVE_ANCHOR_MATCH_WINDOW_MILLIS = 45 * 60 * 1000L
        const val LIVE_ANCHOR_DECAY_MILLIS = 18 * HOUR_MILLIS
        const val MAX_LIVE_CORRECTION_C = 4.0
        const val PENDING_RETENTION_PAST_MILLIS = 2 * HOUR_MILLIS
        const val PENDING_RETENTION_FUTURE_MILLIS = 14 * 24 * HOUR_MILLIS
        const val LONG_RANGE_SAMPLE_HOURS = 6
        const val MAX_PENDING_RECORDS = 700
        const val MAX_SKILL_RECORDS = 120
    }
}

private fun String.toEpochMillis(zone: ZoneId): Long? = runCatching {
    LocalDateTime.parse(this).atZone(zone).toInstant().toEpochMilli()
}.getOrNull()

private fun PendingTemperaturePrediction.identityKey(): String =
    "$verificationKey|$modelId|$validTimeMillis|$leadBucket"

private fun maxOfNullable(first: Double?, second: Double): Double = first?.let { max(it, second) } ?: second

private fun minOfNullable(first: Double?, second: Double): Double = first?.let { min(it, second) } ?: second
