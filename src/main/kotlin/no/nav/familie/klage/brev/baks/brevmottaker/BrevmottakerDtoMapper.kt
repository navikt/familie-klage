package no.nav.familie.klage.brev.baks.brevmottaker

object BrevmottakerDtoMapper {
    fun tilDto(brevmottaker: Brevmottaker): BrevmottakerDto {
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
