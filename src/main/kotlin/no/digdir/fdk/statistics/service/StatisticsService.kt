package no.digdir.fdk.statistics.service

import no.digdir.fdk.statistics.model.Concept
import no.digdir.fdk.statistics.model.DataService
import no.digdir.fdk.statistics.model.Dataset
import no.digdir.fdk.statistics.model.Event
import no.digdir.fdk.statistics.model.InformationModel
import no.digdir.fdk.statistics.model.LatestForDate
import no.digdir.fdk.statistics.model.CalculationRequest
import no.digdir.fdk.statistics.model.ResourceType
import no.digdir.fdk.statistics.model.Service
import no.digdir.fdk.statistics.model.StatsData
import no.digdir.fdk.statistics.model.TimeSeriesPoint
import no.digdir.fdk.statistics.model.TimeSeriesRequest
import no.digdir.fdk.statistics.repository.StatisticsRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.ZoneOffset

@Component
class StatisticsService(private val statisticsRepository: StatisticsRepository) {

    fun storeConceptStatistics(fdkId: String, concept: Concept, timestamp: Long) {
        statisticsRepository.store(
            StatsData(
                id = "$fdkId-$timestamp",
                fdkId = fdkId,
                timestamp = timestamp,
                removed = false,
                type = ResourceType.CONCEPT,
                orgPath = concept.publisher?.orgPath
            )
        )
    }

    fun storeDataServiceStatistics(fdkId: String, dataService: DataService, timestamp: Long) {
        statisticsRepository.store(
            StatsData(
                id = "$fdkId-$timestamp",
                fdkId = fdkId,
                timestamp = timestamp,
                removed = false,
                type = ResourceType.DATA_SERVICE,
                orgPath = dataService.publisher?.orgPath
            )
        )
    }

    fun storeDatasetStatistics(fdkId: String, dataset: Dataset, timestamp: Long) {
        statisticsRepository.store(
            StatsData(
                id = "$fdkId-$timestamp",
                fdkId = fdkId,
                timestamp = timestamp,
                removed = false,
                type = ResourceType.DATASET,
                orgPath = dataset.publisher?.orgPath,
                isRelatedToTransportportal = dataset.isRelatedToTransportportal ?: false,
            )
        )
    }

    fun storeEventStatistics(fdkId: String, event: Event, timestamp: Long) {
        statisticsRepository.store(
            StatsData(
                id = "$fdkId-$timestamp",
                fdkId = fdkId,
                timestamp = timestamp,
                removed = false,
                type = ResourceType.EVENT,
                orgPath = event.catalog?.publisher?.orgPath
            )
        )
    }

    fun storeInformationModelStatistics(fdkId: String, informationModel: InformationModel, timestamp: Long) {
        statisticsRepository.store(
            StatsData(
                id = "$fdkId-$timestamp",
                fdkId = fdkId,
                timestamp = timestamp,
                removed = false,
                type = ResourceType.INFORMATION_MODEL,
                orgPath = informationModel.publisher?.orgPath
            )
        )
    }

    fun storeServiceStatistics(fdkId: String, service: Service, timestamp: Long) {
        val orgPath = if (!service.hasCompetentAuthority.isNullOrEmpty()) service.hasCompetentAuthority.first().orgPath
        else if (!service.ownedBy.isNullOrEmpty()) service.ownedBy.first().orgPath
        else null

        statisticsRepository.store(
            StatsData(
                id = "$fdkId-$timestamp",
                fdkId = fdkId,
                timestamp = timestamp,
                removed = false,
                type = ResourceType.SERVICE,
                orgPath = orgPath
            )
        )
    }

    fun markAsRemovedForTimestamp(fdkId: String, timestamp: Long, resourceType: ResourceType) {
        statisticsRepository.store(
            StatsData(
                id = "$fdkId-$timestamp",
                fdkId = fdkId,
                timestamp = timestamp,
                removed = true,
                type = resourceType,
                orgPath = null
            )
        )
    }

    fun calculateLatest(req: CalculationRequest) {
        req.startInclusive
            .datesUntil(req.endExclusive)
            .forEach { date -> calculateLatestForDate(date) }
    }

    private fun LocalDate.toMillis(): Long =
        atStartOfDay()
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()

    private fun calculateLatestForDate(date: LocalDate) {
        statisticsRepository.latestForTimestamp(date.toMillis())
            .forEach {
                statisticsRepository.storeForDate(
                    LatestForDate(
                        fdkId = it.value,
                        calculatedForDate = date,
                        statId = it.key
                    )
                )
            }
    }

    fun timeSeries(req: TimeSeriesRequest): List<TimeSeriesPoint> {
        if (req.start > req.end) throw ResponseStatusException(HttpStatus.BAD_REQUEST)

        return statisticsRepository.timeSeries(
            req.start,
            req.end,
            req.interval
        )
    }

}
