package no.nav.familie.klage.brevmottaker.domain

import no.nav.familie.klage.brevmottaker.domain.MottakerRolle.FULLMAKT
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle.INSTITUSJON
import no.nav.familie.kontrakter.felles.Organisasjonsnummer

private const val LANDKODE_NO = "NO"

sealed interface NyBrevmottaker {
    val mottakerRolle: MottakerRolle?
}

sealed interface NyBrevmottakerPerson : NyBrevmottaker {
    val navn: String
    override val mottakerRolle: MottakerRolle
}

data class NyBrevmottakerPersonUtenIdent(
    val adresselinje1: String,
    val adresselinje2: String? = null,
    val postnummer: String?,
    val poststed: String?,
    val landkode: String,
    override val navn: String,
    override val mottakerRolle: MottakerRolle,
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
        require(mottakerRolle != INSTITUSJON) { "Mottakerrolle kan ikke være institusjon." }
    }
}

data class NyBrevmottakerPersonMedIdent(
    val personIdent: String,
    override val navn: String,
    override val mottakerRolle: MottakerRolle,
) : NyBrevmottakerPerson {
    init {
        require(personIdent.isNotBlank()) { "Personident kan ikke være blank." }
        require(navn.isNotBlank()) { "Navn kan ikke være blank." }
        require(mottakerRolle != INSTITUSJON) { "Mottakerrolle kan ikke være institusjon." }
    }
}

data class NyBrevmottakerOrganisasjon(
    val organisasjonsnummer: String,
    val organisasjonsnavn: String,
    val navnHosOrganisasjon: String? = null,
    override val mottakerRolle: MottakerRolle? = null,
) : NyBrevmottaker {
    init {
        Organisasjonsnummer(organisasjonsnummer)
        require(organisasjonsnavn.isNotBlank()) { "Organisasjonsnavn kan ikke være blank." }
        mottakerRolle?.let {
            require(mottakerRolle in setOf(INSTITUSJON, FULLMAKT)) {
                "Brevmottakerorganisasjon kan ikke ha mottakerrolle $mottakerRolle."
            }
        }
    }
}
