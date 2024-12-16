package no.nav.familie.klage.brev.mottaker

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BrevmottakerMedAdresseService(
    private val brevmottakerMedAdresseRepository: BrevmottakerMedAdresseRepository,
) {
    fun hentBrevmottakere(behandlingId: UUID): List<BrevmottakerMedAdresse> =
        brevmottakerMedAdresseRepository.findByBehandlingId(behandlingId)

    fun oppdaterBrevmottakere(
        behandlingId: UUID,
        brevmottakere: BrevmottakerMedAdresse,
    ): List<BrevmottakerMedAdresse> {
        //brevmottakerMedAdresseRepository.deleteByBehandlingId(behandlingId)
        brevmottakerMedAdresseRepository.insert(brevmottakere)
        return hentBrevmottakere(behandlingId)
    }
}
