package no.digdir.fdk.statistics

import no.digdir.fdk.statistics.model.CalculationRequest
import no.digdir.fdk.statistics.service.StatisticsService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.time.LocalDate

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "scheduling", name=["enabled"], havingValue="true", matchIfMissing = true)
open class Scheduler(private val statisticsService: StatisticsService) {
    private val log = LoggerFactory.getLogger(Scheduler::class.java)

    /**
     * Run every day at 05:30
     */
    @Scheduled(cron = "0 30 5 * * ?")
    fun calculateLatestForYesterday() {
        val yesterday = LocalDate.now().minusDays(1)
        log.info("Calculating the latest version of all resources for $yesterday")

        statisticsService.calculateLatest(
            CalculationRequest(
                startInclusive = yesterday,
                endExclusive = LocalDate.now()
            )
        )
    }

}
