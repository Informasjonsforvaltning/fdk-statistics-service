package no.digdir.fdk.statistics.utils

import no.digdir.fdk.statistics.model.Catalog
import no.digdir.fdk.statistics.model.Concept
import no.digdir.fdk.statistics.model.DataService
import no.digdir.fdk.statistics.model.Dataset
import no.digdir.fdk.statistics.model.Event
import no.digdir.fdk.statistics.model.InformationModel
import no.digdir.fdk.statistics.model.Interval
import no.digdir.fdk.statistics.model.Organization
import no.digdir.fdk.statistics.model.Service
import no.digdir.fdk.statistics.model.ServiceOrganization
import no.digdir.fdk.statistics.model.TimeSeriesRequest

val CONCEPT_0 = Concept(publisher = Organization(orgPath = "/STAT/123456789"))
val CONCEPT_1 = Concept(publisher = Organization(orgPath = "/PRIVAT/987654321"))

val DATA_SERVICE_0 = DataService(publisher = Organization(orgPath = "/STAT/123456789"))
val DATA_SERVICE_1 = DataService(publisher = Organization(orgPath = "/PRIVAT/987654321"))

val DATASET_0 = Dataset(publisher = Organization(orgPath = "/STAT/123456789"), isRelatedToTransportportal = true)
val DATASET_1 = Dataset(publisher = Organization(orgPath = "/PRIVAT/987654321"), isRelatedToTransportportal = false)

val EVENT_0 = Event(catalog = Catalog(publisher = Organization(orgPath = "/STAT/123456789")))
val EVENT_1 = Event(catalog = Catalog(publisher = Organization(orgPath = "/PRIVAT/987654321")))

val INFORMATION_MODEL_0 = InformationModel(publisher = Organization(orgPath = "/STAT/123456789"))
val INFORMATION_MODEL_1 = InformationModel(publisher = Organization(orgPath = "/PRIVAT/987654321"))

val SERVICE_0 =
    Service(ownedBy = emptyList(), hasCompetentAuthority = listOf(ServiceOrganization(orgPath = "/STAT/123456789")))
val SERVICE_1 =
    Service(ownedBy = listOf(ServiceOrganization(orgPath = "/PRIVAT/987654321")), hasCompetentAuthority = emptyList())

val TIME_SERIES_REQUEST = TimeSeriesRequest(
    start = "2024-01-11",
    end = "2024-08-01",
    interval = Interval.MONTH
)
