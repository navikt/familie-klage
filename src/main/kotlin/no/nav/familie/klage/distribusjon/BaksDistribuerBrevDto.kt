package no.nav.familie.klage.distribusjon

import java.util.UUID

data class BaksDistribuerBrevDto(
    val behandlingId: UUID,
    val journalpostId: String,
)
