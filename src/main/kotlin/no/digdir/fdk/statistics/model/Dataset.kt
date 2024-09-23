package no.digdir.fdk.statistics.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Dataset(
    val publisher: Organization?,
    val isRelatedToTransportportal: Boolean?
)
