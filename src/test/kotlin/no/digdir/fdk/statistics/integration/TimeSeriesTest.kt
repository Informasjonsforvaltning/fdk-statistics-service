package no.digdir.fdk.statistics.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.digdir.fdk.statistics.model.Interval
import no.digdir.fdk.statistics.model.TimeSeriesPoint
import no.digdir.fdk.statistics.utils.ApiTestContext
import no.digdir.fdk.statistics.utils.TIME_SERIES_REQUEST
import no.digdir.fdk.statistics.utils.requestApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import kotlin.test.assertTrue

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
    fun getTimeSeriesHandlesNullBody() {
        val response = requestApi("/time-series", port, null, POST)
        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: List<TimeSeriesPoint> = mapper.readValue(response["body"] as String)
        assertTrue(result.size > 11)
    }

    @Test
    fun getTimeSeriesPeriodMonth() {
        val response = requestApi("/time-series", port, mapper.writeValueAsString(TIME_SERIES_REQUEST), POST)
        assertEquals(HttpStatus.OK.value(), response["status"])

        val expected = listOf(
            TimeSeriesPoint(date = LocalDate.of(2023, 6, 11), count = 2),
            TimeSeriesPoint(date = LocalDate.of(2023, 7, 11), count = 3),
            TimeSeriesPoint(date = LocalDate.of(2023, 8, 11), count = 6),
            TimeSeriesPoint(date = LocalDate.of(2023, 9, 11), count = 7),
            TimeSeriesPoint(date = LocalDate.of(2023, 10, 11), count = 10),
            TimeSeriesPoint(date = LocalDate.of(2023, 11, 11), count = 11),
            TimeSeriesPoint(date = LocalDate.of(2023, 12, 11), count = 12),
            TimeSeriesPoint(date = LocalDate.of(2024, 1, 11), count = 12),
        )
        val result: List<TimeSeriesPoint> = mapper.readValue(response["body"] as String)

        assertEquals(expected, result)
    }

    @Test
    fun getTimeSeriesPeriodWeek() {
        val response = requestApi(
            "/time-series",
            port,
            mapper.writeValueAsString(TIME_SERIES_REQUEST.copy(interval = Interval.WEEK)),
            POST
        )
        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: List<TimeSeriesPoint> = mapper.readValue(response["body"] as String)
        assertEquals(31, result.size)
    }

    @Test
    fun getTimeSeriesPeriodDay() {
        val response = requestApi(
            "/time-series",
            port,
            mapper.writeValueAsString(TIME_SERIES_REQUEST.copy(interval = Interval.DAY)),
            POST
        )
        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: List<TimeSeriesPoint> = mapper.readValue(response["body"] as String)
        assertEquals(215, result.size)
    }

    @Test
    fun getTimeSeriesBadRequestWhenStartIsAfterEnd() {
        val response = requestApi(
            "/time-series",
            port,
            mapper.writeValueAsString(TIME_SERIES_REQUEST.copy(end = "2010-01-01")),
            POST
        )
        assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])
    }
}
