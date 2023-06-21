package no.nav.familie.klage.kabal.domain

import no.nav.familie.klage.infrastruktur.config.DatabaseConfiguration.StringListWrapper
import no.nav.familie.kontrakter.felles.klage.BehandlingEventType
import no.nav.familie.kontrakter.felles.klage.KlageinstansResultatDto
import no.nav.familie.kontrakter.felles.klage.KlageinstansUtfall
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("klageresultat")
class KlageinstansResultat(
    @Id
    val eventId: UUID,
    val type: BehandlingEventType,
    val utfall: KlageinstansUtfall?,
    val mottattEllerAvsluttetTidspunkt: LocalDateTime,
    val kildereferanse: UUID,
    val journalpostReferanser: StringListWrapper,
    val behandlingId: UUID,
    @Column("arsak_feilregistrert")
    val årsakFeilregistrert: String? = null,
)

fun List<KlageinstansResultat>.tilDto(): List<KlageinstansResultatDto> {
    return this.map {
        KlageinstansResultatDto(
            type = it.type,
            utfall = it.utfall,
            mottattEllerAvsluttetTidspunkt = it.mottattEllerAvsluttetTidspunkt,
            journalpostReferanser = it.journalpostReferanser.verdier,
        )
    }
}
