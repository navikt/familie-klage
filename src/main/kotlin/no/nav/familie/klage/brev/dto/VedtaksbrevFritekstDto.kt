package no.nav.familie.ef.sak.brev.dto

import no.nav.familie.klage.brev.dto.Avsnitt
import java.util.UUID

data class VedtaksbrevFritekstDto(
    val overskrift: String,
    val avsnitt: List<Avsnitt>,
    val behandlingId: UUID
)
