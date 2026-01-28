package no.nav.familie.klage.brevmottaker

import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle.FULLMAKT
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle.INSTITUSJON
import no.nav.familie.klage.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import java.util.UUID

object BrevmottakerUtil {
    fun validerBrevmottakere(
        behandlingId: UUID,
        brevmottakere: Brevmottakere,
    ) {
        validerUnikeBrevmottakere(behandlingId, brevmottakere)
        validerMinimumEnMottaker(behandlingId, brevmottakere)
    }

    private fun validerUnikeBrevmottakere(
        behandlingId: UUID,
        brevmottakere: Brevmottakere,
    ) {
        val personmottakerIdentifikatorer =
            brevmottakere.personer.map {
                when (it) {
                    is BrevmottakerPersonMedIdent -> it.personIdent
                    is BrevmottakerPersonUtenIdent -> it.id.toString()
                }
            }
        brukerfeilHvisIkke(personmottakerIdentifikatorer.distinct().size == personmottakerIdentifikatorer.size) {
            "En person kan bare legges til én gang som brevmottaker for behandling $behandlingId."
        }

        val organisasjonsmottakerIdenter = brevmottakere.organisasjoner.map { it.organisasjonsnummer }
        brukerfeilHvisIkke(organisasjonsmottakerIdenter.distinct().size == organisasjonsmottakerIdenter.size) {
            "En organisasjon kan bare legges til én gang som brevmottaker for behandling $behandlingId."
        }
    }

    private fun validerMinimumEnMottaker(
        behandlingId: UUID,
        brevmottakere: Brevmottakere,
    ) {
        feilHvis(brevmottakere.personer.isEmpty() && brevmottakere.organisasjoner.isEmpty()) {
            "Må ha minimum en brevmottaker for behandling $behandlingId."
        }
    }

    fun validerBrevmottakerForInstitusjonssak(mottakere: Brevmottakere) {
        validerAtInstitusjonErBrevmottaker(mottakere)
        validerAtBareInstitusjonOgFullmaktErBrevmottaker(mottakere)
    }

    private fun validerAtInstitusjonErBrevmottaker(mottakere: Brevmottakere) {
        feilHvis(mottakere.organisasjoner.none { it.mottakerRolle == INSTITUSJON }) {
            "I institusjonssaker skal én brevmottaker ha rollen $INSTITUSJON"
        }
    }

    private fun validerAtBareInstitusjonOgFullmaktErBrevmottaker(mottakere: Brevmottakere) {
        val harUgyldigBrevmottaker = mottakere.tilListe().any { it.mottakerRolle !in setOf(INSTITUSJON, FULLMAKT) }
        feilHvis(harUgyldigBrevmottaker) {
            "I institusjonssaker kan brevmottakere kun ha rollene $INSTITUSJON og $FULLMAKT"
        }
    }
}
