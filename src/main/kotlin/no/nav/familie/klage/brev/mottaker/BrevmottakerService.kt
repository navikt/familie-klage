package no.nav.familie.klage.brev.mottaker

import jakarta.transaction.Transactional
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BrevmottakerService(
    private val personopplysningerService: PersonopplysningerService,
    private val brevmottakerRepository: BrevmottakerRepository,
) {
    fun hentBrevmottakere(behandlingId: UUID): List<Brevmottaker> {
        Thread.sleep(500)
        return brevmottakerRepository.findByBehandlingId(behandlingId)
    }

    @Transactional
    fun opprettBrevmottaker(behandlingId: UUID, brevmottaker: Brevmottaker): Brevmottaker {
        Thread.sleep(500)
        val eksisterendeBrevmottakere = hentBrevmottakere(behandlingId)
        val brukerensNavn = personopplysningerService.hentPersonopplysninger(behandlingId).navn
        BrevmottakerValidator.validerNyBrevmottaker(brevmottaker, eksisterendeBrevmottakere, brukerensNavn)
        return brevmottakerRepository.insert(brevmottaker)
    }

    @Transactional
    fun slettBrevmottaker(behandlingId: UUID, brevmottakerId: UUID) {
        Thread.sleep(500)
        val eksisterer = brevmottakerRepository.existsById(brevmottakerId)
        if (!eksisterer) {
            throw Feil("Brevmottaker med id $brevmottakerId finnes ikke.")
        }
        brevmottakerRepository.deleteById(brevmottakerId)
    }
}
