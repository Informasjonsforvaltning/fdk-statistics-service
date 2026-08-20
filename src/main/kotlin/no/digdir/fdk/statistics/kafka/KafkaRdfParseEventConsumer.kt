package no.digdir.fdk.statistics.kafka

import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class KafkaRdfParseEventConsumer(private val kafkaRdfParseEventCircuitBreaker: KafkaRdfParseEventCircuitBreaker) {
    @KafkaListener(
        topics = ["rdf-parse-events"],
        groupId = "fdk-statistics-service",
        containerFactory = "kafkaListenerContainerFactory",
        concurrency = "4",
        id = "rdf-parse",
    )
    fun listen(record: ConsumerRecord<String, GenericRecord>, ack: Acknowledgment) =
        ack.acknowledgeOrNack { kafkaRdfParseEventCircuitBreaker.process(record) }
}
