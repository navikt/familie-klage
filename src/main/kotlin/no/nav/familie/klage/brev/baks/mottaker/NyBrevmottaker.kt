package no.nav.familie.klage.brev.baks.mottaker

data class NyBrevmottaker(
    val mottakertype: Mottakertype,
    val navn: String,
    val adresselinje1: String,
    val adresselinje2: String?,
    val postnummer: String?,
    val poststed: String?,
    val landkode: String,
) {
    init {
        if (landkode.length != 2) {
            throw IllegalStateException("Ugyldig landkode: $landkode.")
        }
        if (navn.isBlank()) {
            throw IllegalStateException("Navn kan ikke være tomt.")
        }
        if (adresselinje1.isBlank()) {
            throw IllegalStateException("Adresselinje1 kan ikke være tomt.")
        }
        if (landkode == "NO") {
            if (postnummer.isNullOrBlank()) {
                throw IllegalStateException("Når landkode er NO (Norge) må postnummer være satt.")
            }
            if (poststed.isNullOrBlank()) {
                throw IllegalStateException("Når landkode er NO (Norge) må poststed være satt.")
            }
            if (postnummer.length != 4 || !postnummer.all { it.isDigit() }) {
                throw IllegalStateException("Postnummer må være 4 siffer.")
            }
            if (mottakertype == Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE) {
                throw IllegalStateException("Bruker med utenlandsk adresse kan ikke ha landkode NO.")
            }
        } else {
            if (!postnummer.isNullOrBlank()) {
                throw IllegalStateException("Ved utenlandsk landkode må postnummer settes i adresselinje 1.")
            }
            if (!poststed.isNullOrBlank()) {
                throw IllegalStateException("Ved utenlandsk landkode må poststed settes i adresselinje 1.")
            }
        }
    }
}
