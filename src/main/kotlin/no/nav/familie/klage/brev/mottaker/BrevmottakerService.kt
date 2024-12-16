package no.nav.familie.klage.brev.mottaker

import no.nav.familie.klage.infrastruktur.exception.Feil
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

    fun oppdaterBrevmottakere(behandlingId: UUID, brevmottaker: Brevmottaker): List<Brevmottaker> {
        val eksisterendeBrevmottakere = hentBrevmottakere(behandlingId)
        val brukerensNavn = personopplysningerService.hentPersonopplysninger(behandlingId).navn
        BrevmottakerValidator.valider(brevmottaker, eksisterendeBrevmottakere, brukerensNavn)

        brevmottakerRepository.insert(brevmottaker)

        return hentBrevmottakere(behandlingId)
    }

    fun slettBrevmottaker(behandlingId: UUID, brevmottakerId: UUID): List<Brevmottaker> {
        val eksisterendeBrevmottakere = hentBrevmottakere(behandlingId)
        if (brevmottakerId !in eksisterendeBrevmottakere.map { it.id }) {
            throw Feil("Fant ikke brevmottaker med id $brevmottakerId")
        }
        brevmottakerRepository.deleteById(brevmottakerId)
        return eksisterendeBrevmottakere.filter { it.id != brevmottakerId }
    }
}
