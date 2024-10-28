package no.digdir.fdk.statistics.configuration

import org.springframework.cache.interceptor.KeyGenerator
import java.lang.reflect.Method
import java.util.UUID

// This generator should only be used with the cacheable method timeSeries in StatisticsRepository,
// where the first parameter is the data class TimeSeriesRequest
class TimeSeriesKeyGenerator : KeyGenerator {
    override fun generate(target: Any, method: Method, vararg params: Any): Any {
        return UUID.nameUUIDFromBytes(params[0].toString().toByteArray())
    }
}
