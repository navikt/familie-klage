package no.nav.familie.klage.behandlingsstatistikk

import no.nav.familie.eksterne.kontrakter.saksstatistikk.klage.BehandlingsstatistikkKlage
import no.nav.familie.kontrakter.felles.objectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class BehandlingsstatistikkProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    @Value("\${BEHANDLINGSSTATISTIKK_TOPIC}")
    lateinit var topic: String

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun sendBehandlingsstatistikk(statistikk: BehandlingsstatistikkKlage) {
        logger.info("Sending to Kafka topic: {}", topic)
        if (secureLogger.isDebugEnabled) {
            secureLogger.debug("Sending to Kafka topic: {}\nBehandlingsstatistikkKlage: {}", topic, statistikk)
        }
        runCatching {
            kafkaTemplate.send(topic, statistikk.behandlingId.toString(), statistikk.toJson()).get()
            logger.info(
                "Behandlingstatistikk for behandling=${statistikk.behandlingId} " +
                    "behandlingStatus=${statistikk.behandlingStatus} sent til Kafka",
            )
        }.onFailure {
            val errorMessage = "Could not send behandling to Kafka. Check secure logs for more information."
            logger.error(errorMessage)
            secureLogger.error("Could not send behandling to Kafka", it)
            throw RuntimeException(errorMessage)
        }
    }

    private fun Any.toJson(): String = objectMapper.writeValueAsString(this)
}
