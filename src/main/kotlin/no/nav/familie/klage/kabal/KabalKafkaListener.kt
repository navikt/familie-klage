package no.nav.familie.klage.kabal

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.kabal.domain.Type
import no.nav.familie.klage.kabal.event.BehandlingEventService
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.klage.BehandlingEventType
import no.nav.familie.kontrakter.felles.klage.KlageinstansUtfall
import no.nav.familie.kontrakter.felles.jsonMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

@Component
class KabalKafkaListener(
    val behandlingEventService: BehandlingEventService,
) : ConsumerSeekAware {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")
    val støttedeFagsystemer = listOf(Fagsystem.BA.name, Fagsystem.EF.name, Fagsystem.KONT.name)

    @KafkaListener(
        id = "familie-klage",
        topics = ["klage.behandling-events.v1"],
        autoStartup = "\${kafka.enabled:true}",
    )
    fun listen(behandlingEventJson: String) {
        secureLogger.info("Klage-kabal-event: $behandlingEventJson")
        val behandlingEvent = jsonMapper.readValue(behandlingEventJson, BehandlingEvent::class.java)

        if (støttedeFagsystemer.contains(behandlingEvent.kilde)) {
            behandlingEventService.handleEvent(behandlingEvent)
        }
        secureLogger.info("Deserialisert behandlingEvent: $behandlingEvent")
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
            BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET -> {
                detaljer.klagebehandlingAvsluttet?.avsluttet ?: throw Feil(feilmelding)
            }

            BehandlingEventType.ANKEBEHANDLING_OPPRETTET -> {
                detaljer.ankebehandlingOpprettet?.mottattKlageinstans ?: throw Feil(feilmelding)
            }

            BehandlingEventType.ANKEBEHANDLING_AVSLUTTET -> {
                detaljer.ankebehandlingAvsluttet?.avsluttet ?: throw Feil(feilmelding)
            }

            BehandlingEventType.ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET -> {
                detaljer.ankeITrygderettenbehandlingOpprettet?.sendtTilTrygderetten ?: throw Feil(feilmelding)
            }

            BehandlingEventType.BEHANDLING_FEILREGISTRERT -> {
                detaljer.behandlingFeilregistrert?.feilregistrert
                    ?: throw Feil("Fant ikke tidspunkt for feilregistrering")
            }

            BehandlingEventType.BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET_AVSLUTTET -> {
                detaljer.behandlingEtterTrygderettenOpphevetAvsluttet?.avsluttet
                    ?: throw Feil(feilmelding)
            }

            BehandlingEventType.OMGJOERINGSKRAVBEHANDLING_AVSLUTTET -> {
                detaljer.omgjoeringskravbehandlingAvsluttet?.avsluttet
                    ?: throw Feil("Ikke implementert for OMGJOERINGSKRAV_AVSLUTTET")
            }

            BehandlingEventType.GJENOPPTAKSBEHANDLING_AVSLUTTET -> {
                detaljer.gjenopptaksbehandlingAvsluttet?.avsluttet ?: throw Feil(feilmelding)
            }
        }
    }

    fun utfall(): KlageinstansUtfall? {
        val feilmelding = "Burde hatt behandlingdetaljer for event fra kabal av type $type"
        return when (type) {
            BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET -> detaljer.klagebehandlingAvsluttet?.utfall ?: throw Feil(feilmelding)
            BehandlingEventType.ANKEBEHANDLING_AVSLUTTET -> detaljer.ankebehandlingAvsluttet?.utfall ?: throw Feil(feilmelding)
            BehandlingEventType.GJENOPPTAKSBEHANDLING_AVSLUTTET -> detaljer.gjenopptaksbehandlingAvsluttet?.utfall ?: throw Feil(feilmelding)
            else -> null
        }
    }

    fun journalpostReferanser(): List<String> =
        when (type) {
            BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET -> detaljer.klagebehandlingAvsluttet?.journalpostReferanser ?: listOf()
            BehandlingEventType.ANKEBEHANDLING_AVSLUTTET -> detaljer.ankebehandlingAvsluttet?.journalpostReferanser ?: listOf()
            BehandlingEventType.GJENOPPTAKSBEHANDLING_AVSLUTTET -> detaljer.gjenopptaksbehandlingAvsluttet?.journalpostReferanser ?: listOf()
            else -> listOf()
        }
}

