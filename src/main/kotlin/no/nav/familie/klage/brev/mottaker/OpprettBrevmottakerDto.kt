package no.nav.familie.klage.brev.mottaker

import no.nav.familie.klage.brev.mottaker.Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE
import no.nav.familie.klage.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.klage.infrastruktur.exception.brukerfeilHvisIkke
import java.util.UUID

data class OpprettBrevmottakerDto(
    val mottakertype: Mottakertype,
    val navn: String,
    val adresselinje1: String,
    val adresselinje2: String?,
    val postnummer: String?,
    val poststed: String?,
    val landkode: String,
) {
    // TODO : Her burde man kanskje ikke bruke HTTP spesifikke exceptions?
    //  Kanskje en mer generisk exception er bedre, som så blir håndtert via f.eks. ApiExceptionHandler.
    init {
        brukerfeilHvisIkke(landkode.length == 2) { "Ugyldig landkode: $landkode" }
        brukerfeilHvis(navn.isBlank()) { "Navn kan ikke være tomt" }
        brukerfeilHvis(adresselinje1.isBlank()) { "Adresselinje1 kan ikke være tomt" }
        if (landkode == "NO") {
            brukerfeilHvis(postnummer.isNullOrBlank()) {
                "Når landkode er NO (Norge) må postnummer være satt"
            }
            brukerfeilHvis(poststed.isNullOrBlank()) {
                "Når landkode er NO (Norge) må poststed være satt"
            }
            brukerfeilHvisIkke(postnummer.length == 4 && postnummer.all { it.isDigit() }) {
                "Postnummer må være 4 siffer"
            }
            brukerfeilHvis(mottakertype == BRUKER_MED_UTENLANDSK_ADRESSE) {
                "Bruker med utenlandsk adresse kan ikke ha landkode NO"
            }
        } else {
            brukerfeilHvisIkke(postnummer.isNullOrBlank() && poststed.isNullOrBlank()) {
                "Ved utenlandsk landkode må postnummer og poststed settes i adresselinje1"
            }
        }
    }
}

fun OpprettBrevmottakerDto.mapTilBrevmottaker(behandlingId: UUID): Brevmottaker =
    Brevmottaker(
        id = UUID.randomUUID(),
        behandlingId = behandlingId,
        mottakertype = this.mottakertype,
        navn = this.navn,
        adresselinje1 = this.adresselinje1,
        adresselinje2 = this.adresselinje2,
        postnummer = this.postnummer,
        poststed = this.poststed,
        landkode = this.landkode,
    )
