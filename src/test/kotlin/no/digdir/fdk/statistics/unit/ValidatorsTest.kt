package no.digdir.fdk.statistics.unit

import no.digdir.fdk.statistics.model.CalculationRequest
import no.digdir.fdk.statistics.model.Interval
import no.digdir.fdk.statistics.model.ResourceType
import no.digdir.fdk.statistics.model.SearchFilter
import no.digdir.fdk.statistics.model.TimeSeriesFilters
import no.digdir.fdk.statistics.service.validate
import no.digdir.fdk.statistics.utils.TIME_SERIES_REQUEST
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

@Tag("unit")
class ValidatorsTest {

    @Nested
    inner class ValidateCalculationRequest {
        private val okReq = CalculationRequest(
            startInclusive = LocalDate.of(2024, 1, 1),
            endExclusive = LocalDate.of(2024, 2, 1)
        )

        @Test
        fun validWhenCalculatingOneMonth() {
            assertDoesNotThrow { okReq.validate() }
        }

        @Test
        fun notValidWhenEndIsBeforeStart() {
            assertThrows<ResponseStatusException> {
                okReq.copy(endExclusive = LocalDate.of(2023, 12, 1)).validate()
            }
        }

        @Test
        fun notValidWhenStartIsBefore2022() {
            assertThrows<ResponseStatusException> {
                okReq.copy(startInclusive = LocalDate.of(2021, 12, 1)).validate()
            }
        }

        @Test
        fun notValidWhenEndIsAfterToday() {
            assertThrows<ResponseStatusException> {
                okReq.copy(endExclusive = LocalDate.now().plusDays(1)).validate()
            }
        }
    }

    @Nested
    inner class ValidateTimeSeriesRequest {

        @Test
        fun notValidWhenDatesAreOtherFormats() {
            assertThrows<ResponseStatusException> {
                TIME_SERIES_REQUEST.copy(start = "01/21/2024").validate()
            }
            assertThrows<ResponseStatusException> {
                TIME_SERIES_REQUEST.copy(end = "01.08.2024").validate()
            }
        }

        @Test
        fun notValidWhenCoveredPeriodIsSmallerThanTheInterval() {
            assertThrows<ResponseStatusException> {
                TIME_SERIES_REQUEST.copy(end = "2024-02-01").validate()
            }
            assertThrows<ResponseStatusException> {
                TIME_SERIES_REQUEST.copy(end = "2024-01-15", interval = Interval.WEEK).validate()
            }
            assertThrows<ResponseStatusException> {
                TIME_SERIES_REQUEST.copy(end = "2024-01-11", interval = Interval.DAY).validate()
            }
        }

        @Test
        fun notValidWhenExceedingMaxCoveragePeriod() {
            assertThrows<ResponseStatusException> {
                TIME_SERIES_REQUEST.copy(end = "2044-01-01").validate()
            }
            assertThrows<ResponseStatusException> {
                TIME_SERIES_REQUEST.copy(end = "2027-01-11", interval = Interval.WEEK).validate()
            }
            assertThrows<ResponseStatusException> {
                TIME_SERIES_REQUEST.copy(end = "2024-07-30", interval = Interval.DAY).validate()
            }
        }

        @Test
        fun notValidWhenStartDateIsBeforeFirstDataInput() {
            val filters = TimeSeriesFilters(resourceType = null, orgPath = null, transport = null)
            assertThrows<ResponseStatusException> {
                TIME_SERIES_REQUEST.copy(start = "2023-12-01", end = "2024-02-01").validate()
            }
            assertThrows<ResponseStatusException> {
                TIME_SERIES_REQUEST.copy(
                    start = "2023-01-01",
                    end = "2023-04-01",
                    filters = filters.copy(resourceType = SearchFilter(ResourceType.CONCEPT))
                ).validate()
            }
            assertThrows<ResponseStatusException> {
                TIME_SERIES_REQUEST.copy(
                    start = "2023-01-01",
                    end = "2023-04-01",
                    filters = filters.copy(resourceType = SearchFilter(ResourceType.DATA_SERVICE))
                ).validate()
            }
            assertThrows<ResponseStatusException> {
                TIME_SERIES_REQUEST.copy(
                    start = "2022-10-01",
                    end = "2022-12-01",
                    filters = filters.copy(resourceType = SearchFilter(ResourceType.DATASET))
                ).validate()
            }
            assertThrows<ResponseStatusException> {
                TIME_SERIES_REQUEST.copy(
                    start = "2023-12-01",
                    end = "2024-02-01",
                    filters = filters.copy(resourceType = SearchFilter(ResourceType.EVENT))
                ).validate()
            }
            assertThrows<ResponseStatusException> {
                TIME_SERIES_REQUEST.copy(
                    start = "2023-12-01",
                    end = "2024-02-01",
                    filters = filters.copy(resourceType = SearchFilter(ResourceType.INFORMATION_MODEL))
                ).validate()
            }
            assertThrows<ResponseStatusException> {
                TIME_SERIES_REQUEST.copy(
                    start = "2023-12-01",
                    end = "2024-02-01",
                    filters = filters.copy(resourceType = SearchFilter(ResourceType.SERVICE))
                ).validate()
            }
        }

        @Test
        fun notValidWhenEndDateIsAfterToday() {
            assertThrows<ResponseStatusException> {
                TIME_SERIES_REQUEST.copy(end = LocalDate.now().plusDays(1).toString()).validate()
            }
        }
    }
}
