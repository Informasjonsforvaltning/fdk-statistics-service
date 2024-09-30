package no.digdir.fdk.statistics.utils

import no.digdir.fdk.statistics.model.CalculationRequest
import no.digdir.fdk.statistics.model.ResourceType
import no.digdir.fdk.statistics.service.StatisticsService
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.testcontainers.containers.PostgreSQLContainer
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

abstract class ApiTestContext {
    @LocalServerPort
    var port = 0

    @Autowired
    private lateinit var statisticsService: StatisticsService

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    private fun epochMillis(year: String, month: String, day: String): Long =
        Instant.parse("$year-$month-${day}T12:00:00.00Z").epochSecond.times(1000)

    @BeforeEach
    fun resetDB() {
        with(jdbcTemplate) { update("TRUNCATE latest_for_date, resource_event_metrics", emptyMap<String, String>()) }

        statisticsService.storeConceptMetrics("concept-0", CONCEPT_0, epochMillis("2024", "01", "09"))
        statisticsService.markResourceAsRemoved("concept-1", epochMillis("2024", "01", "09"), ResourceType.CONCEPT)
        statisticsService.storeConceptMetrics("concept-1", CONCEPT_1, epochMillis("2024", "01", "09"))
        statisticsService.markResourceAsRemoved("concept-0", epochMillis("2024", "01", "16"), ResourceType.CONCEPT)
        statisticsService.storeConceptMetrics("concept-0", CONCEPT_0, epochMillis("2024", "01", "28"))

        statisticsService.storeDataServiceMetrics("data-service-0", DATA_SERVICE_0, epochMillis("2024", "02", "01"))
        statisticsService.markResourceAsRemoved("data-service-1", epochMillis("2024", "02", "14"), ResourceType.DATA_SERVICE)
        statisticsService.storeDataServiceMetrics("data-service-1", DATA_SERVICE_1, epochMillis("2024", "02", "14"))
        statisticsService.markResourceAsRemoved("data-service-0", epochMillis("2024", "02", "21"), ResourceType.DATA_SERVICE)
        statisticsService.storeDataServiceMetrics("data-service-0", DATA_SERVICE_0, epochMillis("2024", "02", "29"))

        statisticsService.storeDatasetMetrics("dataset-0", DATASET_0, epochMillis("2024", "03", "06"))
        statisticsService.markResourceAsRemoved("dataset-1", epochMillis("2024", "03", "06"), ResourceType.DATASET)
        statisticsService.storeDatasetMetrics("dataset-1", DATASET_1, epochMillis("2024", "03", "06"))
        statisticsService.markResourceAsRemoved("dataset-0", epochMillis("2024", "03", "12"), ResourceType.DATASET)
        statisticsService.storeDatasetMetrics("dataset-0", DATASET_0, epochMillis("2024", "03", "26"))

        statisticsService.storeEventMetrics("event-0", EVENT_0, epochMillis("2024", "04", "03"))
        statisticsService.markResourceAsRemoved("event-1", epochMillis("2024", "04", "13"), ResourceType.EVENT)
        statisticsService.storeEventMetrics("event-1", EVENT_1, epochMillis("2024", "04", "13"))
        statisticsService.markResourceAsRemoved("event-0", epochMillis("2024", "04", "13"), ResourceType.EVENT)
        statisticsService.storeEventMetrics("event-0", EVENT_0, epochMillis("2024", "04", "23"))

        statisticsService.storeInformationModelMetrics("model-0", INFORMATION_MODEL_0, epochMillis("2024", "05", "04"))
        statisticsService.markResourceAsRemoved("model-1", epochMillis("2024", "05", "04"), ResourceType.INFORMATION_MODEL)
        statisticsService.storeInformationModelMetrics("model-1", INFORMATION_MODEL_1, epochMillis("2024", "05", "04"))
        statisticsService.markResourceAsRemoved("model-0", epochMillis("2024", "05", "14"), ResourceType.INFORMATION_MODEL)
        statisticsService.storeInformationModelMetrics("model-0", INFORMATION_MODEL_0, epochMillis("2024", "05", "24"))

        statisticsService.storeServiceMetrics("service-0", SERVICE_0, epochMillis("2024", "06", "01"))
        statisticsService.markResourceAsRemoved("service-1", epochMillis("2024", "06", "11"), ResourceType.SERVICE)
        statisticsService.storeServiceMetrics("service-1", SERVICE_1, epochMillis("2024", "06", "11"))
        statisticsService.markResourceAsRemoved("service-0", epochMillis("2024", "06", "11"), ResourceType.SERVICE)
        statisticsService.storeServiceMetrics("service-0", SERVICE_0, epochMillis("2024", "06", "22"))

        statisticsService.calculateLatest(CalculationRequest(
            startInclusive = LocalDate.of(2023, 5, 1),
            endExclusive = LocalDate.of(2024, 8, 1))
        )
    }

    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            TestPropertyValues.of(
                "spring.datasource.url=jdbc:postgresql://${postgreSQLContainer.host}:${
                    postgreSQLContainer.getMappedPort(
                        5432
                    )
                }/stat_test",
                "spring.datasource.username=postgres",
                "spring.datasource.password=postgres",
            ).applyTo(configurableApplicationContext.environment)
        }
    }

    companion object {
        val postgreSQLContainer: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:14-alpine")
            .withExposedPorts(5432)
            .withUsername("postgres")
            .withPassword("postgres")
            .withDatabaseName("stat_test")

        init {
            postgreSQLContainer.start()
            startMockServer()
        }
    }
}
