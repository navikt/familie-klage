package no.nav.familie.klage.henlegg

import java.util.UUID

data class TrukketKlageBrevDto(
    val behandlingId: UUID,
    val saksbehandlerSignatur: String,
    val saksbehandlerIdent: String,
)
