package no.nav.familie.klage.brev.baks

import jakarta.transaction.Transactional
import no.nav.familie.klage.brev.FamilieDokumentClient
import no.nav.familie.klage.felles.domain.Fil
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BaksBrevService(
    private val baksBrevHenter: BaksBrevHenter,
    private val baksBrevOppretter: BaksBrevOppretter,
    private val baksBrevRepository: BaksBrevRepository,
    private val familieDokumentClient: FamilieDokumentClient,
) {
    fun hentBrev(behandlingId: UUID): BaksBrev {
        return baksBrevHenter.hentBrev(behandlingId)
    }

    @Transactional
    fun opprettBrev(behandlingId: UUID): BaksBrev {
        return baksBrevOppretter.opprettBrev(behandlingId)
    }

    // TODO : Hvorfor kan vi ikke bare lage PDFen når vi oppretter brevet i metoden over?
    fun lagBrevPdf(behandlingId: UUID) {
        val brev = baksBrevRepository.findByIdOrThrow(behandlingId)
        feilHvis(brev.pdf != null) {
            "Det finnes allerede en lagret pdf"
        }

        val generertBrev = familieDokumentClient.genererPdfFraHtml(brev.html)
        baksBrevRepository.update(brev.copy(pdf = Fil(generertBrev)))
    }
}
