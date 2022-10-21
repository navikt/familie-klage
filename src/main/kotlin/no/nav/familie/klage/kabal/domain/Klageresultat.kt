package no.nav.familie.klage.kabal.domain

import no.nav.familie.klage.infrastruktur.config.DatabaseConfiguration.StringListWrapper
import no.nav.familie.kontrakter.felles.klage.BehandlingEventType
import no.nav.familie.kontrakter.felles.klage.EksternKlageresultatDto
import no.nav.familie.kontrakter.felles.klage.ExternalUtfall
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

fun List<Klageresultat>.tilDto(): List<EksternKlageresultatDto> {
    return this.map {
        EksternKlageresultatDto(
            type = it.type,
            utfall = it.utfall,
            mottattEllerAvsluttetTidspunkt = it.mottattEllerAvsluttetTidspunkt,
            journalpostReferanser = it.journalpostReferanser.verdier
        )
    }
}
