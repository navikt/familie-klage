package no.nav.familie.klage.brevmottaker.domain

private const val LANDKODE_NO = "NO"

sealed interface NyBrevmottaker

sealed interface NyBrevmottakerPerson : NyBrevmottaker {
    val mottakerRolle: MottakerRolle
    val navn: String
}

data class NyBrevmottakerPersonUtenIdent(
    override val mottakerRolle: MottakerRolle,
    override val navn: String,
    val adresselinje1: String,
    val adresselinje2: String? = null,
    val postnummer: String?,
    val poststed: String?,
    val landkode: String,
) : NyBrevmottakerPerson {
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
        if (landkode == LANDKODE_NO) {
            if (postnummer.isNullOrBlank()) {
                throw IllegalStateException("Når landkode er $LANDKODE_NO (Norge) må postnummer være satt.")
            }
            if (poststed.isNullOrBlank()) {
                throw IllegalStateException("Når landkode er $LANDKODE_NO (Norge) må poststed være satt.")
            }
            if (postnummer.length != 4 || !postnummer.all { it.isDigit() }) {
                throw IllegalStateException("Postnummer må være 4 siffer.")
            }
            if (mottakerRolle == MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE) {
                throw IllegalStateException("Bruker med utenlandsk adresse kan ikke ha landkode $LANDKODE_NO.")
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

data class NyBrevmottakerPersonMedIdent(
    val personIdent: String,
    override val mottakerRolle: MottakerRolle,
    override val navn: String,
) : NyBrevmottakerPerson {
    init {
        if (personIdent.isBlank()) {
            throw IllegalStateException("Personident kan ikke være blank.")
        }
        if (navn.isBlank()) {
            throw IllegalStateException("Navn kan ikke være blank.")
        }
    }
}

data class NyBrevmottakerOrganisasjon(
    val organisasjonsnummer: String,
    val organisasjonsnavn: String,
    val navnHosOrganisasjon: String,
) : NyBrevmottaker
