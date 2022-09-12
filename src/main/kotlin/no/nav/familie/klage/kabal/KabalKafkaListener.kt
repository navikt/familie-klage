package no.nav.familie.klage.kabal

import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.*

@Component
class KabalKafkaListener {

    @KafkaListener(
        id = "familie-klage-test",
        topics = ["klage.vedtak-fattet.v1"]
    )
    fun listen(@Payload behandlingEvent: BehandlingEvent) {
        println(behandlingEvent)
    }
}


data class BehandlingEvent(
    val eventId: UUID,
    val kildeReferanse: String,
    val kilde: String,
    val kabalReferanse: String,
    val type: BehandlingEventType,
    val detaljer: BehandlingDetaljer,
)

enum class BehandlingEventType {
    KLAGEBEHANDLING_AVSLUTTET, ANKEBEHANDLING_OPPRETTET, ANKEBEHANDLING_AVSLUTTET
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
