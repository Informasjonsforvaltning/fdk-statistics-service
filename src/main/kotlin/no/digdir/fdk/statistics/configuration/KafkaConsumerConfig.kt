package no.digdir.fdk.statistics.configuration

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import java.time.Duration
import kotlin.jvm.java

@EnableKafka
@Configuration
class KafkaConsumerConfig(
    @param:Value("\${spring.kafka.bootstrap-servers}") private val bootstrapServers: String,
    @param:Value("\${spring.kafka.properties.security.protocol}") private val securityProtocol: String,
    @param:Value("\${spring.kafka.properties.sasl.mechanism}") private val saslMechanism: String,
    @param:Value("\${spring.kafka.properties.sasl.jaas.config}") private val saslJaasConfig: String,
    @param:Value("\${spring.kafka.consumer.properties.schema.registry.url}") private val schemaRegistryUrl: String,
) {
    @Bean
    fun consumerFactory(): ConsumerFactory<String, Any> {
        val props: MutableMap<String, Any> = HashMap()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = securityProtocol
        props[SaslConfigs.SASL_MECHANISM] = saslMechanism
        props[SaslConfigs.SASL_JAAS_CONFIG] = saslJaasConfig
        props[ConsumerConfig.GROUP_ID_CONFIG] = "fdk-statistics-service"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = KafkaAvroDeserializer::class.java
        props["schema.registry.url"] = schemaRegistryUrl
        props["specific.avro.reader"] = true
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        return DefaultKafkaConsumerFactory(props)
    }

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.setConsumerFactory(consumerFactory())
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        // Keep retrying instead of stopping the listeners for good on an auth failure.
        factory.containerProperties.setAuthExceptionRetryInterval(Duration.ofSeconds(30))
        factory.setConcurrency(4)
        return factory
    }
}
