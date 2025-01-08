package no.nav.familie.klage.distribusjon.baks

import no.nav.familie.klage.brev.baks.brevmottaker.BrevmottakerService
import no.nav.familie.klage.brev.baks.brevmottaker.Mottakertype
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JournalpostBrevmottakereUtleder(
    private val personopplysningerService: PersonopplysningerService,
    private val brevmottakerService: BrevmottakerService,
) {
    fun utled(behandlingId: UUID): List<JournalpostBrevmottaker> {
        val personopplysninger = personopplysningerService.hentPersonopplysninger(behandlingId)
        val manuelleBrevmottakere = brevmottakerService.hentBrevmottakere(behandlingId)

        if (manuelleBrevmottakere.isEmpty()) {
            return listOf(JournalpostBrevmottaker.opprett(personopplysninger.navn, Mottakertype.BRUKER))
        }

        if (manuelleBrevmottakere.any { it.mottakertype == Mottakertype.DØDSBO }) {
            val dødsbo = manuelleBrevmottakere.first { it.mottakertype == Mottakertype.DØDSBO }
            return listOf(JournalpostBrevmottaker.opprett(dødsbo))
        }

        val brukerMedUtenlandskAdresse = manuelleBrevmottakere.find {
            it.mottakertype == Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE
        }

        val bruker = if (brukerMedUtenlandskAdresse != null) {
            JournalpostBrevmottaker.opprett(brukerMedUtenlandskAdresse)
        } else {
            JournalpostBrevmottaker.opprett(personopplysninger.navn, Mottakertype.BRUKER)
        }

        val fullmektigEllerVerge = manuelleBrevmottakere
            .find { it.mottakertype == Mottakertype.FULLMEKTIG || it.mottakertype == Mottakertype.VERGE }
            ?.let { JournalpostBrevmottaker.opprett(it) }

        return listOfNotNull(bruker, fullmektigEllerVerge)
    }
}
