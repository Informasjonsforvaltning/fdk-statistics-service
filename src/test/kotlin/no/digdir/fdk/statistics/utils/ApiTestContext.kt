package no.digdir.fdk.statistics.utils

import no.digdir.fdk.statistics.model.RecalculateRequest
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
import java.time.LocalDate

abstract class ApiTestContext {
    @LocalServerPort
    var port = 0

    @Autowired
    private lateinit var statisticsService: StatisticsService

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @BeforeEach
    fun resetDB() {
        with(jdbcTemplate) { update("TRUNCATE latest_for_date, statistics", emptyMap<String, String>()) }

        statisticsService.storeConceptStatistics("concept-0", CONCEPT_0, 1686305600000)
        statisticsService.storeConceptStatistics("concept-1", CONCEPT_1, 1686305600000)
        statisticsService.markAsRemovedForTimestamp("concept-0", 1686927347000, ResourceType.CONCEPT)
        statisticsService.storeConceptStatistics("concept-0", CONCEPT_0, 1687964147000)

        statisticsService.storeDataServiceStatistics("data-service-0", DATA_SERVICE_0, 1688223347000)
        statisticsService.storeDataServiceStatistics("data-service-1", DATA_SERVICE_1, 1689346547000)
        statisticsService.markAsRemovedForTimestamp("data-service-0", 1689951347000, ResourceType.DATA_SERVICE)
        statisticsService.storeDataServiceStatistics("data-service-0", DATA_SERVICE_0, 1690642547000)

        statisticsService.storeDatasetStatistics("dataset-0", DATASET_0, 1690988147000)
        statisticsService.storeDatasetStatistics("dataset-1", DATASET_1, 1690988147000)
        statisticsService.markAsRemovedForTimestamp("dataset-0", 1691852147000, ResourceType.DATASET)
        statisticsService.storeDatasetStatistics("dataset-0", DATASET_0, 1693061747000)

        statisticsService.storeEventStatistics("event-0", EVENT_0, 1693752947000)
        statisticsService.storeEventStatistics("event-1", EVENT_1, 1694616947000)
        statisticsService.markAsRemovedForTimestamp("event-0", 1694616947000, ResourceType.EVENT)
        statisticsService.storeEventStatistics("event-0", EVENT_0, 1695480947000)

        statisticsService.storeInformationModelStatistics("model-0", INFORMATION_MODEL_0, 1696431347000)
        statisticsService.storeInformationModelStatistics("model-1", INFORMATION_MODEL_1, 1696431347000)
        statisticsService.markAsRemovedForTimestamp("model-0", 1697295347000, ResourceType.INFORMATION_MODEL)
        statisticsService.storeInformationModelStatistics("model-0", INFORMATION_MODEL_0, 1698159347000)

        statisticsService.storeServiceStatistics("service-0", SERVICE_0, 1698850547000)
        statisticsService.storeServiceStatistics("service-1", SERVICE_1, 1699714547000)
        statisticsService.markAsRemovedForTimestamp("service-0", 1699714547000, ResourceType.SERVICE)
        statisticsService.storeServiceStatistics("service-0", SERVICE_0, 1700664947000)

        statisticsService.recalculate(RecalculateRequest(
            startInclusive = LocalDate.of(2023, 5, 1),
            endExclusive = LocalDate.of(2024, 10, 1))
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
