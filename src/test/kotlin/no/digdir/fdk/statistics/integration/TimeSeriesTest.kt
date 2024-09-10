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
import org.springframework.http.HttpMethod.GET
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
        val response = requestApi("/time-series", port, null, GET)
        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: List<TimeSeriesPoint> = mapper.readValue(response["body"] as String)
        assertTrue(result.size > 12)
    }

    @Test
    fun getTimeSeriesPeriodMonth() {
        val response = requestApi("/time-series", port, mapper.writeValueAsString(TIME_SERIES_REQUEST), GET)
        assertEquals(HttpStatus.OK.value(), response["status"])

        val expected = listOf(
            TimeSeriesPoint(dateMillis = 1687305600000, count = 1),
            TimeSeriesPoint(dateMillis = 1689897600000, count = 4),
            TimeSeriesPoint(dateMillis = 1692489600000, count = 5),
            TimeSeriesPoint(dateMillis = 1695081600000, count = 7),
            TimeSeriesPoint(dateMillis = 1697673600000, count = 9),
            TimeSeriesPoint(dateMillis = 1700265600000, count = 11),
            TimeSeriesPoint(dateMillis = 1702857600000, count = 12),
            TimeSeriesPoint(dateMillis = 1704931200000, count = 12)
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
            GET
        )
        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: List<TimeSeriesPoint> = mapper.readValue(response["body"] as String)
        assertTrue(result.size == 31)
    }

    @Test
    fun getTimeSeriesPeriodDay() {
        val response = requestApi(
            "/time-series",
            port,
            mapper.writeValueAsString(TIME_SERIES_REQUEST.copy(interval = Interval.DAY)),
            GET
        )
        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: List<TimeSeriesPoint> = mapper.readValue(response["body"] as String)
        assertTrue(result.size == 205)
    }

    @Test
    fun getTimeSeriesBadRequestWhenStartIsAfterEnd() {
        val response = requestApi(
            "/time-series",
            port,
            mapper.writeValueAsString(TIME_SERIES_REQUEST.copy(end = LocalDate.of(2010, 1, 1))),
            GET
        )
        assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])
    }
}
