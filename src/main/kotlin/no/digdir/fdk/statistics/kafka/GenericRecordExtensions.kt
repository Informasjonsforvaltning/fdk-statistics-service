package no.digdir.fdk.statistics.kafka

import org.apache.avro.generic.GenericRecord

fun GenericRecord.getNullableString(field: String): String? =
    runCatching { get(field) as? String }.getOrNull()
