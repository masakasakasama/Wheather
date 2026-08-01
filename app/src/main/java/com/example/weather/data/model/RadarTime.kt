package com.example.weather.data.model

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val RadarApiTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
private val RadarDisplayTimeFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")
private val JapanTimeZone = ZoneId.of("Asia/Tokyo")

/** JMA radar target times are UTC even though they do not include an offset. */
fun String.toRadarEpochMillis(): Long = LocalDateTime.parse(this, RadarApiTimeFormatter)
    .toInstant(ZoneOffset.UTC)
    .toEpochMilli()

fun String.toRadarDisplayTime(): String = runCatching {
    LocalDateTime.parse(this, RadarApiTimeFormatter)
        .toInstant(ZoneOffset.UTC)
        .atZone(JapanTimeZone)
        .format(RadarDisplayTimeFormatter)
}.getOrElse { this }
