package no.digdir.fdk.statistics.configuration

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreaker.StateTransition
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent
import no.digdir.fdk.statistics.kafka.KafkaManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
open class CircuitBreakerConsumerConfig(
    private val kafkaManager: KafkaManager
) {

    @Bean
    open fun circuitBreakerRegistry(): CircuitBreakerRegistry {
        val defaultConfig = CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10)
            .failureRateThreshold(50f)
            .permittedNumberOfCallsInHalfOpenState(3)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build()

        val registry = CircuitBreakerRegistry.of(defaultConfig)
        attachListener(registry, RDF_PARSE_CIRCUIT_BREAKER_ID, "rdf-parse")
        attachListener(registry, REMOVE_CIRCUIT_BREAKER_ID, "remove")
        return registry
    }

    private fun attachListener(registry: CircuitBreakerRegistry, breakerId: String, listenerId: String) {
        registry.circuitBreaker(breakerId)
            .eventPublisher
            .onStateTransition { event: CircuitBreakerOnStateTransitionEvent ->
                handleStateTransition(event, listenerId)
            }
    }

    private fun handleStateTransition(event: CircuitBreakerOnStateTransitionEvent, listenerId: String) {
        logger.debug("Handling state transition in circuit breaker {}", event)
        when (event.stateTransition) {
            StateTransition.CLOSED_TO_OPEN,
            StateTransition.CLOSED_TO_FORCED_OPEN,
            StateTransition.HALF_OPEN_TO_OPEN -> {
                logger.warn("Circuit breaker opened, pausing Kafka listener: {}", listenerId)
                kafkaManager.pause(listenerId)
            }

            StateTransition.OPEN_TO_HALF_OPEN,
            StateTransition.HALF_OPEN_TO_CLOSED,
            StateTransition.FORCED_OPEN_TO_CLOSED,
            StateTransition.FORCED_OPEN_TO_HALF_OPEN -> {
                logger.info("Circuit breaker closed, resuming Kafka listener: {}", listenerId)
                kafkaManager.resume(listenerId)
            }

            else -> throw IllegalStateException("Unknown transition state: " + event.stateTransition)
        }
    }

    @Bean
    open fun rdfParseCircuitBreaker(registry: CircuitBreakerRegistry): CircuitBreaker =
        registry.circuitBreaker(RDF_PARSE_CIRCUIT_BREAKER_ID)

    @Bean
    open fun removeCircuitBreaker(registry: CircuitBreakerRegistry): CircuitBreaker =
        registry.circuitBreaker(REMOVE_CIRCUIT_BREAKER_ID)

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(CircuitBreakerConsumerConfig::class.java)
        const val RDF_PARSE_CIRCUIT_BREAKER_ID = "rdf-parse"
        const val REMOVE_CIRCUIT_BREAKER_ID = "remove"
    }
}
