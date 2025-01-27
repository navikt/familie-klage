package no.nav.familie.klage.brev.domain

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
    val journalposter: List<BrevmottakerJournalpost>,
)

sealed interface BrevmottakerJournalpost {
    val journalpostId: String
    val distribusjonId: String?
    fun medDistribusjonsId(distribusjonId: String): BrevmottakerJournalpost
}

data class BrevmottakerJournalpostMedIdent(
    val ident: String,
    override val journalpostId: String,
    override val distribusjonId: String? = null,
) : BrevmottakerJournalpost {
    override fun medDistribusjonsId(distribusjonId: String) = copy(distribusjonId = distribusjonId)
}

data class BrevmottakerJournalpostUtenIdent(
    val id: UUID,
    override val journalpostId: String,
    override val distribusjonId: String? = null,
) : BrevmottakerJournalpost {
    override fun medDistribusjonsId(distribusjonId: String) = copy(distribusjonId = distribusjonId)
}
