package no.nav.familie.klage.testutil

import no.nav.familie.klage.brev.domain.MottakerRolle
import no.nav.familie.klage.brevmottaker.NyBrevmottakerOrganisasjonDto
import no.nav.familie.klage.brevmottaker.NyBrevmottakerPersonMedIdentDto
import no.nav.familie.klage.brevmottaker.NyBrevmottakerPersonUtenIdentDto

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

    fun lagNyBrevmottakerPersonMedIdentDto(
        personIdent: String = "23097825289",
        mottakerRolle: MottakerRolle = MottakerRolle.FULLMAKT,
        navn: String = "Navn Navnesen",
    ): NyBrevmottakerPersonMedIdentDto {
        return NyBrevmottakerPersonMedIdentDto(
            personIdent = personIdent,
            mottakerRolle = mottakerRolle,
            navn = navn,
        )
    }

    fun lagNyBrevmottakerOrganisasjonDto(
        organisasjonsnummer: String = "123",
        organisasjonsnavn: String = "Orgnavn",
        navnHosOrganisasjon: String = "navnHosOrganisasjon",
    ): NyBrevmottakerOrganisasjonDto {
        return NyBrevmottakerOrganisasjonDto(
            organisasjonsnummer = organisasjonsnummer,
            organisasjonsnavn = organisasjonsnavn,
            navnHosOrganisasjon = navnHosOrganisasjon,
        )
    }
}
