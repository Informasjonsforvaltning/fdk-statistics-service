package no.digdir.fdk.statistics.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Service(
    val ownedBy: List<ServiceOrganization>?,
    val hasCompetentAuthority: List<ServiceOrganization>?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ServiceOrganization(
    val orgPath: String?,
)

