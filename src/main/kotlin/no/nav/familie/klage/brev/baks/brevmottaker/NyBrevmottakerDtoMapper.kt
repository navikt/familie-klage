package no.nav.familie.klage.brev.baks.brevmottaker

object NyBrevmottakerDtoMapper {
    fun tilDomene(nyBrevmottakerDto: NyBrevmottakerDto): NyBrevmottaker {
        return NyBrevmottaker(
            mottakertype = nyBrevmottakerDto.mottakertype,
            navn = nyBrevmottakerDto.navn,
            adresselinje1 = nyBrevmottakerDto.adresselinje1,
            adresselinje2 = nyBrevmottakerDto.adresselinje2,
            postnummer = nyBrevmottakerDto.postnummer,
            poststed = nyBrevmottakerDto.poststed,
            landkode = nyBrevmottakerDto.landkode,
        )
    }
}
