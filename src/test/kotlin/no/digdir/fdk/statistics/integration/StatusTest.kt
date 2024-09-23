package no.digdir.fdk.statistics.integration

import no.digdir.fdk.statistics.utils.ApiTestContext
import no.digdir.fdk.statistics.utils.requestApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("integration")
class StatusTest : ApiTestContext() {

    @Test
    fun ping() {
        val response = requestApi("/ping", port, null, GET)
        assertEquals(HttpStatus.OK.value(), response["status"])
    }

    @Test
    fun ready() {
        val response = requestApi("/ready", port, null, GET)
        assertEquals(HttpStatus.OK.value(), response["status"])
    }
}
