package no.nav.familie.klage.brev.mottaker

import no.nav.familie.klage.infrastruktur.exception.Feil

object BrevmottakerValidator {
    fun valider(
        brevmottaker: Brevmottaker,
        eksisterendeBrevmottakere: List<Brevmottaker>,
        brukerensNavn: String,
    ) {
        val eksisterendeMottakertyper =
            eksisterendeBrevmottakere
                .map { it.mottakertype }

        when {
            eksisterendeMottakertyper.isNotEmpty() &&
                (
                    brevmottaker.mottakertype !== Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE ||
                        !eksisterendeMottakertyper.contains(Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE)
                ) -> {
                throw Feil("Kan ikke legge til to brevmottakere med mindre en av de er en bruker med utenlandsk adresse.")
            }

            eksisterendeMottakertyper.any { it == brevmottaker.mottakertype } -> {
                throw Feil("Mottakertype finnes allerede.")
            }

            brevmottaker.mottakertype == Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE && brevmottaker.navn != brukerensNavn -> {
                throw Feil("Ved bruker med utenlandsk adresse skal brevmottakerens navn være brukerens navn.")
            }

            brevmottaker.mottakertype == Mottakertype.DØDSBO && !brevmottaker.navn.contains(brukerensNavn) -> {
                throw Feil("Ved dødsbo skal brevmottakerens navn inneholde brukerens navn.")
            }

            brevmottaker.mottakertype == Mottakertype.DØDSBO &&
                eksisterendeBrevmottakere.isNotEmpty() ||
                eksisterendeMottakertyper.any { it == Mottakertype.DØDSBO } -> {
                throw Feil("Ved dødsbo kan det ikke være flere brevmottakere.")
            }
        }
    }
}
