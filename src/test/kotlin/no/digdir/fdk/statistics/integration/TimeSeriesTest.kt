package no.digdir.fdk.statistics.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.digdir.fdk.statistics.model.Interval
import no.digdir.fdk.statistics.model.ResourceType
import no.digdir.fdk.statistics.model.SearchFilter
import no.digdir.fdk.statistics.model.TimeSeriesFilters
import no.digdir.fdk.statistics.model.TimeSeriesPoint
import no.digdir.fdk.statistics.utils.ApiTestContext
import no.digdir.fdk.statistics.utils.TIME_SERIES_REQUEST
import no.digdir.fdk.statistics.utils.requestApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("integration")
class TimeSeriesTest : ApiTestContext() {
    private val mapper = jacksonObjectMapper()

    @Test
    fun getTimeSeriesPeriodMonth() {
        val response = requestApi("/time-series", port, null, mapper.writeValueAsString(TIME_SERIES_REQUEST), POST)
        assertEquals(HttpStatus.OK.value(), response["status"])

        val expected = listOf(
            TimeSeriesPoint(date = LocalDate.of(2024, 2, 1), count = 2),
            TimeSeriesPoint(date = LocalDate.of(2024, 3, 1), count = 4),
            TimeSeriesPoint(date = LocalDate.of(2024, 4, 1), count = 6),
            TimeSeriesPoint(date = LocalDate.of(2024, 5, 1), count = 8),
            TimeSeriesPoint(date = LocalDate.of(2024, 6, 1), count = 10),
            TimeSeriesPoint(date = LocalDate.of(2024, 7, 1), count = 12),
        )
        val result: List<TimeSeriesPoint> = mapper.readValue(response["body"] as String)

        assertEquals(expected, result)
    }

    @Test
    fun getTimeSeriesPeriodWeek() {
        val response = requestApi(
            "/time-series",
            port,
            null,
            mapper.writeValueAsString(TIME_SERIES_REQUEST.copy(interval = Interval.WEEK, end = "2024-05-06")),
            POST
        )
        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: List<TimeSeriesPoint> = mapper.readValue(response["body"] as String)
        assertEquals(17, result.size)
    }

    @Test
    fun getTimeSeriesPeriodDay() {
        val response = requestApi(
            "/time-series",
            port,
            null,
            mapper.writeValueAsString(TIME_SERIES_REQUEST.copy(interval = Interval.DAY, start = "2024-05-01")),
            POST
        )
        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: List<TimeSeriesPoint> = mapper.readValue(response["body"] as String)
        assertEquals(92, result.size)
    }

