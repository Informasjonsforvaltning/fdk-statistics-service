package no.digdir.fdk.statistics.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import java.time.LocalDate

enum class Interval {
    DAY, WEEK, MONTH;
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TimeSeriesRequest(
    val start: String,
    val end: String,
    val interval: Interval,
    val filters: TimeSeriesFilters?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TimeSeriesFilters(
    val resourceType: SearchFilter<ResourceType>?,
    val orgPath: SearchFilter<String>?,
    val transport: SearchFilter<Boolean>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchFilter<T>(
    val value: T
)

data class CalculationRequest(
    @param:JsonSerialize(using = LocalDateSerializer::class)
    @param:JsonDeserialize(using = LocalDateDeserializer::class)
    val startInclusive: LocalDate,
    @param:JsonSerialize(using = LocalDateSerializer::class)
    @param:JsonDeserialize(using = LocalDateDeserializer::class)
    val endExclusive: LocalDate
)

data class TimeSeriesPoint(
    @param:JsonSerialize(using = LocalDateSerializer::class)
    @param:JsonDeserialize(using = LocalDateDeserializer::class)
    val date: LocalDate,
    val count: Int,
)
