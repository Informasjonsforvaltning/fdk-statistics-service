package no.digdir.fdk.statistics.service

import no.digdir.fdk.statistics.model.CalculationRequest
import no.digdir.fdk.statistics.model.Interval
import no.digdir.fdk.statistics.model.ResourceType
import no.digdir.fdk.statistics.model.TimeSeriesRequest
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun CalculationRequest.validate() {
    if (startInclusive.isAfter(
            endExclusive,
        )
    ) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to calculate for negative period")
    }
    if (startInclusive.isBefore(
            LocalDate.of(2022, 1, 1),
        )
    ) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No data available before 2022")
    }
    if (endExclusive.isAfter(LocalDate.now())) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No data available for the future")
}

fun TimeSeriesRequest.validate() {
    if (start.isISODate().not() || end.isISODate().not()) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Dates has to follow format 'yyyy-MM-dd'")
    }

    val startDate = LocalDate.parse(start)
    val endDate = LocalDate.parse(end)
    val type = filters?.resourceType?.value

    when {
        startDate.isAfter(endDate.minusDays(1)) -> {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Period has to cover minimum 1 day")
        }

        interval == Interval.WEEK && listOf(startDate, endDate).any { it.dayOfWeek != DayOfWeek.MONDAY } -> {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Period has start and end on a monday")
        }

        interval == Interval.MONTH && listOf(startDate, endDate).any { it.dayOfMonth != 1 } -> {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Period has to start and end on the first day of a month")
        }

        interval == Interval.DAY && startDate.isBefore(endDate.minusMonths(4)) -> {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Max coverage for period is 4 months")
        }

        interval == Interval.WEEK && startDate.isBefore(endDate.minusYears(2)) -> {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Max coverage for period is 2 years")
        }

        interval == Interval.MONTH && startDate.isBefore(endDate.minusYears(10)) -> {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Max coverage for period is 10 years")
        }

        type == null && startDate.isBefore(ResourceType.earliestCommonDate) -> {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Not all resources have data available before ${ResourceType.earliestCommonDate}",
            )
        }

        type != null && startDate.isBefore(type.availableFrom) -> {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No ${type.readableName()} data available before ${type.availableFrom}")
        }

        endDate.isAfter(LocalDate.now()) -> {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No data available for the future")
        }
    }
}

private fun ResourceType.readableName(): String = name.lowercase().replace('_', ' ')

private fun String.isISODate(): Boolean = try {
    DateTimeFormatter.ISO_LOCAL_DATE.parse(this)
    true
} catch (ex: DateTimeParseException) {
    false
}
