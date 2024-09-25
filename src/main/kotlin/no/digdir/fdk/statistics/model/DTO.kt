package no.digdir.fdk.statistics.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import java.time.LocalDate

enum class Interval {
    DAY, WEEK, MONTH;
}

data class TimeSeriesRequest(
    val start: String = LocalDate.now().minusYears(1).toString(),
    val end: String = LocalDate.now().toString(),
    val interval: Interval = Interval.MONTH
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
