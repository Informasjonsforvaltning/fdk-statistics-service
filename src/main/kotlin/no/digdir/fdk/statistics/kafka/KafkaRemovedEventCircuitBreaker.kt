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
    private fun GenericRecord.getResourceType(): String =
        when (schema?.fullName) {
            "no.fdk.dataset.DatasetEvent" -> "dataset"
            "no.fdk.dataservice.DataServiceEvent" -> "data-service"
            "no.fdk.concept.ConceptEvent" -> "concept"
            "no.fdk.informationmodel.InformationModelEvent" -> "information-model"
            "no.fdk.service.ServiceEvent" -> "service"
            "no.fdk.event.EventEvent" -> "event"
            else -> "invalid-type"
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
                val (deleted, timeElapsed) =
                    measureTimedValue {
                        val eventType = event.get("type")?.toString() ?: ""
                        val fdkId = event.get("fdkId")?.toString() ?: return@measureTimedValue false
                        val timestamp = (event.get("timestamp") as? Number)?.toLong() ?: return@measureTimedValue false

                        val resourceType = REMOVED_EVENT_TYPES[event.getResourceType() to eventType]
                        if (resourceType != null) {
                            logger.debug("Remove {} - id: {}", resourceType, fdkId)
                            statisticsService.markResourceAsRemoved(fdkId, timestamp, resourceType)
                            true
                        } else {
                            logger.debug("Unknown event type: {} / {}, skipping", event.getResourceType(), eventType)
                            false
                        }
                    }

                if (deleted) {
                    Metrics
                        .timer("resource_delete", "type", event.getResourceType())
                        .record(timeElapsed.toJavaDuration())
                }
            } catch (e: Exception) {
                logger.error("Error processing message", e)
                Metrics
                    .counter(
                        "resource_delete_error",
                        "type",
                        event.getResourceType(),
                    ).increment()
                throw e
            }
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(KafkaRemovedEventCircuitBreaker::class.java)

        // Maps (resource type, avro event type) pairs to the corresponding ResourceType
        private val REMOVED_EVENT_TYPES: Map<Pair<String, String>, ResourceType> =
            mapOf(
                ("concept" to "CONCEPT_REMOVED") to ResourceType.CONCEPT,
                ("data-service" to "DATA_SERVICE_REMOVED") to ResourceType.DATA_SERVICE,
                ("dataset" to "DATASET_REMOVED") to ResourceType.DATASET,
                ("event" to "EVENT_REMOVED") to ResourceType.EVENT,
                ("information-model" to "INFORMATION_MODEL_REMOVED") to ResourceType.INFORMATION_MODEL,
                ("service" to "SERVICE_REMOVED") to ResourceType.SERVICE,
            )
    }
}
