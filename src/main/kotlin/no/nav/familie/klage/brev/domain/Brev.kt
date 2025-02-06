package no.nav.familie.klage.brev.domain

import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.felles.domain.Fil
import no.nav.familie.klage.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Brev(
    @Id
    val behandlingId: UUID,
    val saksbehandlerHtml: String,
    val pdf: Fil? = null,
    val mottakere: Brevmottakere? = null,
    val mottakereJournalposter: BrevmottakereJournalposter? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {

    fun brevPdf() = this.pdf?.bytes ?: error("Mangler brev-pdf for behandling=$behandlingId")
}

data class BrevmottakereJournalposter(
    val journalposter: List<BrevmottakereJournalpost>,
)

data class BrevmottakereJournalpostMedIdent(
    val ident: String?, // Enten personnummer eller orgnummer
    override val journalpostId: String,
    override val distribusjonId: String? = null,
) : BrevmottakereJournalpost

data class BrevmottakereJournalpostUtenIdent(
    val idForBrevmottakereUtenIdent: UUID?,
    override val journalpostId: String,
    override val distribusjonId: String? = null,
) : BrevmottakereJournalpost

sealed interface BrevmottakereJournalpost {
    val journalpostId: String
    val distribusjonId: String?
}
