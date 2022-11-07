package no.nav.familie.klage.metrics

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import no.nav.familie.klage.metrics.domain.MålerRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class MålerService(private val målerRepository: MålerRepository) {

    private val åpneBehandlingerPerUkeGauge = MultiGauge.builder("KlarTilBehandlingPerUke").register(Metrics.globalRegistry)
    private val behandlingerPerStatus = MultiGauge.builder("BehandlingerPerStatus").register(Metrics.globalRegistry)
    private val vedtakGauge = MultiGauge.builder("Vedtak").register(Metrics.globalRegistry)

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(initialDelay = 60 * 1000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun åpneBehandlingerPerUke() {
        val behandlinger = målerRepository.finnÅpneBehandlingerPerUke()
        logger.info("Åpne behandlinger per uke returnerte ${behandlinger.sumOf { it.antall }} fordelt på ${behandlinger.size} uker.")
        val rows = behandlinger.map {
            MultiGauge.Row.of(
                Tags.of(
                    "ytelse",
                    it.stonadstype.name,
                    "uke",
                    it.år.toString() + "-" + it.uke.toString().padStart(2, '0')
                ),
                it.antall
            )
        }

        åpneBehandlingerPerUkeGauge.register(rows, true)
    }

    @Scheduled(initialDelay = 90 * 1000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun behandlingsstatus() {
        val behandlinger = målerRepository.finnBehandlingerPerStatus()
        logger.info(
            "Behandlinger per status returnerte ${behandlinger.sumOf { it.antall }} " +
                    "fordelt på ${behandlinger.size} statuser."
        )
        val rows = behandlinger.map {
            MultiGauge.Row.of(
                Tags.of(
                    "ytelse",
                    it.stonadstype.name,
                    "status",
                    it.status.name
                ),
                it.antall
            )
        }

        behandlingerPerStatus.register(rows, true)
    }

    @Scheduled(initialDelay = 180 * 1000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun vedtakPerUke() {
        val data = målerRepository.finnVedtakPerUke()
        logger.info("Vedtak returnerte ${data.sumOf { it.antall }} fordelt på ${data.size} typer/uker.")

        val rows = data.map {
            MultiGauge.Row.of(
                Tags.of(
                    "ytelse", it.stonadstype.name,
                    "resultat", it.resultat.name,
                    "uke", it.år.toString() + "-" + it.uke.toString().padStart(2, '0')
                ),
                it.antall
            )
        }
        vedtakGauge.register(rows)
    }

    companion object {

        const val OPPDATERINGSFREKVENS = 30 * 60 * 1000L
    }
}
