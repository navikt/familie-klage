package no.nav.familie.klage.infrastruktur.config

import no.nav.familie.kafka.KafkaErrorHandler
import no.nav.familie.klage.kabal.BehandlingEvent
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory

@EnableKafka
@Configuration
@Import(
    KafkaErrorHandler::class
)
class KafkaConfig {

    @Bean
    fun klageEventListenerContainerFactory(properties: KafkaProperties, kafkaErrorHandler: KafkaErrorHandler):
        ConcurrentKafkaListenerContainerFactory<String, BehandlingEvent> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, BehandlingEvent>()
        factory.consumerFactory = DefaultKafkaConsumerFactory(properties.buildConsumerProperties())
        factory.setCommonErrorHandler(kafkaErrorHandler)
        return factory
    }
}
