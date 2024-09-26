package no.digdir.fdk.statistics.model

import java.time.LocalDate

data class ResourceEventMetrics(
    val id: String,
    val fdkId: String,
    val timestamp: Long,
    val removed: Boolean,
    val type: ResourceType,
    val orgPath: String? = null,
    val isRelatedToTransportportal: Boolean = false,
)

data class LatestForDate(
    val fdkId: String,
    val calculatedForDate: LocalDate,
    val statId: String
)
