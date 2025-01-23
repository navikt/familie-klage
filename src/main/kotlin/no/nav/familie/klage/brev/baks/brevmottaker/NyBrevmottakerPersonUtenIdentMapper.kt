package no.nav.familie.klage.brev.baks.brevmottaker

object NyBrevmottakerPersonUtenIdentMapper {
    fun tilDomene(nyBrevmottakerPersonUtenIdentDto: NyBrevmottakerPersonUtenIdentDto): NyBrevmottakerPersonUtenIdent {
        return NyBrevmottakerPersonUtenIdent(
            mottakerRolle = nyBrevmottakerPersonUtenIdentDto.mottakerRolle,
            navn = nyBrevmottakerPersonUtenIdentDto.navn,
            adresselinje1 = nyBrevmottakerPersonUtenIdentDto.adresselinje1,
            adresselinje2 = nyBrevmottakerPersonUtenIdentDto.adresselinje2,
            postnummer = nyBrevmottakerPersonUtenIdentDto.postnummer,
            poststed = nyBrevmottakerPersonUtenIdentDto.poststed,
            landkode = nyBrevmottakerPersonUtenIdentDto.landkode,
        )
    }
}
