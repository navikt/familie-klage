package no.nav.familie.klage.distribusjon.baks

import no.nav.familie.klage.brev.baks.brevmottaker.Brevmottaker
import no.nav.familie.klage.brev.baks.brevmottaker.Mottakertype

fun utledJournalførbareBrevmottakere(
    brukersNavn: String,
    brevmottakere: List<Brevmottaker>,
): List<JournalførbarBrevmottaker> {
    if (brevmottakere.isEmpty()) {
        return listOf(JournalførbarBrevmottaker.opprettForBruker(brukersNavn))
    }

    if (brevmottakere.any { it.mottakertype == Mottakertype.DØDSBO }) {
        val dødsbo = brevmottakere.first { it.mottakertype == Mottakertype.DØDSBO }
        return listOf(JournalførbarBrevmottaker.opprettForBrevmottaker(dødsbo))
    }

    val brukerMedUtenlandskAdresse = brevmottakere.find {
        it.mottakertype == Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE
    }

    val bruker = if (brukerMedUtenlandskAdresse != null) {
        JournalførbarBrevmottaker.opprettForBrevmottaker(brukerMedUtenlandskAdresse)
    } else {
        JournalførbarBrevmottaker.opprettForBruker(brukersNavn)
    }

    val fullmektigEllerVerge = brevmottakere
        .find { it.erFullmektigEllerVerge() }
        ?.let { JournalførbarBrevmottaker.opprettForBrevmottaker(it) }

    return listOfNotNull(bruker, fullmektigEllerVerge)
}
