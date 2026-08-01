package com.example.weather.data.model

data class DisasterSummary(
    val officeName: String?,
    val warningHeadline: String?,
    val activeWarnings: List<String>,
    val typhoons: List<TyphoonSummary>,
    val updatedAtMillis: Long,
) {
    val hasImportantInfo: Boolean
        get() = activeWarnings.isNotEmpty() || typhoons.isNotEmpty() || !warningHeadline.isNullOrBlank()
}

data class TyphoonSummary(
    val number: String?,
    val category: String,
    val issueTime: String,
)

fun TyphoonSummary.displayLabel(): String = when {
    category == "熱帯低気圧" && number != null -> "台風第${number}号から変わった熱帯低気圧"
    category == "熱帯低気圧" -> "熱帯低気圧"
    number != null -> "台風第${number}号"
    else -> "台風情報"
}
