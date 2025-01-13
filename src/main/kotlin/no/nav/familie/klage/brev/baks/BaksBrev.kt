package no.nav.familie.klage.brev.baks

import no.nav.familie.klage.felles.domain.Fil
import no.nav.familie.klage.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class BaksBrev(
    @Id
    val behandlingId: UUID,
    val html: String,
    val pdf: Fil? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {
    fun pdfSomBytes() = this.pdf?.bytes ?: error("Mangler brev-pdf for behandling=$behandlingId")
}
