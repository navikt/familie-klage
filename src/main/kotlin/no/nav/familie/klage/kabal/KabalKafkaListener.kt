package no.nav.familie.klage.kabal

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.kabal.event.BehandlingEventService
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.objectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

@Component
class KabalKafkaListener(val behandlingEventService: BehandlingEventService) : ConsumerSeekAware {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @KafkaListener(
        id = "familie-klage",
        topics = ["klage.behandling-events.v1"],
        autoStartup = "\${kafka.enabled:true}"
    )
    fun listen(behandlingEventJson: String) {
        secureLogger.info("Klage-kabal-event: $behandlingEventJson")
        val behandlingEvent = objectMapper.readValue<BehandlingEvent>(behandlingEventJson)
        if (behandlingEvent.kilde == Fagsystem.EF.name) { // BA og KS kan legges til her ved behov
            behandlingEventService.handleEvent(behandlingEvent)
        }
        secureLogger.info("Serialisert behandlingEvent: $behandlingEvent")
    }

    /* Beholdes for å enkelt kunne lese fra start ved behov
    override fun onPartitionsAssigned(
        assignments: MutableMap<org.apache.kafka.common.TopicPartition, Long>,
        callback: ConsumerSeekAware.ConsumerSeekCallback
    ) {
        logger.info("overrided onPartitionsAssigned seekToBeginning")
        assignments.keys.stream()
            .filter { it.topic() == "klage.behandling-events.v1" }
            .forEach {
                callback.seekToBeginning("klage.behandling-events.v1", it.partition())
            }
    }
     */
}

// se no.nav.familie.klage.kabal.OversendtKlageAnkeV3
data class BehandlingEvent(
    val eventId: UUID,
    val kildeReferanse: String,
    val kilde: String,
    val kabalReferanse: String,
    val type: BehandlingEventType,
    val detaljer: BehandlingDetaljer,
) {
    fun mottattEllerAvsluttetTidspunkt(): LocalDateTime {
        val feilmelding = "Burde hatt behandlingdetaljer for event fra kabal av type $type"
        return when (type) {
            BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET ->
                detaljer.klagebehandlingAvsluttet?.avsluttet ?: throw Feil(feilmelding)
            BehandlingEventType.ANKEBEHANDLING_OPPRETTET ->
                detaljer.ankebehandlingOpprettet?.mottattKlageinstans ?: throw Feil(feilmelding)
            BehandlingEventType.ANKEBEHANDLING_AVSLUTTET -> detaljer.ankebehandlingAvsluttet?.avsluttet ?: throw Feil(feilmelding)
            BehandlingEventType.ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET -> throw Feil("Håndterer ikke typen $type")
        }
    }

    fun utfall(): ExternalUtfall? {
        val feilmelding = "Burde hatt behandlingdetaljer for event fra kabal av type $type"
        return when (type) {
            BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET -> detaljer.klagebehandlingAvsluttet?.utfall ?: throw Feil(feilmelding)
            BehandlingEventType.ANKEBEHANDLING_AVSLUTTET -> detaljer.ankebehandlingAvsluttet?.utfall ?: throw Feil(feilmelding)
            else -> null
        }
    }

    fun journalpostReferanser(): List<String> {
        return when (type) {
            BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET -> detaljer.klagebehandlingAvsluttet?.journalpostReferanser ?: listOf()
            BehandlingEventType.ANKEBEHANDLING_AVSLUTTET -> detaljer.ankebehandlingAvsluttet?.journalpostReferanser ?: listOf()
            else -> listOf()
        }
    }
}

enum class BehandlingEventType {
    KLAGEBEHANDLING_AVSLUTTET, ANKEBEHANDLING_OPPRETTET, ANKEBEHANDLING_AVSLUTTET, ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET // TODO ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET skal fjernes på sikt
}

data class BehandlingDetaljer(
    val klagebehandlingAvsluttet: KlagebehandlingAvsluttetDetaljer? = null,
    val ankebehandlingOpprettet: AnkebehandlingOpprettetDetaljer? = null,
    val ankebehandlingAvsluttet: AnkebehandlingAvsluttetDetaljer? = null,
) {

    fun journalpostReferanser(): List<String> {
        return klagebehandlingAvsluttet?.journalpostReferanser ?: ankebehandlingAvsluttet?.journalpostReferanser ?: listOf()
    }

    fun oppgaveTekst(): String {
        return klagebehandlingAvsluttet?.oppgaveTekst()
            ?: ankebehandlingOpprettet?.oppgaveTekst()
            ?: ankebehandlingAvsluttet?.oppgaveTekst()
            ?: "Ukjent"
    }
}

data class KlagebehandlingAvsluttetDetaljer(
    val avsluttet: LocalDateTime,
    val utfall: ExternalUtfall,
    val journalpostReferanser: List<String>
) {

    fun oppgaveTekst(): String {
        return "Hendelse fra klage av type klagebehandling avsluttet med utfall: $utfall mottatt. " +
            "Avsluttet tidspunkt: $avsluttet. " +
            "Journalpost referanser: ${journalpostReferanser.joinToString(", ")}"
    }
}

data class AnkebehandlingOpprettetDetaljer(
    val mottattKlageinstans: LocalDateTime
) {

    fun oppgaveTekst(): String {
        return "Hendelse fra klage av type ankebehandling opprettet mottatt. Mottatt klageinstans: $mottattKlageinstans."
    }
}

data class AnkebehandlingAvsluttetDetaljer(
    val avsluttet: LocalDateTime,
    val utfall: ExternalUtfall,
    val journalpostReferanser: List<String>,
) {

    fun oppgaveTekst(): String {
        return "Hendelse fra klage av type ankebehandling avsluttet med utfall: $utfall mottatt. " +
            "Avsluttet tidspunkt: $avsluttet. " +
            "Journalpost referanser: ${journalpostReferanser.joinToString(", ")}"
    }
}

enum class ExternalUtfall(val navn: String) {
    TRUKKET("Trukket"),
    RETUR("Retur"),
    OPPHEVET("Opphevet"),
    MEDHOLD("Medhold"),
    DELVIS_MEDHOLD("Delvis medhold"),
    STADFESTELSE("Stadfestelse"),
    UGUNST("Ugunst (Ugyldig)"),
    AVVIST("Avvist");
}
