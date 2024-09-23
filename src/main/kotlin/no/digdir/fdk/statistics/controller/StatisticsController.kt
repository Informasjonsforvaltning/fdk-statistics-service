package no.digdir.fdk.statistics.controller

import no.digdir.fdk.statistics.model.RecalculateRequest
import no.digdir.fdk.statistics.model.TimeSeriesPoint
import no.digdir.fdk.statistics.model.TimeSeriesRequest
import no.digdir.fdk.statistics.service.StatisticsService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(produces = ["application/json"])
class StatisticsController(private val statisticsService: StatisticsService) {

    @PostMapping(value = ["/time-series"])
    fun timeSeries(@RequestBody req: TimeSeriesRequest?): ResponseEntity<List<TimeSeriesPoint>> =
        ResponseEntity(
            statisticsService.timeSeries(req ?: TimeSeriesRequest()),
            HttpStatus.OK
        )

    @PostMapping(value = ["/recalculate"])
    fun recalculate(@RequestBody req: RecalculateRequest): ResponseEntity<Unit> {
        statisticsService.recalculate(req)
        return ResponseEntity(HttpStatus.OK)
    }

}
