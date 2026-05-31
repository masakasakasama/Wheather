package com.example.weather.data.api

import com.example.weather.data.model.DisasterSummary
import com.example.weather.data.model.TyphoonSummary
import com.example.weather.data.model.WeatherLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class JmaDisasterClient(
    private val httpClient: OkHttpClient,
    private val json: Json,
) {
    suspend fun fetchSummary(location: WeatherLocation): Result<DisasterSummary> = runCatching {
        withContext(Dispatchers.IO) {
            val office = nearestOffice(location)
            val warnings = fetchWarningSummary(office)
            val typhoons = fetchTyphoons()
            DisasterSummary(
                officeName = office.name,
                warningHeadline = warnings.headline,
                activeWarnings = warnings.activeWarnings,
                typhoons = typhoons,
                updatedAtMillis = System.currentTimeMillis(),
            )
        }
    }

    private fun fetchWarningSummary(office: JmaOffice): WarningSummary {
        val request = Request.Builder()
            .url("https://www.jma.go.jp/bosai/warning/data/warning/${office.code}.json")
            .header("User-Agent", "PersonalWeather/1.0")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return WarningSummary(null, emptyList())
            val root = json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
            val headline = root["headlineText"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            val warningNames = mutableSetOf<String>()
            root["areaTypes"]?.jsonArray.orEmpty().forEach { areaType ->
                areaType.jsonObject["areas"]?.jsonArray.orEmpty().forEach { area ->
                    area.jsonObject["warnings"]?.jsonArray.orEmpty().forEach { warning ->
                        val item = warning.jsonObject
                        val status = item["status"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        val code = item["code"]?.jsonPrimitive?.contentOrNull
                        if (code != null && status != "解除") {
                            warningNames += warningName(code)
                        }
                    }
                }
            }
            return WarningSummary(headline, warningNames.sortedWith(warningComparator))
        }
    }

    private fun fetchTyphoons(): List<TyphoonSummary> {
        val request = Request.Builder()
            .url("https://www.jma.go.jp/bosai/typhoon/data/targetTc.json")
            .header("User-Agent", "PersonalWeather/1.0")
            .build()
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use emptyList()
            val root = json.parseToJsonElement(response.body?.string().orEmpty())
            if (root !is JsonArray) return@use emptyList()
            root.mapNotNull { element -> element.toTyphoonSummary() }
        }
    }

    private fun JsonElement.toTyphoonSummary(): TyphoonSummary? {
        val item = (this as? JsonObject) ?: return null
        val category = item["category"]?.jsonPrimitive?.contentOrNull.orEmpty()
        if (category == "LOW") return null
        val number = item["typhoonNumber"]?.jsonPrimitive?.contentOrNull ?: return null
        val issue = item["issue"]?.jsonPrimitive?.contentOrNull.orEmpty()
        return TyphoonSummary(
            number = number.takeLast(2).trimStart('0').ifBlank { number },
            category = typhoonCategoryLabel(category),
            issueTime = issue,
        )
    }

    private fun nearestOffice(location: WeatherLocation): JmaOffice {
        return Offices.minBy { it.distanceKm(location.latitude, location.longitude) }
    }

    private fun JmaOffice.distanceKm(latitude: Double, longitude: Double): Double {
        val radius = 6371.0
        val dLat = Math.toRadians(lat - latitude)
        val dLon = Math.toRadians(lon - longitude)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(lat)) * sin(dLon / 2).pow(2.0)
        return radius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}

private data class WarningSummary(
    val headline: String?,
    val activeWarnings: List<String>,
)

private data class JmaOffice(
    val code: String,
    val name: String,
    val lat: Double,
    val lon: Double,
)

private val warningComparator = compareBy<String> {
    when {
        it.contains("特別警報") -> 0
        it.contains("警報") -> 1
        else -> 2
    }
}.thenBy { it }

