package no.nav.familie.klage.brev.mottaker

import no.nav.familie.klage.brev.domain.Mottakertype
import no.nav.familie.klage.infrastruktur.exception.Feil

object BrevmottakerValidator {
    fun valider(
        brevmottaker: Brevmottaker,
        eksisterendeBrevmottakere: List<Brevmottaker>,
        brukerensNavn: String,
    ) {
        val eksistererMottakertypeAllerede =
            eksisterendeBrevmottakere
                .map { it.mottakerType }
                .any { it == brevmottaker.mottakerType }
        if (eksistererMottakertypeAllerede) {
            throw Feil("Mottakertype finnes allerede.")
        }
        if (brevmottaker.mottakerType == Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE && brevmottaker.navn != brukerensNavn) {
            throw Feil("Ved bruker med utenlandsk adresse skal brevmottakerens navn være brukerens navn.")
        }
        if (brevmottaker.mottakerType == Mottakertype.DØDSBO && brevmottaker.navn != brukerensNavn) {
            throw Feil("Ved dødsbo skal brevmottakerens navn være brukerens navn.")
        }
        if (brevmottaker.mottakerType == Mottakertype.DØDSBO && eksisterendeBrevmottakere.isNotEmpty()) {
            throw Feil("Ved dødsbo kan det ikke være flere brevmottakere.")
        }
    }
}
