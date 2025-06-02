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
        require(landkode.length == 2) { "Ugyldig landkode: $landkode." }
        require(navn.isNotBlank()) { "Navn kan ikke være tomt." }
        require(adresselinje1.isNotBlank()) { "Adresselinje1 kan ikke være tomt." }
        if (landkode == LANDKODE_NO) {
            require(!postnummer.isNullOrBlank()) { "Når landkode er $LANDKODE_NO (Norge) må postnummer være satt." }
            require(!poststed.isNullOrBlank()) { "Når landkode er $LANDKODE_NO (Norge) må poststed være satt." }
            require(postnummer.length == 4 && postnummer.all { it.isDigit() }) { "Postnummer må være 4 siffer." }
            require(mottakerRolle != MottakerRolle.BRUKER_MED_UTENLANDSK_ADRESSE) { "Bruker med utenlandsk adresse kan ikke ha landkode $LANDKODE_NO." }
        } else {
            require(postnummer.isNullOrBlank()) { "Ved utenlandsk landkode må postnummer settes i adresselinje 1." }
            require(poststed.isNullOrBlank()) { "Ved utenlandsk landkode må poststed settes i adresselinje 1." }
        }
    }
}

data class NyBrevmottakerPersonMedIdent(
    val personIdent: String,
    override val mottakerRolle: MottakerRolle,
    override val navn: String,
) : NyBrevmottakerPerson {
    init {
        require(personIdent.isNotBlank()) { "Personident kan ikke være blank." }
        require(navn.isNotBlank()) { "Navn kan ikke være blank." }
    }
}

data class NyBrevmottakerOrganisasjon(
    val organisasjonsnummer: String,
    val organisasjonsnavn: String,
    val navnHosOrganisasjon: String,
) : NyBrevmottaker
