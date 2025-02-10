package no.nav.familie.klage.brevmottaker

import jakarta.transaction.Transactional
import no.nav.familie.klage.brevmottaker.domain.Brevmottaker
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.brevmottaker.domain.NyBrevmottaker
import no.nav.familie.klage.brevmottaker.domain.SlettbarBrevmottaker
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BrevmottakerService(
    private val brevmottakerHenter: BrevmottakerHenter,
    private val brevmottakerErstatter: BrevmottakerErstatter,
    private val brevmottakerOppretter: BrevmottakerOppretter,
    private val brevmottakerSletter: BrevmottakerSletter,
) {
    fun hentBrevmottakere(behandlingId: UUID): Brevmottakere {
        return brevmottakerHenter.hentBrevmottakere(behandlingId)
    }

    @Transactional
    fun erstattBrevmottakere(behandlingId: UUID, brevmottakere: Brevmottakere): Brevmottakere {
        return brevmottakerErstatter.erstattBrevmottakere(behandlingId, brevmottakere)
    }

    @Transactional
    fun opprettBrevmottaker(behandlingId: UUID, nyBrevmottaker: NyBrevmottaker): Brevmottaker {
        return brevmottakerOppretter.opprettBrevmottaker(behandlingId, nyBrevmottaker)
    }

    @Transactional
    fun slettBrevmottaker(behandlingId: UUID, slettbarBrevmottaker: SlettbarBrevmottaker) {
        brevmottakerSletter.slettBrevmottaker(behandlingId, slettbarBrevmottaker)
    }
}
