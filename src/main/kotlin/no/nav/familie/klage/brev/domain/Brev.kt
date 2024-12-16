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
    val journalposter: List<BrevmottakereJournalpost>,
)

data class BrevmottakereJournalpost(
    val ident: String,
    val journalpostId: String,
    val distribusjonId: String? = null,
)

data class Brevmottakere(
    val personer: List<BrevmottakerPerson> = emptyList(),
    val organisasjoner: List<BrevmottakerOrganisasjon> = emptyList(),
)

enum class Mottakertype {
    BRUKER,
    VERGE,
    FULLMAKT,
    BRUKER_MED_UTENLANDSK_ADRESSE,
    DØDSBO,
}

data class BrevmottakerPerson(
    val personIdent: String,
    val navn: String,
    val mottakerType: Mottakertype,
) : Brevmottaker()

data class BrevmottakerOrganisasjon(
    val organisasjonsnummer: String,
    val organisasjonsnavn: String,
    val navnHosOrganisasjon: String,
) : Brevmottaker()

sealed class Brevmottaker
