package no.nav.familie.klage.brev.brevmottaker

import no.nav.familie.klage.brev.domain.MottakerRolle
import no.nav.familie.klage.infrastruktur.exception.ApiFeil

private const val LANDKODE_NO = "NO"

data class NyBrevmottakerPersonUtenIdentDto(
    val mottakerRolle: MottakerRolle,
    val navn: String,
    val adresselinje1: String,
    val adresselinje2: String?,
    val postnummer: String?,
    val poststed: String?,
    val landkode: String,
) {
    fun valider() {
        if (mottakerRolle == MottakerRolle.BRUKER) {
            throw ApiFeil.badRequest("Det er ikke mulig å sette ${MottakerRolle.BRUKER} for saksbehandler.")
        }
        if (landkode.length != 2) {
            throw ApiFeil.badRequest("Ugyldig landkode: $landkode.")
        }
        if (navn.isBlank()) {
            throw ApiFeil.badRequest("Navn kan ikke være tomt.")
        }
        if (adresselinje1.isBlank()) {
            throw ApiFeil.badRequest("Adresselinje 1 kan ikke være tomt.")
        }
        if (landkode == LANDKODE_NO) {
            if (postnummer.isNullOrBlank()) {
                throw ApiFeil.badRequest("Når landkode er $LANDKODE_NO (Norge) må postnummer være satt.")
            }
            if (poststed.isNullOrBlank()) {
                throw ApiFeil.badRequest("Når landkode er $LANDKODE_NO (Norge) må poststed være satt.")
            }
            if (postnummer.length != 4 || !postnummer.all { it.isDigit() }) {
                throw ApiFeil.badRequest("Postnummer må være 4 siffer.")
            }
            if (mottakerRolle == MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE) {
                throw ApiFeil.badRequest("Bruker med utenlandsk adresse kan ikke ha landkode $LANDKODE_NO.")
            }
        } else {
            if (!postnummer.isNullOrBlank()) {
                throw ApiFeil.badRequest("Ved utenlandsk landkode må postnummer settes i adresselinje 1.")
            }
            if (!poststed.isNullOrBlank()) {
                throw ApiFeil.badRequest("Ved utenlandsk landkode må poststed settes i adresselinje 1.")
            }
        }
    }
}
