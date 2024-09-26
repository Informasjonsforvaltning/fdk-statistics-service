package no.digdir.fdk.statistics.repository

import no.digdir.fdk.statistics.model.Interval
import no.digdir.fdk.statistics.model.LatestForDate
import no.digdir.fdk.statistics.model.ResourceEventMetrics
import no.digdir.fdk.statistics.model.ResourceType
import no.digdir.fdk.statistics.model.SearchFilter
import no.digdir.fdk.statistics.model.TimeSeriesFilters
import no.digdir.fdk.statistics.model.TimeSeriesPoint
import no.digdir.fdk.statistics.model.TimeSeriesRequest
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet

@Component
open class StatisticsRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {
    private val timeSeriesRowMapper: (ResultSet, rowNum: Int) -> TimeSeriesPoint? = { rs, _ ->
        TimeSeriesPoint(
            date = rs.getDate("calcDate").toLocalDate(),
            count = rs.getInt("calcCount")
        )
    }
    private val latestRowMapper: (ResultSet, rowNum: Int) -> Pair<String, String>? = { rs, _ ->
        Pair(
            rs.getString("id"),
            rs.getString("fdkId")
        )
    }

    private fun Interval.toValue(): String =
        when (this) {
            Interval.DAY -> "1 DAY"
            Interval.WEEK -> "7 DAYS"
            Interval.MONTH -> "1 MONTH"
        }

    private fun typeFilter(filter: SearchFilter<ResourceType>?): String =
        if (filter == null) ""
        else " AND metrics.type = :type"

    private fun orgPathFilter(filter: SearchFilter<String>?): String =
        if (filter == null) ""
        else " AND metrics.orgPath ~ :orgPath"

    private fun transportFilter(filter: SearchFilter<Boolean>?): String =
        if (filter == null) ""
        else " AND metrics.isRelatedToTransportportal = :transport"

    private fun TimeSeriesFilters?.toSQL(): String =
        if (this == null) ""
        else "${typeFilter(resourceType)}${orgPathFilter(orgPath)}${transportFilter(transport)}"

    open fun timeSeries(req: TimeSeriesRequest): List<TimeSeriesPoint> = with(jdbcTemplate) {
        query(
            """WITH range AS (SELECT generate_series(:start::DATE , :end::DATE , :interval::INTERVAL) AS dt)
                SELECT r.dt AS calcDate, COUNT(*) AS calcCount
                FROM range r
                JOIN latest_for_date lfd ON lfd.calculatedForDate = r.dt
                JOIN resource_event_metrics metrics ON lfd.statId = metrics.id
                WHERE metrics.removed = false ${req.filters.toSQL()}
                GROUP BY r.dt
                ORDER BY r.dt;
            """.trimIndent(),
            mapOf(
                "start" to req.start,
                "end" to req.end,
                "interval" to req.interval.toValue(),
                "type" to req.filters?.resourceType?.value?.name,
                "orgPath" to req.filters?.orgPath?.value,
                "transport" to req.filters?.transport?.value
            ),
            timeSeriesRowMapper
        ).filterNotNull()
    }

    open fun latestForTimestamp(date: Long): Map<String, String> = with(jdbcTemplate) {
        query(
            """WITH ranked_statistics AS (
                    SELECT id, fdkId, ROW_NUMBER() OVER (PARTITION BY fdkId ORDER BY timestamp DESC) AS rn
                    FROM resource_event_metrics
                    WHERE timestamp < :date
                )
                SELECT id, fdkId
                FROM ranked_statistics
                WHERE rn = 1;
            """.trimIndent(), mapOf("date" to date), latestRowMapper
        )
    }.filterNotNull().toMap()

    @Transactional
    open fun storeMetrics(data: ResourceEventMetrics) = with(jdbcTemplate) {
        update(
            """INSERT INTO resource_event_metrics (id, fdkId, timestamp, removed, type, orgPath, isRelatedToTransportportal)
                    VALUES (:id, :fdkId, :timestamp, :removed, :type, :orgPath, :isRelatedToTransportportal)
                    ON CONFLICT (id)
                    DO UPDATE SET removed = :removed,
                        type = :type,
                        orgPath = :orgPath,
                        isRelatedToTransportportal = :isRelatedToTransportportal;
                """.trimIndent(), data.asParams()
        )
    }

    @Transactional
    open fun storeLatestForDate(latestForDate: LatestForDate) = with(jdbcTemplate) {
        update(
            """INSERT INTO latest_for_date (fdkId, calculatedForDate, statId)
                    VALUES (:fdkId, :calculatedForDate, :statId)
                    ON CONFLICT (fdkId, calculatedForDate)
                    DO UPDATE SET statId = :statId;
                """.trimIndent(), latestForDate.asParams()
        )
    }

    private fun ResourceEventMetrics.asParams() = mapOf(
        "id" to id,
        "fdkId" to fdkId,
        "timestamp" to timestamp,
        "removed" to removed,
        "type" to type.name,
        "orgPath" to orgPath,
        "isRelatedToTransportportal" to isRelatedToTransportportal,
    )

    private fun LatestForDate.asParams() = mapOf(
        "fdkId" to fdkId,
        "calculatedForDate" to calculatedForDate,
        "statId" to statId
    )

}
