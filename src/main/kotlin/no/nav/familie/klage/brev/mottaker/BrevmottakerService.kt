package no.nav.familie.klage.brev.mottaker

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BrevmottakerService(
    private val brevmottakerRepository: BrevmottakerRepository,
) {
    fun hentBrevmottakere(behandlingId: UUID): List<Brevmottaker> = brevmottakerRepository.findByBehandlingId(behandlingId)

    fun oppdaterBrevmottakere(
        behandlingId: UUID,
        brevmottakere: Brevmottaker,
    ): List<Brevmottaker> {
        // brevmottakerMedAdresseRepository.deleteByBehandlingId(behandlingId)
        brevmottakerRepository.insert(brevmottakere)
        return hentBrevmottakere(behandlingId)
    }
}
