package no.nav.familie.klage.fagsak.domain

import no.nav.familie.klage.felles.domain.Sporbar
import no.nav.familie.klage.institusjon.Institusjon
import no.nav.familie.klage.kabal.domain.Ytelse
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

data class Fagsak(
    val id: UUID,
    val fagsakEierPersonId: UUID,
    val søkerPersonId: UUID,
    val institusjon: Institusjon? = null,
    val fagsakEierIdenter: Set<PersonIdent>,
    val søkerIdenter: Set<PersonIdent>,
    val eksternId: String,
    val stønadstype: Stønadstype,
    val fagsystem: Fagsystem,
    val sporbar: Sporbar,
) {
    fun hentFagsakEierIdent(): String = fagsakEierIdenter.maxByOrNull { it.sporbar.endret.endretTid }?.ident ?: error("Fant ingen ident på fagsak $id")

    fun hentSøkerIdent(): String = søkerIdenter.maxByOrNull { it.sporbar.endret.endretTid }?.ident ?: error("Fant ingen ident for søker på fagsak $id")

    fun erInstitusjonssak(): Boolean = institusjon != null

    fun erSøkerFagsakEier(): Boolean = fagsakEierPersonId == søkerPersonId
}

@Table("fagsak")
data class FagsakDomain(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsakEierPersonId: UUID,
    @Column("soker_person_id")
    val søkerPersonId: UUID,
    @Column("fk_institusjon_id")
    var institusjonId: UUID? = null,
    @Column("stonadstype")
    val stønadstype: Stønadstype,
    val fagsystem: Fagsystem,
    val eksternId: String,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {
    fun tilFagsakMedPersonOgInstitusjon(
        fagsakEierIdenter: Set<PersonIdent>,
        søkerIdenter: Set<PersonIdent> = fagsakEierIdenter,
        institusjon: Institusjon? = null,
    ): Fagsak =
        Fagsak(
            id = id,
            fagsakEierPersonId = fagsakEierPersonId,
            søkerPersonId = søkerPersonId,
            institusjon = institusjon,
            fagsakEierIdenter = fagsakEierIdenter,
            søkerIdenter = søkerIdenter,
            eksternId = eksternId,
            stønadstype = stønadstype,
            fagsystem = fagsystem,
            sporbar = sporbar,
        )
}

fun Stønadstype.tilYtelse(): Ytelse =
    when (this) {
        Stønadstype.OVERGANGSSTØNAD -> Ytelse.ENF_ENF
        Stønadstype.SKOLEPENGER -> Ytelse.ENF_ENF
        Stønadstype.BARNETILSYN -> Ytelse.ENF_ENF
        Stønadstype.BARNETRYGD -> Ytelse.BAR_BAR
        Stønadstype.KONTANTSTØTTE -> Ytelse.KON_KON
    }
