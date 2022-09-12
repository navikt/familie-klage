package no.nav.familie.klage.fagsak.dto

import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Ytelsestype
import java.util.UUID

data class FagsakDto(
    val id: UUID,
    val fagsakPersonId: UUID,
    val personIdent: String,
    val ytelsestype: Ytelsestype,
    val eksternId: String,
    val fagsystem: Fagsystem
)

fun Fagsak.tilDto(): FagsakDto =
    FagsakDto(
        id = this.id,
        fagsakPersonId = this.fagsakPersonId,
        personIdent = this.hentAktivIdent(),
        ytelsestype = ytelsestype,
        eksternId = this.eksternId,
        fagsystem = this.fagsystem
    )
