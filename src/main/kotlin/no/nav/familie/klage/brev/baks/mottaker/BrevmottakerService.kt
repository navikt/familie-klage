package no.nav.familie.klage.brev.baks.mottaker

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BrevmottakerService(
    private val brevmottakerHenter: BrevmottakerHenter,
    private val brevmottakerOppretter: BrevmottakerOppretter,
    private val brevmottakerSletter: BrevmottakerSletter,
) {
    fun hentBrevmottakere(behandlingId: UUID): List<Brevmottaker> {
        return brevmottakerHenter.hentBrevmottakere(behandlingId)
    }

    @Transactional
    fun opprettBrevmottaker(behandlingId: UUID, nyBrevmottaker: NyBrevmottaker): Brevmottaker {
        return brevmottakerOppretter.opprettBrevmottaker(behandlingId, nyBrevmottaker)
    }

    @Transactional
    fun slettBrevmottaker(behandlingId: UUID, brevmottakerId: UUID) {
        brevmottakerSletter.slettBrevmottaker(behandlingId, brevmottakerId)
    }
}
