package no.nav.familie.klage.personopplysninger.pdl

import no.nav.familie.klage.infrastruktur.exception.Feil
import org.springframework.http.HttpStatus

fun Navn.visningsnavn(): String =
    if (mellomnavn == null) {
        "$fornavn $etternavn"
    } else {
        "$fornavn $mellomnavn $etternavn"
    }

fun Personnavn.visningsnavn(): String =
    if (mellomnavn == null) {
        "$fornavn $etternavn"
    } else {
        "$fornavn $mellomnavn $etternavn"
    }

fun List<Navn>.gjeldende(): Navn {
    val masterOgNavn =
        this
            .filter { !it.metadata.historisk }
            .groupBy { Master.fraVerdi(it.metadata.master) }
            .minByOrNull { it.key.prioritet }

    if (masterOgNavn == null) {
        throw Feil(
            message = "Fant ingen gjeldende navn for personen. Forventet minst ett.",
            frontendFeilmelding = "Fant ikke det gjeldende navnet til personen.",
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
        )
    }

    val master = masterOgNavn.key
    val navn = masterOgNavn.value

    if (navn.size > 1) {
        throw Feil(
            message = "Fant flere (${navn.size}) gjeldende navn for personen med master $master. Forventet nøyaktig ett.",
            frontendFeilmelding = "Fant flere gjeldende navn til personen.",
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
        )
    }

    return navn.single()
}

fun List<Dødsfall>.gjeldende(): Dødsfall? = this.firstOrNull()

fun List<Folkeregisterpersonstatus>.gjeldende(): Folkeregisterpersonstatus? = this.find { !it.metadata.historisk }

fun List<Adressebeskyttelse>.gjeldende(): Adressebeskyttelse? = this.find { !it.metadata.historisk }

fun List<Kjønn>.gjeldende(): Kjønn = this.single()

fun List<Fødselsdato>.gjeldende(): Fødselsdato? = this.firstOrNull()

fun PdlIdenter.identer(): Set<String> = this.identer.map { it.ident }.toSet()
