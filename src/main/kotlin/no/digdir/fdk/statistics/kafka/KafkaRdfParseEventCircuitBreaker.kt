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
import org.apache.avro.generic.GenericRecord
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

    private fun storeConcept(event: GenericRecord) {
        val fdkId = event.get("fdkId")?.toString() ?: return
        val data = event.get("data")?.toString() ?: return
        val timestamp = (event.get("timestamp") as? Number)?.toLong() ?: return
        logger.debug("Store concept metrics - id: $fdkId")
        statisticsService.storeConceptMetrics(
            fdkId,
            jacksonObjectMapper().readValue(data, Concept::class.java),
            timestamp
        )
    }

    private fun storeDataService(event: GenericRecord) {
        val fdkId = event.get("fdkId")?.toString() ?: return
        val data = event.get("data")?.toString() ?: return
        val timestamp = (event.get("timestamp") as? Number)?.toLong() ?: return
        logger.debug("Store data service metrics - id: $fdkId")
        statisticsService.storeDataServiceMetrics(
            fdkId,
            jacksonObjectMapper().readValue(data, DataService::class.java),
            timestamp
        )
    }

    private fun storeDataset(event: GenericRecord) {
        val fdkId = event.get("fdkId")?.toString() ?: return
        val data = event.get("data")?.toString() ?: return
        val timestamp = (event.get("timestamp") as? Number)?.toLong() ?: return
        logger.debug("Store dataset metrics - id: $fdkId")
        statisticsService.storeDatasetMetrics(
            fdkId,
            jacksonObjectMapper().readValue(data, Dataset::class.java),
            timestamp
        )
    }

    private fun storeEvent(event: GenericRecord) {
        val fdkId = event.get("fdkId")?.toString() ?: return
        val data = event.get("data")?.toString() ?: return
        val timestamp = (event.get("timestamp") as? Number)?.toLong() ?: return
        logger.debug("Store event metrics - id: $fdkId")
        statisticsService.storeEventMetrics(
            fdkId,
            jacksonObjectMapper().readValue(data, Event::class.java),
            timestamp
        )
    }

    private fun storeInformationModel(event: GenericRecord) {
        val fdkId = event.get("fdkId")?.toString() ?: return
        val data = event.get("data")?.toString() ?: return
        val timestamp = (event.get("timestamp") as? Number)?.toLong() ?: return
        logger.debug("Store information model metrics - id: $fdkId")
        statisticsService.storeInformationModelMetrics(
            fdkId,
            jacksonObjectMapper().readValue(data, InformationModel::class.java),
            timestamp
        )
    }

    private fun storeService(event: GenericRecord) {
        val fdkId = event.get("fdkId")?.toString() ?: return
        val data = event.get("data")?.toString() ?: return
        val timestamp = (event.get("timestamp") as? Number)?.toLong() ?: return
        logger.debug("Store service metrics - id: $fdkId")
        statisticsService.storeServiceMetrics(
            fdkId,
            jacksonObjectMapper().readValue(data, Service::class.java),
            timestamp
        )
    }

    @CircuitBreaker(name = "rdf-parse")
    @Transactional
    open fun process(
        record: ConsumerRecord<String, GenericRecord>
    ) {
        logger.debug("CB Received message - offset: " + record.offset())

        val event = record.value()
        val harvestRunId = event.getNullableString("harvestRunId")
        val uri = event.getNullableString("uri")
        logger.debug("Message harvestRunId={}, uri={}", harvestRunId, uri)

        val resourceType = event.get("resourceType")?.toString()?.lowercase() ?: ""

        try {
            val timeElapsed = measureTimedValue {
                when (resourceType) {
                    "concept" -> storeConcept(event)
                    "data_service" -> storeDataService(event)
                    "dataset" -> storeDataset(event)
                    "event" -> storeEvent(event)
                    "information_model" -> storeInformationModel(event)
                    "service" -> storeService(event)
                    else -> logger.debug("unknown rdf parse type")
                }
            }
            Metrics.timer("store_resource", "type", resourceType)
                .record(timeElapsed.duration.toJavaDuration())
        } catch (e: Exception) {
            logger.error("Error processing message", e)
            Metrics.counter(
                "store_resource_error",
                "type", resourceType
            ).increment()
            throw e
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(KafkaRdfParseEventCircuitBreaker::class.java)
    }
}
