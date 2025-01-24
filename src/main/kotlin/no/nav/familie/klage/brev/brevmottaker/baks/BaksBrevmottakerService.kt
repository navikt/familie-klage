package no.nav.familie.klage.brev.brevmottaker.baks

import jakarta.transaction.Transactional
import no.nav.familie.klage.brev.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brev.domain.Brevmottakere
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BaksBrevmottakerService(
    private val baksBrevmottakerHenter: BaksBrevmottakerHenter,
    private val baksBrevmottakerOppretter: BaksBrevmottakerOppretter,
    private val baksBrevmottakerSletter: BaksBrevmottakerSletter,
) {
    fun hentBrevmottakere(behandlingId: UUID): Brevmottakere {
        return baksBrevmottakerHenter.hentBrevmottakere(behandlingId)
    }

    @Transactional
    fun opprettBrevmottaker(behandlingId: UUID, nyBrevmottakerPersonUtenIdent: NyBrevmottakerPersonUtenIdent): BrevmottakerPersonUtenIdent {
        return baksBrevmottakerOppretter.opprettBrevmottaker(behandlingId, nyBrevmottakerPersonUtenIdent)
    }

    @Transactional
    fun slettBrevmottaker(behandlingId: UUID, brevmottakerId: UUID) {
        baksBrevmottakerSletter.slettBrevmottaker(behandlingId, brevmottakerId)
    }
}
