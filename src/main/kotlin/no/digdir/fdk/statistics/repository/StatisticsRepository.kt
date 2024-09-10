package no.digdir.fdk.statistics.repository

import no.digdir.fdk.statistics.model.StatisticsObject
import no.digdir.fdk.statistics.model.TimeSeriesPoint
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.ResultSet

@Component
class StatisticsRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {
    private val rowMapper: (ResultSet, rowNum: Int) -> TimeSeriesPoint? = { rs, _ ->
        TimeSeriesPoint(
            dateMillis = rs.getLong("date"),
            count = rs.getInt("count")
        )
    }

    fun timeSeries(start: Long, end: Long, interval: Long): List<TimeSeriesPoint> = with(jdbcTemplate) {
        query(
            """WITH range AS (SELECT generate_series(:start,:end,:interval) AS date UNION SELECT :end),
                        latestForRange AS (
                           SELECT DISTINCT ON (date, fdkId) *
                           FROM statistics, range
                           WHERE timestamp < date
                           ORDER BY date, fdkId, timestamp DESC
                        )
                    SELECT date, count(id)
                    FROM latestForRange
                    WHERE removed = false
                    GROUP BY date;
                """.trimIndent(), mapOf("start" to start, "end" to end, "interval" to interval), rowMapper
        )
    }.filterNotNull()

    fun store(statistics: StatisticsObject) = with(jdbcTemplate) {
        update(
            """INSERT INTO statistics (id, fdkId, timestamp, removed, type, orgPath, isRelatedToTransportportal)
                    VALUES (:id, :fdkId, :timestamp, :removed, :type, :orgPath, :isRelatedToTransportportal)
                    ON CONFLICT DO NOTHING
                """.trimIndent(), statistics.asParams()
        )
    }

    private fun StatisticsObject.asParams() = mapOf(
        "id" to id,
        "fdkId" to fdkId,
        "timestamp" to timestamp,
        "removed" to removed,
        "type" to type.name,
        "orgPath" to orgPath,
        "isRelatedToTransportportal" to isRelatedToTransportportal,
    )

}
