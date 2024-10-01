package no.digdir.fdk.statistics.controller

import no.digdir.fdk.statistics.model.CalculationRequest
import no.digdir.fdk.statistics.model.TimeSeriesPoint
import no.digdir.fdk.statistics.model.TimeSeriesRequest
import no.digdir.fdk.statistics.service.EndpointPermissions
import no.digdir.fdk.statistics.service.StatisticsService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping(produces = ["application/json"])
class StatisticsController(
    private val statisticsService: StatisticsService,
    private val endpointPermissions: EndpointPermissions
) {
    private val logger: Logger = LoggerFactory.getLogger(StatisticsController::class.java)

    @PostMapping(value = ["/time-series"])
    fun timeSeries(@RequestBody req: TimeSeriesRequest?): ResponseEntity<List<TimeSeriesPoint>>  =
        try {
            ResponseEntity(statisticsService.timeSeries(req ?: TimeSeriesRequest()), HttpStatus.OK)
        } catch (ex: ResponseStatusException) {
            logger.debug("Time series failed with code {} and reason {}", ex.statusCode, ex.reason)
            ResponseEntity(ex.statusCode)
        }

    @PostMapping(value = ["/calculate-latest"])
    fun calculateLatest(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody req: CalculationRequest
    ): ResponseEntity<Unit> =
        if (endpointPermissions.hasAdminPermission(jwt)) {
            try {
                statisticsService.calculateLatest(req)
                ResponseEntity(HttpStatus.OK)
            } catch (ex: ResponseStatusException) {
                logger.debug("Calculate latest failed with code {} and reason {}", ex.statusCode, ex.reason)
                ResponseEntity(ex.statusCode)
            }
        } else ResponseEntity(HttpStatus.FORBIDDEN)

}
