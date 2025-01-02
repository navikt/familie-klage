package no.nav.familie.klage.brev.mottaker

import no.nav.familie.klage.infrastruktur.exception.Feil

object BrevmottakerValidator {
    fun valider(
        brevmottaker: Brevmottaker,
        eksisterendeBrevmottakere: List<Brevmottaker>,
        brukerensNavn: String,
    ) {
        val eksisterendeMottakertyper = eksisterendeBrevmottakere.map { it.mottakertype }

        when {
            eksisterendeMottakertyper.any { it == brevmottaker.mottakertype } -> {
                throw Feil("Kan ikke ha duplikate mottakertyper. ${brevmottaker.mottakertype} finnes allerede.")
            }

            brevmottaker.mottakertype == Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE && brevmottaker.navn != brukerensNavn -> {
                throw Feil("Ved bruker med utenlandsk adresse skal brevmottakerens navn være brukerens navn.")
            }

            brevmottaker.mottakertype == Mottakertype.DØDSBO && !brevmottaker.navn.contains(brukerensNavn) -> {
                throw Feil("Ved dødsbo skal brevmottakerens navn inneholde brukerens navn.")
            }

            brevmottaker.mottakertype == Mottakertype.DØDSBO && eksisterendeBrevmottakere.isNotEmpty() -> {
                throw Feil("Kan ikke legge til dødsbo når det allerede finnes brevmottakere.")
            }

            eksisterendeMottakertyper.any { it == Mottakertype.DØDSBO } -> {
                throw Feil("Kan ikke legge til flere brevmottakere når det allerede finnes et dødsbo")
            }

            Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE in eksisterendeMottakertyper &&
                brevmottaker.mottakertype !== Mottakertype.VERGE &&
                brevmottaker.mottakertype !== Mottakertype.FULLMEKTIG
            -> {
                throw Feil("Bruker med utenlandsk adresse kan kun kombineres med verge eller fullmektig.")
            }

            eksisterendeMottakertyper.isNotEmpty() &&
                Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE !in eksisterendeMottakertyper &&
                brevmottaker.mottakertype !== Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE
            -> {
                throw Feil("Kan kun legge til bruker med utenlandsk adresse om det finnes en brevmottaker allerede.")
            }
        }
    }
}
