package no.nav.familie.klage.testutil

import no.nav.familie.klage.brev.baks.brevmottaker.Mottakertype
import no.nav.familie.klage.brev.baks.brevmottaker.NyBrevmottakerDto

object DtoTestUtil {
    fun lagNyBrevmottakerDto(
        mottakertype: Mottakertype = Mottakertype.FULLMEKTIG,
        navn: String = "Navn Navnesen",
        adresselinje1: String = "Adresselinje 1",
        adresselinje2: String? = null,
        postnummer: String? = "0010",
        poststed: String? = "Oslo",
        landkode: String = "NO",
    ): NyBrevmottakerDto {
        return NyBrevmottakerDto(
            mottakertype = mottakertype,
            navn = navn,
            adresselinje1 = adresselinje1,
            adresselinje2 = adresselinje2,
            postnummer = postnummer,
            poststed = poststed,
            landkode = landkode,
        )
    }
}
