package no.nav.familie.klage.henlegg

import java.util.UUID

data class TrukketKlageDto(
    val behandlingId: UUID,
    val saksbehandlerSignatur: String,
    val saksbehandlerIdent: String,
)
