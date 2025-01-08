package no.nav.familie.klage.brev.baks.brevmottaker

import java.util.UUID

data class BrevmottakerDto(
    val id: UUID,
    val mottakertype: Mottakertype,
    val navn: String,
    val adresselinje1: String,
    val adresselinje2: String?,
    val postnummer: String?,
    val poststed: String?,
    val landkode: String,
) {
    companion object Fabrikk {
        fun opprett(brevmottaker: Brevmottaker): BrevmottakerDto {
            return BrevmottakerDto(
                id = brevmottaker.id,
                mottakertype = brevmottaker.mottakertype,
                navn = brevmottaker.navn,
                adresselinje1 = brevmottaker.adresselinje1,
                adresselinje2 = brevmottaker.adresselinje2,
                postnummer = brevmottaker.postnummer,
                poststed = brevmottaker.poststed,
                landkode = brevmottaker.landkode,
            )
        }
    }
}
