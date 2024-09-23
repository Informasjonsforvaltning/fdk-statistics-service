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
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonDeserialize(using = LocalDateDeserializer::class)
    val start: LocalDate = LocalDate.of(2023, 1, 1),
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonDeserialize(using = LocalDateDeserializer::class)
    val end: LocalDate = LocalDate.now(),
    val interval: Interval = Interval.MONTH
)
