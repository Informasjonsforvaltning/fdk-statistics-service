package no.digdir.fdk.statistics.kafka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.micrometer.core.instrument.Metrics
import no.digdir.fdk.statistics.model.Concept
import no.digdir.fdk.statistics.model.DataService
import no.digdir.fdk.statistics.model.Dataset
import no.digdir.fdk.statistics.model.Event
import no.digdir.fdk.statistics.model.InformationModel
import no.digdir.fdk.statistics.model.Service
import no.digdir.fdk.statistics.service.StatisticsService
import no.fdk.rdf.parse.RdfParseEvent
import no.fdk.rdf.parse.RdfParseResourceType
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration

@Component
open class KafkaRdfParseEventCircuitBreaker(
    private val statisticsService: StatisticsService
) {

    private fun storeConcept(event: RdfParseEvent) {
        logger.debug("Store concept metrics - id: " + event.fdkId)
        statisticsService.storeConceptMetrics(
            event.fdkId.toString(),
            jacksonObjectMapper().readValue(event.data.toString(), Concept::class.java),
            event.timestamp
        )
    }

    private fun storeDataService(event: RdfParseEvent) {
        logger.debug("Store data service metrics - id: " + event.fdkId)
        statisticsService.storeDataServiceMetrics(
            event.fdkId.toString(),
            jacksonObjectMapper().readValue(event.data.toString(), DataService::class.java),
            event.timestamp
        )
    }

    private fun storeDataset(event: RdfParseEvent) {
        logger.debug("Store dataset metrics - id: " + event.fdkId)
        statisticsService.storeDatasetMetrics(
            event.fdkId.toString(),
            jacksonObjectMapper().readValue(event.data.toString(), Dataset::class.java),
            event.timestamp
        )
    }

    private fun storeEvent(event: RdfParseEvent) {
        logger.debug("Store event metrics - id: " + event.fdkId)
        statisticsService.storeEventMetrics(
            event.fdkId.toString(),
            jacksonObjectMapper().readValue(event.data.toString(), Event::class.java),
            event.timestamp
        )
    }

    private fun storeInformationModel(event: RdfParseEvent) {
        logger.debug("Store information model metrics - id: " + event.fdkId)
        statisticsService.storeInformationModelMetrics(
            event.fdkId.toString(),
            jacksonObjectMapper().readValue(event.data.toString(), InformationModel::class.java),
            event.timestamp
        )
    }

    private fun storeService(event: RdfParseEvent) {
        logger.debug("Store service metrics - id: " + event.fdkId)
        statisticsService.storeServiceMetrics(
            event.fdkId.toString(),
            jacksonObjectMapper().readValue(event.data.toString(), Service::class.java),
            event.timestamp
        )
    }

    @CircuitBreaker(name = "rdf-parse")
    @Transactional
    open fun process(
        record: ConsumerRecord<String, RdfParseEvent>
    ) {
        logger.debug("CB Received message - offset: " + record.offset())

        val event = record.value()
        try {
            val timeElapsed = measureTimedValue {
                when (event?.resourceType) {
                    RdfParseResourceType.CONCEPT -> storeConcept(event)
                    RdfParseResourceType.DATA_SERVICE -> storeDataService(event)
                    RdfParseResourceType.DATASET -> storeDataset(event)
                    RdfParseResourceType.EVENT -> storeEvent(event)
                    RdfParseResourceType.INFORMATION_MODEL -> storeInformationModel(event)
                    RdfParseResourceType.SERVICE -> storeService(event)
                    else -> logger.debug("unknown rdf parse type")
                }
            }
            Metrics.timer("store_resource", "type", event.resourceType.name.lowercase())
                .record(timeElapsed.duration.toJavaDuration())
        } catch (e: Exception) {
            logger.error("Error processing message", e)
            Metrics.counter(
                "store_resource_error",
                "type", event.resourceType.name.lowercase()
            ).increment()
            throw e
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(KafkaRdfParseEventCircuitBreaker::class.java)
    }
}
