package no.digdir.fdk.statistics.model

import java.time.LocalDate

enum class ResourceType(val availableFrom: LocalDate) {
    CONCEPT(LocalDate.of(2023, 2, 1)),
    DATASET(LocalDate.of(2022, 11, 1)),
    DATA_SERVICE(LocalDate.of(2023, 2, 1)),
    INFORMATION_MODEL(LocalDate.of(2024, 1, 1)),
    SERVICE(LocalDate.of(2024, 1, 1)),
    EVENT(LocalDate.of(2024, 1, 1)),
    ;

    companion object {
        // The earliest date for which every resource type has data available
        val earliestCommonDate: LocalDate = entries.maxOf { it.availableFrom }
    }
}
