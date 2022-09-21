package no.nav.familie.klage.kabal.dto

import no.nav.familie.klage.kabal.BehandlingEventType
import no.nav.familie.klage.kabal.ExternalUtfall
import java.time.LocalDateTime

class KlageresultatDto(
    val type: BehandlingEventType,
    val utfall: ExternalUtfall?,
    val mottattEllerAvsluttetTidspunkt: LocalDateTime,
    val journalpostReferanser: List<String>
)
