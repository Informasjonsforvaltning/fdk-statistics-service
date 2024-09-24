package no.digdir.fdk.statistics.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.digdir.fdk.statistics.model.RecalculateRequest
import no.digdir.fdk.statistics.utils.ApiTestContext
import no.digdir.fdk.statistics.utils.jwk.Access
import no.digdir.fdk.statistics.utils.jwk.JwtToken
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("integration")
class RecalculateTest : ApiTestContext() {
    private val mapper = jacksonObjectMapper()
    private val defaultBody = RecalculateRequest(
        startInclusive = LocalDate.of(2023, 9, 1),
        endExclusive = LocalDate.of(2023, 10, 1)
    )

    @Test
    fun unauthorizedWhenMissingToken() {
        val response = requestApi(
            "/recalculate",
            port,
            null,
            mapper.writeValueAsString(defaultBody),
            POST
        )
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
    }

    @Test
    fun forbiddenWhenNotSysAdmin() {
        val response = requestApi(
            "/recalculate",
            port,
            JwtToken(Access.ORG_WRITE).toString(),
            mapper.writeValueAsString(defaultBody),
            POST
        )
        assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
    }

    @Test
    fun okWhenSysAdmin() {
        val response = requestApi(
            "/recalculate",
            port,
            JwtToken(Access.ROOT).toString(),
            mapper.writeValueAsString(defaultBody),
            POST
        )
        assertEquals(HttpStatus.OK.value(), response["status"])
    }

}
