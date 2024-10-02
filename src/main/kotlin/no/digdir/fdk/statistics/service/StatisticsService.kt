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
import no.digdir.fdk.statistics.model.ResourceEventMetrics
import no.digdir.fdk.statistics.model.TimeSeriesPoint
import no.digdir.fdk.statistics.model.TimeSeriesRequest
import no.digdir.fdk.statistics.repository.StatisticsRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneOffset

@Component
class StatisticsService(private val statisticsRepository: StatisticsRepository) {
    private val logger: Logger = LoggerFactory.getLogger(StatisticsService::class.java)

    fun storeConceptMetrics(fdkId: String, concept: Concept, timestamp: Long) {
        statisticsRepository.storeMetrics(
            ResourceEventMetrics(
                id = "$fdkId-$timestamp",
                fdkId = fdkId,
                timestamp = timestamp,
                removed = false,
                type = ResourceType.CONCEPT,
                orgPath = concept.publisher?.orgPath
            )
        )
    }

    fun storeDataServiceMetrics(fdkId: String, dataService: DataService, timestamp: Long) {
        statisticsRepository.storeMetrics(
            ResourceEventMetrics(
                id = "$fdkId-$timestamp",
                fdkId = fdkId,
                timestamp = timestamp,
                removed = false,
                type = ResourceType.DATA_SERVICE,
                orgPath = dataService.publisher?.orgPath
            )
        )
    }

    fun storeDatasetMetrics(fdkId: String, dataset: Dataset, timestamp: Long) {
        statisticsRepository.storeMetrics(
            ResourceEventMetrics(
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

    fun storeEventMetrics(fdkId: String, event: Event, timestamp: Long) {
        statisticsRepository.storeMetrics(
            ResourceEventMetrics(
                id = "$fdkId-$timestamp",
                fdkId = fdkId,
                timestamp = timestamp,
                removed = false,
                type = ResourceType.EVENT,
                orgPath = event.catalog?.publisher?.orgPath
            )
        )
    }

    fun storeInformationModelMetrics(fdkId: String, informationModel: InformationModel, timestamp: Long) {
        statisticsRepository.storeMetrics(
            ResourceEventMetrics(
                id = "$fdkId-$timestamp",
                fdkId = fdkId,
                timestamp = timestamp,
                removed = false,
                type = ResourceType.INFORMATION_MODEL,
                orgPath = informationModel.publisher?.orgPath
            )
        )
    }

    fun storeServiceMetrics(fdkId: String, service: Service, timestamp: Long) {
        val orgPath = if (!service.hasCompetentAuthority.isNullOrEmpty()) service.hasCompetentAuthority.first().orgPath
        else if (!service.ownedBy.isNullOrEmpty()) service.ownedBy.first().orgPath
        else null

        statisticsRepository.storeMetrics(
            ResourceEventMetrics(
                id = "$fdkId-$timestamp",
                fdkId = fdkId,
                timestamp = timestamp,
                removed = false,
                type = ResourceType.SERVICE,
                orgPath = orgPath
            )
        )
    }

    fun markResourceAsRemoved(fdkId: String, timestamp: Long, resourceType: ResourceType) {
        statisticsRepository.storeMetrics(
            ResourceEventMetrics(
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
        logger.info("Starting calculation of latest metrics for period between {} and {}", req.startInclusive, req.endExclusive)
        req.validate()
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
                statisticsRepository.storeLatestForDate(
                    LatestForDate(
                        fdkId = it.value,
                        calculatedForDate = date,
                        statId = it.key
                    )
                )
            }
    }

    fun clearTimeSeriesCache() = statisticsRepository.clearTimeSeriesCache()

    fun timeSeries(req: TimeSeriesRequest): List<TimeSeriesPoint> {
        logger.debug("Building time series for request: {}", req)
        req.validate()
        return statisticsRepository.timeSeries(req)
    }

}
