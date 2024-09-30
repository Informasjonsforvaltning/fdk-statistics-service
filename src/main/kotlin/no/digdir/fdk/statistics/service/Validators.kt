package no.digdir.fdk.statistics.service

import no.digdir.fdk.statistics.model.CalculationRequest
import no.digdir.fdk.statistics.model.Interval
import no.digdir.fdk.statistics.model.ResourceType
import no.digdir.fdk.statistics.model.TimeSeriesRequest
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate

fun CalculationRequest.validate() {
    if (startInclusive.isAfter(endExclusive)) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to calculate for negative period")
    if (startInclusive.isBefore(LocalDate.of(2022, 1, 1))) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No data available before 2022")
    if (endExclusive.isAfter(LocalDate.now())) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No data available for the future")
}

fun TimeSeriesRequest.validate() {
    if (start.isISODate().not() || end.isISODate().not())
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Dates has to follow format 'yyyy-MM-dd'")

    val startDate = LocalDate.parse(start)
    val endDate = LocalDate.parse(end)
    val type = filters?.resourceType?.value

    when {
        interval == Interval.DAY && startDate.isAfter(endDate.minusDays(1)) ->
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Period has to cover minimum 1 day")

        interval == Interval.WEEK && startDate.isAfter(endDate.minusDays(7)) ->
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Period has to cover minimum 1 week")

        interval == Interval.MONTH && startDate.isAfter(endDate.minusMonths(1)) ->
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Period has to cover minimum 1 month")

        interval == Interval.DAY && startDate.isBefore(endDate.minusMonths(4)) ->
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Max coverage for period is 4 months")

        interval == Interval.WEEK && startDate.isBefore(endDate.minusYears(2)) ->
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Max coverage for period is 2 years")

        interval == Interval.MONTH && startDate.isBefore(endDate.minusYears(10)) ->
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Max coverage for period is 10 years")

        type == null && startDate.isBefore(LocalDate.of(2024, 1, 1)) ->
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Not all resources have data available before 2024")

        type == ResourceType.CONCEPT && startDate.isBefore(LocalDate.of(2023, 2, 1)) ->
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No concept data available before february 2023")

        type == ResourceType.DATA_SERVICE && startDate.isBefore(LocalDate.of(2023, 2, 1)) ->
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No data service data available before february 2023")

        type == ResourceType.DATASET && startDate.isBefore(LocalDate.of(2022, 11, 1)) ->
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No dataset data available before november 2022")

        type == ResourceType.EVENT && startDate.isBefore(LocalDate.of(2024, 1, 1)) ->
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No event data available before 2024")

        type == ResourceType.INFORMATION_MODEL && startDate.isBefore(LocalDate.of(2024, 1, 1)) ->
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No information model data available before 2024")

        type == ResourceType.SERVICE && startDate.isBefore(LocalDate.of(2024, 1, 1)) ->
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No service data available before 2024")

        endDate.isAfter(LocalDate.now()) ->
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No data available for the future")
    }
}

private fun String.isISODate(): Boolean {
    val sdf = SimpleDateFormat("yyyy-MM-dd")
    return try {
        sdf.parse(this)
        true
    } catch (ex: ParseException) {
        false
    }
}
