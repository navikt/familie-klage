package no.nav.familie.klage.brev.mottaker

import no.nav.familie.klage.infrastruktur.exception.Feil

object BrevmottakerValidator {
    fun valider(
        brevmottaker: Brevmottaker,
        eksisterendeBrevmottakere: List<Brevmottaker>,
        brukerensNavn: String,
    ) {
        val eksistererMottakertypeAllerede =
            eksisterendeBrevmottakere
                .map { it.mottakertype }
                .any { it == brevmottaker.mottakertype }
        if (eksistererMottakertypeAllerede) {
            throw Feil("Mottakertype finnes allerede.")
        }
        if (brevmottaker.mottakertype == Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE && brevmottaker.navn != brukerensNavn) {
            throw Feil("Ved bruker med utenlandsk adresse skal brevmottakerens navn være brukerens navn.")
        }
        if (brevmottaker.mottakertype == Mottakertype.DØDSBO && brevmottaker.navn != brukerensNavn) {
            throw Feil("Ved dødsbo skal brevmottakerens navn være brukerens navn.")
        }
        if (brevmottaker.mottakertype == Mottakertype.DØDSBO && eksisterendeBrevmottakere.isNotEmpty()) {
            throw Feil("Ved dødsbo kan det ikke være flere brevmottakere.")
        }
    }
}
