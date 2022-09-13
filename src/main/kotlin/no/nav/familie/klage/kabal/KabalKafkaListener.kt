package no.nav.familie.klage.kabal

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.klage.kabal.event.BehandlingEventService
import no.nav.familie.kontrakter.felles.objectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.*

@Component
class KabalKafkaListener(val behandlingEventService: BehandlingEventService) : ConsumerSeekAware {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @KafkaListener(
        id = "familie-klage",
        topics = ["klage.behandling-events.v1"]
    )
    fun listen(behandlingEventJson: String) {
        secureLogger.info("Klage-kabal-event: $behandlingEventJson")
        val behandlingEvent = objectMapper.readValue<BehandlingEvent>(behandlingEventJson)
        if (behandlingEvent.kilde == "EF") { // BA og KS kan legges til her ved behov
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

    fun utfallErMedhold(): Boolean {
        return detaljer.klagebehandlingAvsluttet?.utfall == ExternalUtfall.MEDHOLD ||
            detaljer.ankebehandlingAvsluttet?.utfall == ExternalUtfall.MEDHOLD
    }

    fun lagOppgaveTekst(): String {
        val utfall = detaljer.klagebehandlingAvsluttet?.utfall ?: detaljer.ankebehandlingAvsluttet?.utfall ?: "ukjent"
        val tidspunkt = detaljer.klagebehandlingAvsluttet?.avsluttet ?: detaljer.ankebehandlingAvsluttet?.avsluttet ?: "ukjent"
        val journalpostReferanser = detaljer.klagebehandlingAvsluttet?.journalpostReferanser
        return "Hendelse fra klage av type $type har fått utfall $utfall den $tidspunkt. Journalpost referanser: ${journalpostReferanser?.joinToString(", ")}"
    }
}

enum class BehandlingEventType {
    KLAGEBEHANDLING_AVSLUTTET, ANKEBEHANDLING_OPPRETTET, ANKEBEHANDLING_AVSLUTTET, ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET
}

data class BehandlingDetaljer(
    val klagebehandlingAvsluttet: KlagebehandlingAvsluttetDetaljer? = null,
    val ankebehandlingOpprettet: AnkebehandlingOpprettetDetaljer? = null,
    val ankebehandlingAvsluttet: AnkebehandlingAvsluttetDetaljer? = null,
)

data class KlagebehandlingAvsluttetDetaljer(
    val avsluttet: LocalDateTime,
    val utfall: ExternalUtfall,
    val journalpostReferanser: List<String>,
)

data class AnkebehandlingOpprettetDetaljer(
    val mottattKlageinstans: LocalDateTime
)

data class AnkebehandlingAvsluttetDetaljer(
    val avsluttet: LocalDateTime,
    val utfall: ExternalUtfall,
    val journalpostReferanser: List<String>,
)

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
