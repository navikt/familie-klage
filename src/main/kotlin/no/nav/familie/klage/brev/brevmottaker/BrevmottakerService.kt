package no.nav.familie.klage.brev.brevmottaker

import jakarta.transaction.Transactional
import no.nav.familie.klage.brev.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brev.domain.Brevmottakere
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
    fun opprettBrevmottaker(behandlingId: UUID, nyBrevmottakerPersonUtenIdent: NyBrevmottakerPersonUtenIdent): BrevmottakerPersonUtenIdent {
        return brevmottakerOppretter.opprettBrevmottaker(behandlingId, nyBrevmottakerPersonUtenIdent)
    }

    @Transactional
    fun slettBrevmottaker(behandlingId: UUID, brevmottakerId: UUID) {
        brevmottakerSletter.slettBrevmottaker(behandlingId, brevmottakerId)
    }
}
