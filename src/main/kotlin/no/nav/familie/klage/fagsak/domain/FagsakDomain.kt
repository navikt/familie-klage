package no.nav.familie.klage.fagsak.domain

import no.nav.familie.klage.felles.domain.Sporbar
import no.nav.familie.kontrakter.felles.Fagsystem
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Fagsak(
    val id: UUID,
    val fagsakPersonId: UUID,
    val personIdenter: Set<PersonIdent>,
    val eksternId: String,
    val stønadstype: Stønadstype,
    val fagsystem: Fagsystem,
    val sporbar: Sporbar
) {
    fun hentAktivIdent(): String {
        return personIdenter.maxByOrNull { it.sporbar.endret.endretTid }?.ident ?: error("Fant ingen ident på fagsak $id")
    }
}

data class FagsakDomain(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsakPersonId: UUID,
    @Column("stonadstype")
    val stønadstype: Stønadstype,
    val fagsystem: Fagsystem,
    val eksternId: String,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar()
) {
    fun tilFagsakMedPerson(identer: Set<PersonIdent>): Fagsak =
        Fagsak(
            id = id,
            fagsakPersonId = fagsakPersonId,
            personIdenter = identer,
            eksternId = eksternId,
            stønadstype = stønadstype,
            fagsystem = fagsystem,
            sporbar = sporbar
        )
}

enum class Stønadstype {
    OVERGANGSSTØNAD,
    SKOLEPENGER,
    BARNETILSYN,
    BARNETRYGD,
    KONTANTSTØTTE
}
