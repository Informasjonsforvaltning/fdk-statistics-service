package no.digdir.fdk.statistics.service

import no.digdir.fdk.statistics.model.Concept
import no.digdir.fdk.statistics.model.DataService
import no.digdir.fdk.statistics.model.Dataset
import no.digdir.fdk.statistics.model.Event
import no.digdir.fdk.statistics.model.InformationModel
import no.digdir.fdk.statistics.model.LatestForDate
import no.digdir.fdk.statistics.model.CalculationRequest
import no.digdir.fdk.statistics.model.Interval
import no.digdir.fdk.statistics.model.ResourceType
import no.digdir.fdk.statistics.model.Service
import no.digdir.fdk.statistics.model.ResourceEventMetrics
import no.digdir.fdk.statistics.model.SearchFilter
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
    private val cacheManager: CacheManager
) {
    private val logger: Logger = LoggerFactory.getLogger(StatisticsService::class.java)

    fun clearTimeSeriesCache() {
        cacheManager.getCache("time_series_cache")
            ?.invalidate()
    }

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

    fun timeSeries(req: TimeSeriesRequest): List<TimeSeriesPoint> {
        logger.debug("Building time series for request: {}", req)
        req.validate()
        return statisticsRepository.timeSeries(req)
    }

    /**
     * Run on startup
     */
    @EventListener
    private fun initCache(event: ApplicationReadyEvent) =
        cacheDefaultRequests()

    fun cacheDefaultRequests() {
        logger.info("Cache default requests")

        val firstOfThisMonth = LocalDate.now().withDayOfMonth(1).toString()
        val conceptStart = "2023-02-01"
        val dataServiceStart = "2023-02-01"
        val datasetStart = "2022-11-01"
        val infoModelStart = "2024-01-01"

        val conceptReq = TimeSeriesRequest(
            start = conceptStart,
            end = firstOfThisMonth,
            interval = Interval.MONTH,
            filters = TimeSeriesFilters(
                resourceType = SearchFilter(value = ResourceType.CONCEPT),
                orgPath = null,
                transport = null
            )
        )

        statisticsRepository.timeSeries(conceptReq)
        statisticsRepository.timeSeries(conceptReq.addTransportFilter())

        val dataServiceReq = TimeSeriesRequest(
            start = dataServiceStart,
            end = firstOfThisMonth,
            interval = Interval.MONTH,
            filters = TimeSeriesFilters(
                resourceType = SearchFilter(value = ResourceType.DATA_SERVICE),
                orgPath = null,
                transport = null
            )
        )

        statisticsRepository.timeSeries(dataServiceReq)
        statisticsRepository.timeSeries(dataServiceReq.addTransportFilter())

        val datasetReq = TimeSeriesRequest(
            start = datasetStart,
            end = firstOfThisMonth,
            interval = Interval.MONTH,
            filters = TimeSeriesFilters(
                resourceType = SearchFilter(value = ResourceType.DATASET),
                orgPath = null,
                transport = null
            )
        )

        statisticsRepository.timeSeries(datasetReq)
        statisticsRepository.timeSeries(datasetReq.addTransportFilter())

        val infoModelReq = TimeSeriesRequest(
            start = infoModelStart,
            end = firstOfThisMonth,
            interval = Interval.MONTH,
            filters = TimeSeriesFilters(
                resourceType = SearchFilter(value = ResourceType.INFORMATION_MODEL),
                orgPath = null,
                transport = null
            )
        )

        statisticsRepository.timeSeries(infoModelReq)
        statisticsRepository.timeSeries(infoModelReq.addTransportFilter())
    }

    private fun TimeSeriesRequest.addTransportFilter() =
        copy(
            filters = filters?.copy(
                transport = SearchFilter(value = true)
            )
        )

}
