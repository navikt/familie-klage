package no.nav.familie.klage.kabal.domain

import no.nav.familie.klage.infrastruktur.config.DatabaseConfiguration.StringListWrapper
import no.nav.familie.klage.kabal.BehandlingEventType
import no.nav.familie.klage.kabal.ExternalUtfall
import no.nav.familie.klage.kabal.dto.KlageresultatDto
import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.util.UUID

class Klageresultat(
    @Id
    val eventId: UUID,
    val type: BehandlingEventType,
    val utfall: ExternalUtfall?,
    val mottattEllerAvsluttetTidspunkt: LocalDateTime,
    val kildereferanse: UUID,
    val journalpostReferanser: StringListWrapper,
    val behandlingId: UUID
)

fun List<Klageresultat>.tilDto(): List<KlageresultatDto> {
    return this.map {
        KlageresultatDto(
            type = it.type,
            utfall = it.utfall,
            hendelseTidspunkt = it.mottattEllerAvsluttetTidspunkt,
            journalpostReferanser = it.journalpostReferanser.verdier,
            behandlingId = it.behandlingId
        )
    }
}
