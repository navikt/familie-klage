package no.nav.familie.klage.brevmottaker

import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.familie.klage.infrastruktur.exception.feilHvis

object BrevmottakerUtil {
    fun validerUnikeBrevmottakere(mottakere: Brevmottakere) {
        val personmottakerIdentifikatorer = mottakere.personer.map {
            when (it) {
                is BrevmottakerPersonMedIdent -> it.personIdent
                is BrevmottakerPersonUtenIdent -> it.id.toString()
            }
        }
        brukerfeilHvisIkke(personmottakerIdentifikatorer.distinct().size == personmottakerIdentifikatorer.size) {
            "En person kan bare legges til en gang som brevmottaker"
        }

        val organisasjonsmottakerIdenter = mottakere.organisasjoner.map { it.organisasjonsnummer }
        brukerfeilHvisIkke(organisasjonsmottakerIdenter.distinct().size == organisasjonsmottakerIdenter.size) {
            "En organisasjon kan bare legges til en gang som brevmottaker"
        }
    }

    fun validerMinimumEnMottaker(mottakere: Brevmottakere) {
        feilHvis(mottakere.personer.isEmpty() && mottakere.organisasjoner.isEmpty()) {
            "Må ha minimum en mottaker"
        }
    }
}
