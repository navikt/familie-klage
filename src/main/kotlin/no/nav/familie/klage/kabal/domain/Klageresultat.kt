package no.nav.familie.klage.kabal.domain

import no.nav.familie.klage.infrastruktur.config.DatabaseConfiguration.StringListWrapper
import no.nav.familie.kontrakter.felles.klage.BehandlingEventType
import no.nav.familie.kontrakter.felles.klage.KlageinstansResultatDto
import no.nav.familie.kontrakter.felles.klage.KlageinstansUtfall
import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.util.UUID

class Klageresultat(
    @Id
    val eventId: UUID,
    val type: BehandlingEventType,
    val utfall: KlageinstansUtfall?,
    val mottattEllerAvsluttetTidspunkt: LocalDateTime,
    val kildereferanse: UUID,
    val journalpostReferanser: StringListWrapper,
    val behandlingId: UUID
)

fun List<Klageresultat>.tilDto(): List<KlageinstansResultatDto> {
    return this.map {
        KlageinstansResultatDto(
            type = it.type,
            utfall = it.utfall,
            mottattEllerAvsluttetTidspunkt = it.mottattEllerAvsluttetTidspunkt,
            journalpostReferanser = it.journalpostReferanser.verdier
        )
    }
}
