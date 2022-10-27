package no.nav.familie.klage.brev.dto

import java.util.UUID

data class FritekstBrevDto(
    val overskrift: String,
    val avsnitt: List<AvsnittDto>,
    val behandlingId: UUID,
)

data class FritekstBrevRequestDto(
    val overskrift: String,
    val avsnitt: List<AvsnittDto>,
    val personIdent: String,
    val navn: String
)
