package no.nav.familie.klage.distribusjon.baks

import no.nav.familie.klage.brev.baks.brevmottaker.BrevmottakerService
import no.nav.familie.klage.brev.baks.brevmottaker.Mottakertype
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import org.springframework.stereotype.Component
import java.util.UUID

// TODO : Rename me
@Component
class DistribuerbarBrevmottakerUtleder(
    private val personopplysningerService: PersonopplysningerService,
    private val brevmottakerService: BrevmottakerService,
) {
    // TODO : Rename me
    fun utledDistribuerbareBrevmottakereForBehandling(behandlingId: UUID): List<DistribuerbarBrevmottaker> {
        val personopplysninger = personopplysningerService.hentPersonopplysninger(behandlingId)
        val brevmottakere = brevmottakerService.hentBrevmottakere(behandlingId)

        if (brevmottakere.isEmpty()) {
            return listOf(DistribuerbarBrevmottaker.opprettForBruker(personopplysninger.navn))
        }

        if (brevmottakere.any { it.mottakertype == Mottakertype.DØDSBO }) {
            val dødsbo = brevmottakere.first { it.mottakertype == Mottakertype.DØDSBO }
            return listOf(DistribuerbarBrevmottaker.opprettForBrevmottaker(dødsbo))
        }

        val brukerMedUtenlandskAdresse = brevmottakere.find {
            it.mottakertype == Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE
        }

        val bruker = if (brukerMedUtenlandskAdresse != null) {
            DistribuerbarBrevmottaker.opprettForBrevmottaker(brukerMedUtenlandskAdresse)
        } else {
            DistribuerbarBrevmottaker.opprettForBruker(personopplysninger.navn)
        }

        val fullmektigEllerVerge = brevmottakere
            .find { it.erFullmektigEllerVerge() }
            ?.let { DistribuerbarBrevmottaker.opprettForBrevmottaker(it) }

        return listOfNotNull(bruker, fullmektigEllerVerge)
    }
}
