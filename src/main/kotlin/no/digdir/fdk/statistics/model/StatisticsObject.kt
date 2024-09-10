package no.digdir.fdk.statistics.model

data class StatisticsObject(
    val id: String,
    val fdkId: String,
    val timestamp: Long,
    val removed: Boolean,
    val type: ResourceType,
    val orgPath: String? = null,
    val isRelatedToTransportportal: Boolean = false,
)
