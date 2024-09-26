package no.digdir.fdk.statistics.kafka

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.micrometer.core.instrument.Metrics
import no.digdir.fdk.statistics.model.ResourceType
import no.digdir.fdk.statistics.service.StatisticsService
import no.fdk.concept.ConceptEvent
import no.fdk.concept.ConceptEventType
import no.fdk.dataservice.DataServiceEvent
import no.fdk.dataservice.DataServiceEventType
import no.fdk.dataset.DatasetEvent
import no.fdk.dataset.DatasetEventType
import no.fdk.event.EventEvent
import no.fdk.event.EventEventType
import no.fdk.informationmodel.InformationModelEvent
import no.fdk.informationmodel.InformationModelEventType
import no.fdk.service.ServiceEvent
import no.fdk.service.ServiceEventType
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration

@Component
open class KafkaRemovedEventCircuitBreaker(
    private val statisticsService: StatisticsService
) {
    private fun SpecificRecord.getResourceType(): String {
        return when (this) {
            is DatasetEvent -> "dataset"
            is DataServiceEvent -> "data-service"
            is ConceptEvent -> "concept"
            is InformationModelEvent -> "information-model"
            is ServiceEvent -> "service"
            is EventEvent -> "event"
            else -> "invalid-type"
        }
    }

    @CircuitBreaker(name = "remove")
    @Transactional
    open fun process(record: ConsumerRecord<String, SpecificRecord>) {
        logger.debug("Received message - offset: " + record.offset())

        val event = record.value()
        try {
            val (deleted, timeElapsed) = measureTimedValue {
                when {
                    event is ConceptEvent && event.type == ConceptEventType.CONCEPT_REMOVED -> {
                        logger.debug("Remove concept - id: {}", event.fdkId)
                        statisticsService.markResourceAsRemoved(
                            event.fdkId.toString(),
                            event.timestamp,
                            ResourceType.CONCEPT
                        )
                        true
                    }

                    event is DataServiceEvent && event.type == DataServiceEventType.DATA_SERVICE_REMOVED -> {
                        logger.debug("Remove data service - id: {}", event.fdkId)
                        statisticsService.markResourceAsRemoved(
                            event.fdkId.toString(),
                            event.timestamp,
                            ResourceType.DATA_SERVICE
                        )
                        true
                    }

                    event is DatasetEvent && event.type == DatasetEventType.DATASET_REMOVED -> {
                        logger.debug("Remove dataset - id: {}", event.fdkId)
                        statisticsService.markResourceAsRemoved(
                            event.fdkId.toString(),
                            event.timestamp,
                            ResourceType.DATASET
                        )
                        true
                    }

                    event is EventEvent && event.type == EventEventType.EVENT_REMOVED -> {
                        logger.debug("Remove event - id: {}", event.fdkId)
                        statisticsService.markResourceAsRemoved(
                            event.fdkId.toString(),
                            event.timestamp,
                            ResourceType.EVENT
                        )
                        true
                    }

                    event is InformationModelEvent && event.type == InformationModelEventType.INFORMATION_MODEL_REMOVED -> {
                        logger.debug("Remove information model - id: {}", event.fdkId)
                        statisticsService.markResourceAsRemoved(
                            event.fdkId.toString(),
                            event.timestamp,
                            ResourceType.INFORMATION_MODEL
                        )
                        true
                    }

                    event is ServiceEvent && event.type == ServiceEventType.SERVICE_REMOVED -> {
                        logger.debug("Remove service - id: {}", event.fdkId)
                        statisticsService.markResourceAsRemoved(
                            event.fdkId.toString(),
                            event.timestamp,
                            ResourceType.SERVICE
                        )
                        true
                    }

                    else -> {
                        logger.debug("Unknown event type: {}, skipping", event)
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

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(KafkaRemovedEventCircuitBreaker::class.java)
    }
}
