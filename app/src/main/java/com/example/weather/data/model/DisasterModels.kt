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
    val number: String,
    val category: String,
    val issueTime: String,
)
