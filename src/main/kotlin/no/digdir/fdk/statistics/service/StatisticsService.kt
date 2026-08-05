package no.digdir.fdk.statistics.service

import no.digdir.fdk.statistics.model.CalculationRequest
import no.digdir.fdk.statistics.model.Concept
import no.digdir.fdk.statistics.model.DataService
import no.digdir.fdk.statistics.model.Dataset
import no.digdir.fdk.statistics.model.Event
import no.digdir.fdk.statistics.model.InformationModel
import no.digdir.fdk.statistics.model.Interval
import no.digdir.fdk.statistics.model.LatestForDate
import no.digdir.fdk.statistics.model.ResourceEventMetrics
import no.digdir.fdk.statistics.model.ResourceType
import no.digdir.fdk.statistics.model.SearchFilter
import no.digdir.fdk.statistics.model.Service
import no.digdir.fdk.statistics.model.TimeSeriesFilters
import no.digdir.fdk.statistics.model.TimeSeriesPoint
import no.digdir.fdk.statistics.model.TimeSeriesRequest
import no.digdir.fdk.statistics.repository.StatisticsRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.cache.CacheManager
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneOffset

@Component
class StatisticsService(
    private val statisticsRepository: StatisticsRepository,
    private val cacheManager: CacheManager,
) {
    private val logger: Logger = LoggerFactory.getLogger(StatisticsService::class.java)

    fun clearTimeSeriesCache() {
        cacheManager
            .getCache("time_series_cache")
            ?.invalidate()
    }

    private fun storeMetrics(
        fdkId: String,
        timestamp: Long,
        type: ResourceType,
        orgPath: String?,
        removed: Boolean = false,
        isRelatedToTransportportal: Boolean = false,
    ) {
        statisticsRepository.storeMetrics(
            ResourceEventMetrics(
                id = "$fdkId-$timestamp",
                fdkId = fdkId,
                timestamp = timestamp,
                removed = removed,
                type = type,
                orgPath = orgPath,
                isRelatedToTransportportal = isRelatedToTransportportal,
            ),
        )
    }

    fun storeConceptMetrics(
        fdkId: String,
        concept: Concept,
        timestamp: Long,
    ) = storeMetrics(fdkId, timestamp, ResourceType.CONCEPT, concept.publisher?.orgPath)

    fun storeDataServiceMetrics(
        fdkId: String,
        dataService: DataService,
        timestamp: Long,
    ) = storeMetrics(fdkId, timestamp, ResourceType.DATA_SERVICE, dataService.publisher?.orgPath)

    fun storeDatasetMetrics(
        fdkId: String,
        dataset: Dataset,
        timestamp: Long,
    ) = storeMetrics(
        fdkId,
        timestamp,
        ResourceType.DATASET,
        dataset.publisher?.orgPath,
        isRelatedToTransportportal = dataset.isRelatedToTransportportal ?: false,
    )

    fun storeEventMetrics(
        fdkId: String,
        event: Event,
        timestamp: Long,
    ) = storeMetrics(fdkId, timestamp, ResourceType.EVENT, event.catalog?.publisher?.orgPath)

    fun storeInformationModelMetrics(
        fdkId: String,
        informationModel: InformationModel,
        timestamp: Long,
    ) = storeMetrics(fdkId, timestamp, ResourceType.INFORMATION_MODEL, informationModel.publisher?.orgPath)

    fun storeServiceMetrics(
        fdkId: String,
        service: Service,
        timestamp: Long,
    ) = storeMetrics(fdkId, timestamp, ResourceType.SERVICE, service.orgPath())

    private fun Service.orgPath(): String? =
        when {
            !hasCompetentAuthority.isNullOrEmpty() -> hasCompetentAuthority.first().orgPath
            !ownedBy.isNullOrEmpty() -> ownedBy.first().orgPath
            else -> null
        }

    fun markResourceAsRemoved(
        fdkId: String,
        timestamp: Long,
        resourceType: ResourceType,
    ) = storeMetrics(fdkId, timestamp, resourceType, orgPath = null, removed = true)

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
        statisticsRepository
            .latestForTimestamp(date.toMillis())
            .forEach {
                statisticsRepository.storeLatestForDate(
                    LatestForDate(
                        fdkId = it.value,
                        calculatedForDate = date,
                        statId = it.key,
                    ),
                )
            }
    }

    fun timeSeries(req: TimeSeriesRequest): List<TimeSeriesPoint> {
        logger.debug("Building time series for request: {}", req)
        req.validate()
        return statisticsRepository.timeSeries(req)
    }

    /**
     * Run on startup
     */
    @EventListener
    private fun initCache(event: ApplicationReadyEvent) = cacheDefaultRequests()

    fun cacheDefaultRequests() {
        logger.info("Cache default requests")

        val firstOfThisMonth = LocalDate.now().withDayOfMonth(1).toString()
        val typesToWarmUp =
            listOf(
                ResourceType.CONCEPT,
                ResourceType.DATA_SERVICE,
                ResourceType.DATASET,
                ResourceType.INFORMATION_MODEL,
            )

        typesToWarmUp.forEach { type ->
            val req =
                TimeSeriesRequest(
                    start = type.availableFrom.toString(),
                    end = firstOfThisMonth,
                    interval = Interval.MONTH,
                    filters =
                        TimeSeriesFilters(
                            resourceType = SearchFilter(value = type),
                            orgPath = null,
                            transport = null,
                        ),
                )

            statisticsRepository.timeSeries(req)
            statisticsRepository.timeSeries(req.addTransportFilter())
        }
    }

    private fun TimeSeriesRequest.addTransportFilter() =
        copy(
            filters =
                filters?.copy(
                    transport = SearchFilter(value = true),
                ),
        )
}
