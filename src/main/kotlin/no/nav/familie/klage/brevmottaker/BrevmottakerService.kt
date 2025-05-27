package no.nav.familie.klage.brevmottaker

import jakarta.transaction.Transactional
import no.nav.familie.klage.brevmottaker.domain.Brevmottaker
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
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
    private val brevmottakerUtleder: BrevmottakerUtleder,
) {
    fun hentBrevmottakere(behandlingId: UUID): Brevmottakere = brevmottakerHenter.hentBrevmottakere(behandlingId)

    @Transactional
    fun erstattBrevmottakere(
        behandlingId: UUID,
        brevmottakere: Brevmottakere,
    ): Brevmottakere = brevmottakerErstatter.erstattBrevmottakere(behandlingId, brevmottakere)

    @Transactional
    fun opprettBrevmottaker(
        behandlingId: UUID,
        nyBrevmottaker: NyBrevmottaker,
    ): Brevmottaker = brevmottakerOppretter.opprettBrevmottaker(behandlingId, nyBrevmottaker)

    @Transactional
    fun slettBrevmottaker(
        behandlingId: UUID,
        slettbarBrevmottaker: SlettbarBrevmottaker,
    ) {
        brevmottakerSletter.slettBrevmottaker(behandlingId, slettbarBrevmottaker)
    }

    fun utledInitielleBrevmottakere(behandlingId: UUID): Brevmottakere = brevmottakerUtleder.utledInitielleBrevmottakere(behandlingId)

    fun utledBrevmottakerBrukerFraBehandling(behandlingId: UUID): BrevmottakerPersonMedIdent = brevmottakerUtleder.utledBrevmottakerBrukerFraBehandling(behandlingId)
}
