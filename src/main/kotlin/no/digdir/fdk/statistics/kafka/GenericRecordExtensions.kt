package no.digdir.fdk.statistics.kafka

import org.apache.avro.generic.GenericRecord
import org.springframework.kafka.support.Acknowledgment
import java.time.Duration

fun GenericRecord.getNullableString(field: String): String? =
    runCatching { get(field) as? String }.getOrNull()

fun Acknowledgment.acknowledgeOrNack(process: () -> Unit) {
    try {
        process()
        acknowledge()
    } catch (e: Exception) {
        nack(Duration.ZERO)
    }
}
