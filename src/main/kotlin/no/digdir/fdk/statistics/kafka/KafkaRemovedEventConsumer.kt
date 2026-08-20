package no.digdir.fdk.statistics.kafka

import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class KafkaRemovedEventConsumer(private val kafkaRemovedEventCircuitBreaker: KafkaRemovedEventCircuitBreaker) {
    @KafkaListener(
        topics = [
            "dataset-events",
            "data-service-events",
            "concept-events",
            "information-model-events",
            "event-events",
            "service-events",
        ],
        groupId = "fdk-statistics-service",
        concurrency = "4",
        containerFactory = "kafkaListenerContainerFactory",
        id = "remove",
    )
    fun listen(record: ConsumerRecord<String, GenericRecord>, ack: Acknowledgment) =
        ack.acknowledgeOrNack { kafkaRemovedEventCircuitBreaker.process(record) }
}
