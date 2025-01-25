package no.nav.familie.klage.brev.dto

import no.nav.familie.klage.brev.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brev.domain.BrevmottakerPerson
import no.nav.familie.klage.brev.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brev.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brev.domain.Brevmottakere
import no.nav.familie.klage.infrastruktur.exception.ApiFeil

data class BrevmottakereDto(
    val personer: List<BrevmottakerPerson>,
    val organisasjoner: List<BrevmottakerOrganisasjon>,
) {
    fun valider() {
        if (personer.isEmpty() && organisasjoner.isEmpty()) {
            throw ApiFeil.badRequest("Må ha minimum en brevmottaker.")
        }

        val personmottakerIdentifikatorer = personer.map {
            when (it) {
                is BrevmottakerPersonMedIdent -> it.personIdent
                is BrevmottakerPersonUtenIdent -> it.id.toString()
            }
        }
        if (personmottakerIdentifikatorer.distinct().size != personmottakerIdentifikatorer.size) {
            throw ApiFeil.badRequest("En person kan bare legges til en gang som brevmottaker.")
        }

        val organisasjonsmottakerIdenter = organisasjoner.map { it.organisasjonsnummer }
        if (organisasjonsmottakerIdenter.distinct().size != organisasjonsmottakerIdenter.size) {
            throw ApiFeil.badRequest("En organisasjon kan bare legges til en gang som brevmottaker.")
        }
    }
}

fun Brevmottakere.tilDto() = BrevmottakereDto(
    personer = this.personer,
    organisasjoner = this.organisasjoner,
)

fun BrevmottakereDto.tilDomene() = Brevmottakere(
    personer = this.personer,
    organisasjoner = this.organisasjoner,
)
