package no.nav.familie.klage.testutil

import no.nav.familie.klage.brev.brevmottaker.baks.NyBrevmottakerPersonUtenIdentDto
import no.nav.familie.klage.brev.domain.MottakerRolle

object DtoTestUtil {
    fun lagNyBrevmottakerPersonUtenIdentDto(
        mottakerRolle: MottakerRolle = MottakerRolle.FULLMAKT,
        navn: String = "Navn Navnesen",
        adresselinje1: String = "Adresselinje 1",
        adresselinje2: String? = null,
        postnummer: String? = "0010",
        poststed: String? = "Oslo",
        landkode: String = "NO",
    ): NyBrevmottakerPersonUtenIdentDto {
        return NyBrevmottakerPersonUtenIdentDto(
            mottakerRolle = mottakerRolle,
            navn = navn,
            adresselinje1 = adresselinje1,
            adresselinje2 = adresselinje2,
            postnummer = postnummer,
            poststed = poststed,
            landkode = landkode,
        )
    }
}