data class BehandlingDetaljer(
    val klagebehandlingAvsluttet: KlagebehandlingAvsluttetDetaljer? = null,
    val ankebehandlingOpprettet: AnkebehandlingOpprettetDetaljer? = null,
    val ankebehandlingAvsluttet: AnkebehandlingAvsluttetDetaljer? = null,
    val behandlingFeilregistrert: BehandlingFeilregistrertDetaljer? = null,
    val ankeITrygderettenbehandlingOpprettet: AnkeITrygderettenbehandlingOpprettetDetaljer? = null,
    val behandlingEtterTrygderettenOpphevetAvsluttet: BehandlingEtterTrygderettenOpphevetAvsluttetDetaljer? = null,
    val omgjoeringskravbehandlingAvsluttet: OmgjoeringskravbehandlingAvsluttetDetaljer? = null,
    val gjenopptaksbehandlingAvsluttet: GjenopptaksbehandlingAvsluttetDetaljer? = null,
) {
    fun oppgaveTekst(saksbehandlersEnhet: String): String =
        klagebehandlingAvsluttet?.oppgaveTekst(saksbehandlersEnhet)
            ?: ankebehandlingOpprettet?.oppgaveTekst(saksbehandlersEnhet)
            ?: ankebehandlingAvsluttet?.oppgaveTekst(saksbehandlersEnhet)
            ?: behandlingEtterTrygderettenOpphevetAvsluttet?.oppgaveTekst(saksbehandlersEnhet)
            ?: omgjoeringskravbehandlingAvsluttet?.oppgaveTekst(saksbehandlersEnhet)
            ?: gjenopptaksbehandlingAvsluttet?.oppgaveTekst(saksbehandlersEnhet)
            ?: "Ukjent"
}

data class KlagebehandlingAvsluttetDetaljer(
    val avsluttet: LocalDateTime,
    val utfall: KlageinstansUtfall,
    val journalpostReferanser: List<String>,
) {
    fun oppgaveTekst(saksbehandlersEnhet: String): String =
        "Hendelse fra klage av type klagebehandling avsluttet med utfall: $utfall mottatt. " +
            "Avsluttet tidspunkt: $avsluttet. " +
            "Opprinnelig klagebehandling er behandlet av enhet: $saksbehandlersEnhet. " +
            "Journalpost referanser: ${journalpostReferanser.joinToString(", ")}"
}

data class AnkebehandlingOpprettetDetaljer(
    val mottattKlageinstans: LocalDateTime,
) {
    fun oppgaveTekst(saksbehandlersEnhet: String): String =
        "Hendelse fra klage av type ankebehandling opprettet mottatt. Mottatt klageinstans: $mottattKlageinstans. " +
            "Opprinnelig klagebehandling er behandlet av enhet: $saksbehandlersEnhet."
}

data class AnkebehandlingAvsluttetDetaljer(
    val avsluttet: LocalDateTime,
    val utfall: KlageinstansUtfall,
    val journalpostReferanser: List<String>,
) {
    fun oppgaveTekst(saksbehandlersEnhet: String): String =
        "Hendelse fra klage av type ankebehandling avsluttet med utfall: $utfall mottatt. " +
            "Avsluttet tidspunkt: $avsluttet. " +
            "Opprinnelig klagebehandling er behandlet av enhet: $saksbehandlersEnhet. " +
            "Journalpost referanser: ${journalpostReferanser.joinToString(", ")}"
}

data class GjenopptaksbehandlingAvsluttetDetaljer(
    val avsluttet: LocalDateTime,
    val utfall: KlageinstansUtfall,
    val journalpostReferanser: List<String>,
) {
    fun oppgaveTekst(saksbehandlersEnhet: String): String =
        "Hendelse fra klage av type gjenopptak avsluttet med utfall: $utfall mottatt. " +
            "Avsluttet tidspunkt: $avsluttet. " +
            "Opprinnelig klagebehandling er behandlet av enhet: $saksbehandlersEnhet. " +
            "Journalpost referanser: ${journalpostReferanser.joinToString(", ")}"
}

data class BehandlingFeilregistrertDetaljer(
    val reason: String,
    val type: Type,
    val feilregistrert: LocalDateTime,
)

data class AnkeITrygderettenbehandlingOpprettetDetaljer(
    val sendtTilTrygderetten: LocalDateTime,
    val utfall: KlageinstansUtfall?,
)

data class BehandlingEtterTrygderettenOpphevetAvsluttetDetaljer(
    val avsluttet: LocalDateTime,
    val utfall: KlageinstansUtfall,
    val journalpostReferanser: List<String>,
) {
    fun oppgaveTekst(saksbehandlersEnhet: String): String =
        "Hendelse fra klage av type behandling etter trygderetten opphevet avsluttet med utfall: $utfall mottatt. " +
            "Avsluttet tidspunkt: $avsluttet. " +
            "Opprinnelig klagebehandling er behandlet av enhet: $saksbehandlersEnhet. " +
            "Journalpost referanser: ${journalpostReferanser.joinToString(", ")}"
}

data class OmgjoeringskravbehandlingAvsluttetDetaljer(
    val avsluttet: LocalDateTime,
    val utfall: KlageinstansUtfall,
    val journalpostReferanser: List<String>,
) {
    fun oppgaveTekst(saksbehandlersEnhet: String) =
        "Hendelse fra klage etter omgjøringskrav med utfall $utfall mottatt. " +
            "Avsluttet tidspunkt: $avsluttet. " +
            "Opprinnelig klagebehandling er behandlet av enhet: $saksbehandlersEnhet. " +
            "Journalpost referanser: ${journalpostReferanser.joinToString(", ")}"
}
