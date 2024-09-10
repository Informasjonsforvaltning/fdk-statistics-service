package no.digdir.fdk.statistics.model

data class TimeSeriesPoint(
    val dateMillis: Long,
    val count: Int,
)
