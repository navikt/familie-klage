package no.nav.familie.klage.brev.dto

import java.util.UUID

data class VedtaksbrevFritekstDto(
    val overskrift: String,
    val avsnitt: List<AvsnittDto>,
    val behandlingId: UUID
)
