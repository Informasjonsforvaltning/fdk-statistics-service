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
    val start: String = LocalDate.now().minusYears(1).toString(),
    val end: String = LocalDate.now().toString(),
    val interval: Interval = Interval.MONTH,
    val filters: TimeSeriesFilters? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TimeSeriesFilters(
    val resourceType: SearchFilter<ResourceType>?,
    val orgPath: SearchFilter<String>?,
    val transport: SearchFilter<Boolean>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
class SearchFilter<T>(
    val value: T
)

data class CalculationRequest(
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonDeserialize(using = LocalDateDeserializer::class)
    val startInclusive: LocalDate,
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonDeserialize(using = LocalDateDeserializer::class)
    val endExclusive: LocalDate
)

data class TimeSeriesPoint(
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonDeserialize(using = LocalDateDeserializer::class)
    val date: LocalDate,
    val count: Int,
)
