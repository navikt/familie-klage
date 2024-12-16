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
        /* TODO:
         * Dødsbo kan ikke kombineres med andre mottakere.
         * Samme mottakertype kan ikke velges to ganger.
         * Hvis dødsbo skal navn være pre-utfylt med brukerens navn.
         * Ved mottakertype "BRUKER_MED_UTENLANDSK_ADRESSE" skal brukerens navn være pre-utfylt.
         */
        brevmottakerRepository.insert(brevmottakere)
        return hentBrevmottakere(behandlingId)
    }
}
