package no.nav.familie.klage.henlegg

import no.nav.familie.klage.fagsak.domain.Fagsak
import java.util.UUID

data class TrukketKlageBrevDto(
    val behandlingId: UUID,
    val saksbehandlerSignatur: String,
    val saksbehandlerIdent: String,
    val fagSak: Fagsak,
)
