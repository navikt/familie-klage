package no.nav.familie.klage.brev.mottaker

import no.nav.familie.klage.brev.domain.Mottakertype
import no.nav.familie.klage.infrastruktur.exception.Feil
import java.util.UUID

data class BrevmottakerDto(
    val id: UUID,
    val behandlingId: UUID,
    val mottakertype: Mottakertype,
    val navn: String,
    val adresselinje1: String,
    val adresselinje2: String?,
    val postnummer: String?,
    val poststed: String?,
    val landkode: String,
) {
    fun valider() {
        if (landkode.length != 2) {
            throw Feil("Ugyldig landkode: $landkode")
        }
        if (navn.isBlank()) {
            throw Feil("Navn kan ikke være tomt")
        }
        if (adresselinje1.isBlank()) {
            throw Feil("Adresselinje1 kan ikke være tomt")
        }
        if (landkode == "NO" && (postnummer.isNullOrBlank() || poststed.isNullOrBlank())) {
            throw Feil("Når landkode er NO (Norge) må postnummer og poststed være satt")
        }
        if (landkode != "NO" && (!postnummer.isNullOrBlank() || !poststed.isNullOrBlank())) {
            throw Feil("Ved utenlandsk landkode må postnummer og poststed via adresselinje1")
        }
        if (postnummer != null && (postnummer.length != 4 || postnummer.any { !it.isDigit() })) {
            throw Feil("Postnummer må være 4 siffer")
        }
        if (landkode == "NO" && mottakertype == Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE) {
            throw Feil("Bruker med utenlandsk adresse kan ikke ha landkode NO")
        }
    }
}

fun BrevmottakerDto.mapTilBrevmottaker(): Brevmottaker =
    Brevmottaker(
        id = this.id,
        behandlingId = this.behandlingId,
        mottakerType = this.mottakertype,
        navn = this.navn,
        adresselinje1 = this.adresselinje1,
        adresselinje2 = this.adresselinje2,
        postnummer = this.postnummer,
        poststed = this.poststed,
        landkode = this.landkode,
    )
