package no.nav.familie.klage.fagsak.dto

import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.fagsak.domain.Stønadstype
import java.util.UUID

data class FagsakDto(
    val id: UUID,
    val fagsakPersonId: UUID,
    val personIdent: String,
    val stønadstype: Stønadstype,
    val eksternId: String
)

fun Fagsak.tilDto(): FagsakDto =
    FagsakDto(
        id = this.id,
        fagsakPersonId = this.fagsakPersonId,
        personIdent = this.hentAktivIdent(),
        stønadstype = this.stønadstype,
        eksternId = this.eksternId
    )