    @Test
    fun getTimeSeriesBadRequestWhenStartIsAfterEnd() {
        val response = requestApi(
            "/time-series",
            port,
            null,
            mapper.writeValueAsString(TIME_SERIES_REQUEST.copy(end = "2010-01-01")),
            POST
        )
        assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])
    }

    @Nested
    inner class ResourceTypeFilter {

        @Test
        fun getConceptTimeSeries() {
            val response = requestApi(
                "/time-series",
                port,
                null,
                mapper.writeValueAsString(
                    TIME_SERIES_REQUEST.copy(
                        filters = TimeSeriesFilters(
                            resourceType = SearchFilter(ResourceType.CONCEPT),
                            orgPath = null,
                            transport = null
                        )
                    )
                ),
                POST
            )
            assertEquals(HttpStatus.OK.value(), response["status"])

            val expected = listOf(
                TimeSeriesPoint(date = LocalDate.of(2024, 2, 1), count = 2),
                TimeSeriesPoint(date = LocalDate.of(2024, 3, 1), count = 2),
                TimeSeriesPoint(date = LocalDate.of(2024, 4, 1), count = 2),
                TimeSeriesPoint(date = LocalDate.of(2024, 5, 1), count = 2),
                TimeSeriesPoint(date = LocalDate.of(2024, 6, 1), count = 2),
                TimeSeriesPoint(date = LocalDate.of(2024, 7, 1), count = 2),
            )
            val result: List<TimeSeriesPoint> = mapper.readValue(response["body"] as String)

            assertEquals(expected, result)
        }

        @Test
        fun getDataServiceTimeSeries() {
            val response = requestApi(
                "/time-series",
                port,
                null,
                mapper.writeValueAsString(
                    TIME_SERIES_REQUEST.copy(
                        filters = TimeSeriesFilters(
                            resourceType = SearchFilter(ResourceType.DATA_SERVICE),
                            orgPath = null,
                            transport = null
                        )
                    )
                ),
                POST
            )
            assertEquals(HttpStatus.OK.value(), response["status"])

            val expected = listOf(
                TimeSeriesPoint(date = LocalDate.of(2024, 3, 1), count = 2),
                TimeSeriesPoint(date = LocalDate.of(2024, 4, 1), count = 2),
                TimeSeriesPoint(date = LocalDate.of(2024, 5, 1), count = 2),
                TimeSeriesPoint(date = LocalDate.of(2024, 6, 1), count = 2),
                TimeSeriesPoint(date = LocalDate.of(2024, 7, 1), count = 2),
            )
            val result: List<TimeSeriesPoint> = mapper.readValue(response["body"] as String)

            assertEquals(expected, result)
        }

        @Test
        fun getDatasetTimeSeries() {
            val response = requestApi(
                "/time-series",
                port,
                null,
                mapper.writeValueAsString(
                    TIME_SERIES_REQUEST.copy(
                        filters = TimeSeriesFilters(
                            resourceType = SearchFilter(ResourceType.DATASET),
                            orgPath = null,
                            transport = null
                        )
                    )
                ),
                POST
            )
            assertEquals(HttpStatus.OK.value(), response["status"])

            val expected = listOf(
                TimeSeriesPoint(date = LocalDate.of(2024, 4, 1), count = 2),
                TimeSeriesPoint(date = LocalDate.of(2024, 5, 1), count = 2),
                TimeSeriesPoint(date = LocalDate.of(2024, 6, 1), count = 2),
                TimeSeriesPoint(date = LocalDate.of(2024, 7, 1), count = 2),
            )
            val result: List<TimeSeriesPoint> = mapper.readValue(response["body"] as String)

            assertEquals(expected, result)
        }

        @Test
        fun getEventTimeSeries() {
            val response = requestApi(
                "/time-series",
                port,
                null,
                mapper.writeValueAsString(
                    TIME_SERIES_REQUEST.copy(
                        filters = TimeSeriesFilters(
                            resourceType = SearchFilter(ResourceType.EVENT),
                            orgPath = null,
                            transport = null
                        )
                    )
                ),
                POST
            )
            assertEquals(HttpStatus.OK.value(), response["status"])

            val expected = listOf(
                TimeSeriesPoint(date = LocalDate.of(2024, 5, 1), count = 2),
                TimeSeriesPoint(date = LocalDate.of(2024, 6, 1), count = 2),
                TimeSeriesPoint(date = LocalDate.of(2024, 7, 1), count = 2),
            )
            val result: List<TimeSeriesPoint> = mapper.readValue(response["body"] as String)

            assertEquals(expected, result)
        }

        @Test
        fun getInformationModelTimeSeries() {
            val response = requestApi(
                "/time-series",
                port,
                null,
                mapper.writeValueAsString(
                    TIME_SERIES_REQUEST.copy(
                        filters = TimeSeriesFilters(
                            resourceType = SearchFilter(ResourceType.INFORMATION_MODEL),
                            orgPath = null,
                            transport = null
                        )
                    )
                ),
                POST
            )
            assertEquals(HttpStatus.OK.value(), response["status"])

            val expected = listOf(
                TimeSeriesPoint(date = LocalDate.of(2024, 6, 1), count = 2),
                TimeSeriesPoint(date = LocalDate.of(2024, 7, 1), count = 2),
            )
            val result: List<TimeSeriesPoint> = mapper.readValue(response["body"] as String)

            assertEquals(expected, result)
        }

        @Test
        fun getServiceTimeSeries() {
            val response = requestApi(
                "/time-series",
                port,
                null,
                mapper.writeValueAsString(
                    TIME_SERIES_REQUEST.copy(
                        filters = TimeSeriesFilters(
                            resourceType = SearchFilter(ResourceType.SERVICE),
                            orgPath = null,
                            transport = null
                        )
                    )
                ),
                POST
            )
            assertEquals(HttpStatus.OK.value(), response["status"])

            val expected = listOf(
                TimeSeriesPoint(date = LocalDate.of(2024, 7, 1), count = 2),
            )
            val result: List<TimeSeriesPoint> = mapper.readValue(response["body"] as String)

            assertEquals(expected, result)
        }

    }

    @Nested
    inner class OrgPathFilter {

        @Test
        fun getTimeSeriesForOrg() {
            val response = requestApi(
                "/time-series",
                port,
                null,
                mapper.writeValueAsString(
                    TIME_SERIES_REQUEST.copy(
                        filters = TimeSeriesFilters(
                            resourceType = null,
                            orgPath = SearchFilter("/PRIVAT/987654321"),
                            transport = null
                        )
                    )
                ),
                POST
            )
            assertEquals(HttpStatus.OK.value(), response["status"])

            val expected = listOf(
                TimeSeriesPoint(date = LocalDate.of(2024, 2, 1), count = 1),
                TimeSeriesPoint(date = LocalDate.of(2024, 3, 1), count = 2),
                TimeSeriesPoint(date = LocalDate.of(2024, 4, 1), count = 3),
                TimeSeriesPoint(date = LocalDate.of(2024, 5, 1), count = 4),
                TimeSeriesPoint(date = LocalDate.of(2024, 6, 1), count = 5),
                TimeSeriesPoint(date = LocalDate.of(2024, 7, 1), count = 6),
            )
            val result: List<TimeSeriesPoint> = mapper.readValue(response["body"] as String)

            assertEquals(expected, result)
        }

        @Test
        fun getTimeSeriesForSTAT() {
            val response = requestApi(
                "/time-series",
                port,
                null,
                mapper.writeValueAsString(
                    TIME_SERIES_REQUEST.copy(
                        filters = TimeSeriesFilters(
                            resourceType = null,
                            orgPath = SearchFilter("/STAT"),
                            transport = null
                        )
                    )
                ),
                POST
            )
            assertEquals(HttpStatus.OK.value(), response["status"])

            val expected = listOf(
                TimeSeriesPoint(date = LocalDate.of(2024, 2, 1), count = 1),
                TimeSeriesPoint(date = LocalDate.of(2024, 3, 1), count = 2),
                TimeSeriesPoint(date = LocalDate.of(2024, 4, 1), count = 3),
                TimeSeriesPoint(date = LocalDate.of(2024, 5, 1), count = 4),
                TimeSeriesPoint(date = LocalDate.of(2024, 6, 1), count = 5),
                TimeSeriesPoint(date = LocalDate.of(2024, 7, 1), count = 6),
            )
            val result: List<TimeSeriesPoint> = mapper.readValue(response["body"] as String)

            assertEquals(expected, result)
        }
    }

    @Nested
    inner class TransportFilter {

        @Test
        fun getTransportTimeSeries() {
            val response = requestApi(
                "/time-series",
                port,
                null,
                mapper.writeValueAsString(
                    TIME_SERIES_REQUEST.copy(
                        filters = TimeSeriesFilters(
                            resourceType = null,
                            orgPath = null,
                            transport = SearchFilter(true)
                        )
                    )
                ),
                POST
            )
            assertEquals(HttpStatus.OK.value(), response["status"])

            val expected = listOf(
                TimeSeriesPoint(date = LocalDate.of(2024, 4, 1), count = 1),
                TimeSeriesPoint(date = LocalDate.of(2024, 5, 1), count = 1),
                TimeSeriesPoint(date = LocalDate.of(2024, 6, 1), count = 1),
                TimeSeriesPoint(date = LocalDate.of(2024, 7, 1), count = 1),
            )
            val result: List<TimeSeriesPoint> = mapper.readValue(response["body"] as String)

            assertEquals(expected, result)
        }

    }
}
