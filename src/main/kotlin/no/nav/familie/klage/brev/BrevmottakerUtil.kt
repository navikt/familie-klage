package no.nav.familie.klage.brev

import no.nav.familie.klage.brev.domain.Brevmottakere
import no.nav.familie.klage.infrastruktur.exception.feilHvis

object BrevmottakerUtil {
    fun validerMinimumEnMottaker(mottakere: Brevmottakere) {
        feilHvis(mottakere.personer.isEmpty() && mottakere.organisasjoner.isEmpty()) {
            "Må ha minimum en mottaker"
        }
    }
}
