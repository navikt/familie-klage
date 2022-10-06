package no.nav.familie.klage.brev.dto

import java.util.UUID

data class BrevMedAvsnittDto(
    val behandlingId: UUID,
    val overskrift: String,
    val avsnitt: List<AvsnittDto>
)