package no.digdir.fdk.statistics.configuration

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    fun caffeineConfig(): Caffeine<Any, Any> =
        Caffeine
            .newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.DAYS)

    @Bean
    fun cacheManager(caffeine: Caffeine<Any, Any>): CacheManager {
        val caffeineCacheManager = CaffeineCacheManager()
        caffeineCacheManager.setCaffeine(caffeine)
        return caffeineCacheManager
    }

    @Bean("timeSeriesKeyGenerator")
    fun keyGenerator(): KeyGenerator = TimeSeriesKeyGenerator()
}
