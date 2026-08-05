package no.digdir.fdk.statistics.kafka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.micrometer.core.instrument.Metrics
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
open class KafkaRdfParseEventCircuitBreaker(
    private val statisticsService: StatisticsService,
    @param:Qualifier("rdfParseCircuitBreaker")
    private val circuitBreaker: CircuitBreaker,
) {
    private val mapper = jacksonObjectMapper()

    private inline fun <reified T> GenericRecord.parseAndStore(store: (String, T, Long) -> Unit) {
        val fdkId = get("fdkId")?.toString() ?: return
        val data = get("data")?.toString() ?: return
        val timestamp = (get("timestamp") as? Number)?.toLong() ?: return
        logger.debug("Store {} metrics - id: {}", T::class.simpleName, fdkId)
        store(fdkId, mapper.readValue(data, T::class.java), timestamp)
    }

    @Transactional
    open fun process(record: ConsumerRecord<String, GenericRecord>) {
        circuitBreaker.executeRunnable {
            logger.debug("CB Received message - offset: " + record.offset())

            val event = record.value()
            val harvestRunId = event.getNullableString("harvestRunId")
            val uri = event.getNullableString("uri")
            logger.debug("Message harvestRunId={}, uri={}", harvestRunId, uri)

            val resourceType = event.get("resourceType")?.toString()?.lowercase() ?: ""

            try {
                val timeElapsed =
                    measureTimedValue {
                        when (resourceType) {
                            "concept" -> event.parseAndStore(statisticsService::storeConceptMetrics)
                            "data_service" -> event.parseAndStore(statisticsService::storeDataServiceMetrics)
                            "dataset" -> event.parseAndStore(statisticsService::storeDatasetMetrics)
                            "event" -> event.parseAndStore(statisticsService::storeEventMetrics)
                            "information_model" -> event.parseAndStore(statisticsService::storeInformationModelMetrics)
                            "service" -> event.parseAndStore(statisticsService::storeServiceMetrics)
                            else -> logger.debug("unknown rdf parse type")
                        }
                    }
                Metrics
                    .timer("store_resource", "type", resourceType)
                    .record(timeElapsed.duration.toJavaDuration())
            } catch (e: Exception) {
                logger.error("Error processing message", e)
                Metrics
                    .counter(
                        "store_resource_error",
                        "type",
                        resourceType,
                    ).increment()
                throw e
            }
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(KafkaRdfParseEventCircuitBreaker::class.java)
    }
}
