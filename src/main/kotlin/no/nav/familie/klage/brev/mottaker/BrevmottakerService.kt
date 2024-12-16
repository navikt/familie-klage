package no.nav.familie.klage.brev.mottaker

import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BrevmottakerService(
    private val personopplysningerService: PersonopplysningerService,
    private val brevmottakerRepository: BrevmottakerRepository,
) {
    fun hentBrevmottakere(behandlingId: UUID): List<Brevmottaker> =
        brevmottakerRepository.findByBehandlingId(behandlingId)

    fun oppdaterBrevmottakere(
        behandlingId: UUID,
        brevmottaker: Brevmottaker,
    ): List<Brevmottaker> {
        val eksisterendeBrevmottakere = hentBrevmottakere(behandlingId)
        val brukerensNavn = personopplysningerService.hentPersonopplysninger(behandlingId).navn
        BrevmottakerValidator.valider(brevmottaker, eksisterendeBrevmottakere, brukerensNavn)

        brevmottakerRepository.insert(brevmottaker)

        return hentBrevmottakere(behandlingId)
    }
}
