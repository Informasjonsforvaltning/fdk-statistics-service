package no.digdir.fdk.statistics.kafka

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.micrometer.core.instrument.Metrics
import no.digdir.fdk.statistics.model.ResourceType
import no.digdir.fdk.statistics.service.StatisticsService
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration

@Component
open class KafkaRemovedEventCircuitBreaker(
    private val statisticsService: StatisticsService,
    @param:Qualifier("removeCircuitBreaker")
    private val circuitBreaker: CircuitBreaker,
) {
    private fun GenericRecord.getResourceType(): String {
        return when (schema?.fullName) {
            "no.fdk.dataset.DatasetEvent" -> "dataset"
            "no.fdk.dataservice.DataServiceEvent" -> "data-service"
            "no.fdk.concept.ConceptEvent" -> "concept"
            "no.fdk.informationmodel.InformationModelEvent" -> "information-model"
            "no.fdk.service.ServiceEvent" -> "service"
            "no.fdk.event.EventEvent" -> "event"
            else -> "invalid-type"
        }
    }

    @Transactional
    open fun process(record: ConsumerRecord<String, GenericRecord>) {
        circuitBreaker.executeRunnable {
            logger.debug("Received message - offset: " + record.offset())

            val event = record.value()
            val harvestRunId = event.getNullableString("harvestRunId")
            val uri = event.getNullableString("uri")
            logger.debug("Message harvestRunId={}, uri={}", harvestRunId, uri)

            try {
                val (deleted, timeElapsed) = measureTimedValue {
                    val eventType = event.get("type")?.toString() ?: ""
                    val fdkId = event.get("fdkId")?.toString() ?: return@measureTimedValue false
                    val timestamp = (event.get("timestamp") as? Number)?.toLong() ?: return@measureTimedValue false

                    when (event.getResourceType() to eventType) {
                        "concept" to "CONCEPT_REMOVED" -> {
                            logger.debug("Remove concept - id: {}", fdkId)
                            statisticsService.markResourceAsRemoved(fdkId, timestamp, ResourceType.CONCEPT)
                            true
                        }
                        "data-service" to "DATA_SERVICE_REMOVED" -> {
                            logger.debug("Remove data service - id: {}", fdkId)
                            statisticsService.markResourceAsRemoved(fdkId, timestamp, ResourceType.DATA_SERVICE)
                            true
                        }
                        "dataset" to "DATASET_REMOVED" -> {
                            logger.debug("Remove dataset - id: {}", fdkId)
                            statisticsService.markResourceAsRemoved(fdkId, timestamp, ResourceType.DATASET)
                            true
                        }
                        "event" to "EVENT_REMOVED" -> {
                            logger.debug("Remove event - id: {}", fdkId)
                            statisticsService.markResourceAsRemoved(fdkId, timestamp, ResourceType.EVENT)
                            true
                        }
                        "information-model" to "INFORMATION_MODEL_REMOVED" -> {
                            logger.debug("Remove information model - id: {}", fdkId)
                            statisticsService.markResourceAsRemoved(fdkId, timestamp, ResourceType.INFORMATION_MODEL)
                            true
                        }
                        "service" to "SERVICE_REMOVED" -> {
                            logger.debug("Remove service - id: {}", fdkId)
                            statisticsService.markResourceAsRemoved(fdkId, timestamp, ResourceType.SERVICE)
                            true
                        }
                        else -> {
                            logger.debug("Unknown event type: {} / {}, skipping", event.getResourceType(), eventType)
                            false
                        }
                    }
                }

                if (deleted) {
                    Metrics.timer("resource_delete", "type", event.getResourceType())
                        .record(timeElapsed.toJavaDuration())
                }
            } catch (e: Exception) {
                logger.error("Error processing message", e)
                Metrics.counter(
                    "resource_delete_error",
                    "type", event.getResourceType()
                ).increment()
                throw e
            }
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(KafkaRemovedEventCircuitBreaker::class.java)
    }
}
