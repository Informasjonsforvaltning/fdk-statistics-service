package no.digdir.fdk.statistics.controller

import no.digdir.fdk.statistics.model.TimeSeriesPoint
import no.digdir.fdk.statistics.model.TimeSeriesRequest
import no.digdir.fdk.statistics.service.StatisticsService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(produces = ["application/json"])
class StatisticsController(private val statisticsService: StatisticsService) {

    @GetMapping(value = ["/time-series"])
    fun timeSeries(@RequestBody req: TimeSeriesRequest?): ResponseEntity<List<TimeSeriesPoint>> =
        ResponseEntity(
            statisticsService.timeSeries(req ?: TimeSeriesRequest()),
            HttpStatus.OK
        )

}