private fun warningName(code: String): String = when (code) {
    "02" -> "暴風雪警報"
    "03" -> "大雨警報"
    "04" -> "洪水警報"
    "05" -> "暴風警報"
    "06" -> "大雪警報"
    "07" -> "波浪警報"
    "08" -> "高潮警報"
    "10" -> "大雨注意報"
    "12" -> "大雪注意報"
    "13" -> "風雪注意報"
    "14" -> "雷注意報"
    "15" -> "強風注意報"
    "16" -> "波浪注意報"
    "17" -> "融雪注意報"
    "18" -> "洪水注意報"
    "19" -> "高潮注意報"
    "20" -> "濃霧注意報"
    "21" -> "乾燥注意報"
    "22" -> "なだれ注意報"
    "23" -> "低温注意報"
    "24" -> "霜注意報"
    "25" -> "着氷注意報"
    "26" -> "着雪注意報"
    "32" -> "暴風雪特別警報"
    "33" -> "大雨特別警報"
    "35" -> "暴風特別警報"
    "36" -> "大雪特別警報"
    "37" -> "波浪特別警報"
    "38" -> "高潮特別警報"
    else -> "警報・注意報$code"
}

private fun typhoonCategoryLabel(category: String): String = when (category) {
    "TD" -> "熱帯低気圧"
    "TS" -> "台風"
    "STS" -> "強い台風"
    "TY" -> "非常に強い台風"
    else -> category
}

private val Offices = listOf(
    JmaOffice("016000", "石狩・空知・後志地方", 43.0618, 141.3545),
    JmaOffice("020000", "青森県", 40.8244, 140.7400),
    JmaOffice("030000", "岩手県", 39.7036, 141.1527),
    JmaOffice("040000", "宮城県", 38.2682, 140.8694),
    JmaOffice("050000", "秋田県", 39.7186, 140.1024),
    JmaOffice("060000", "山形県", 38.2404, 140.3633),
    JmaOffice("070000", "福島県", 37.7503, 140.4676),
    JmaOffice("080000", "茨城県", 36.3418, 140.4468),
    JmaOffice("090000", "栃木県", 36.5657, 139.8836),
    JmaOffice("100000", "群馬県", 36.3912, 139.0609),
    JmaOffice("110000", "埼玉県", 35.8569, 139.6489),
    JmaOffice("120000", "千葉県", 35.6074, 140.1065),
    JmaOffice("130000", "東京都", 35.6812, 139.7671),
    JmaOffice("140000", "神奈川県", 35.4437, 139.6380),
    JmaOffice("150000", "新潟県", 37.9026, 139.0232),
    JmaOffice("160000", "富山県", 36.6953, 137.2113),
    JmaOffice("170000", "石川県", 36.5947, 136.6256),
    JmaOffice("180000", "福井県", 36.0652, 136.2216),
    JmaOffice("190000", "山梨県", 35.6639, 138.5684),
    JmaOffice("200000", "長野県", 36.6486, 138.1948),
    JmaOffice("210000", "岐阜県", 35.4233, 136.7607),
    JmaOffice("220000", "静岡県", 34.9756, 138.3828),
    JmaOffice("230000", "愛知県", 35.1815, 136.9066),
    JmaOffice("240000", "三重県", 34.7303, 136.5086),
    JmaOffice("250000", "滋賀県", 35.0045, 135.8686),
    JmaOffice("260000", "京都府", 35.0116, 135.7681),
    JmaOffice("270000", "大阪府", 34.6937, 135.5023),
    JmaOffice("280000", "兵庫県", 34.6901, 135.1955),
    JmaOffice("290000", "奈良県", 34.6851, 135.8048),
    JmaOffice("300000", "和歌山県", 34.2260, 135.1675),
    JmaOffice("310000", "鳥取県", 35.5011, 134.2351),
    JmaOffice("320000", "島根県", 35.4723, 133.0505),
    JmaOffice("330000", "岡山県", 34.6618, 133.9350),
    JmaOffice("340000", "広島県", 34.3853, 132.4553),
    JmaOffice("350000", "山口県", 34.1785, 131.4737),
    JmaOffice("360000", "徳島県", 34.0703, 134.5548),
    JmaOffice("370000", "香川県", 34.3401, 134.0434),
    JmaOffice("380000", "愛媛県", 33.8416, 132.7657),
    JmaOffice("390000", "高知県", 33.5597, 133.5311),
    JmaOffice("400000", "福岡県", 33.5902, 130.4017),
    JmaOffice("410000", "佐賀県", 33.2494, 130.2988),
    JmaOffice("420000", "長崎県", 32.7448, 129.8737),
    JmaOffice("430000", "熊本県", 32.8031, 130.7079),
    JmaOffice("440000", "大分県", 33.2382, 131.6126),
    JmaOffice("450000", "宮崎県", 31.9077, 131.4202),
    JmaOffice("460100", "鹿児島県", 31.5966, 130.5571),
    JmaOffice("471000", "沖縄本島地方", 26.2124, 127.6809),
)
