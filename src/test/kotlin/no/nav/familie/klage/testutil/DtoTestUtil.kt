package no.nav.familie.klage.testutil

import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.brevmottaker.dto.NyBrevmottakerOrganisasjonDto
import no.nav.familie.klage.brevmottaker.dto.NyBrevmottakerPersonMedIdentDto
import no.nav.familie.klage.brevmottaker.dto.NyBrevmottakerPersonUtenIdentDto
import no.nav.familie.klage.brevmottaker.dto.SlettbarBrevmottakerPersonUtenIdentDto
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import java.time.LocalDate
import java.util.UUID

object DtoTestUtil {
    fun lagNyBrevmottakerPersonUtenIdentDto(
        mottakerRolle: MottakerRolle = MottakerRolle.FULLMAKT,
        navn: String = "Navn Navnesen",
        adresselinje1: String = "Adresselinje 1",
        adresselinje2: String? = null,
        postnummer: String? = "0010",
        poststed: String? = "Oslo",
        landkode: String = "NO",
    ): NyBrevmottakerPersonUtenIdentDto =
        NyBrevmottakerPersonUtenIdentDto(
            mottakerRolle = mottakerRolle,
            navn = navn,
            adresselinje1 = adresselinje1,
            adresselinje2 = adresselinje2,
            postnummer = postnummer,
            poststed = poststed,
            landkode = landkode,
        )

    fun lagNyBrevmottakerPersonMedIdentDto(
        personIdent: String = "23097825289",
        mottakerRolle: MottakerRolle = MottakerRolle.FULLMAKT,
        navn: String = "Navn Navnesen",
    ): NyBrevmottakerPersonMedIdentDto =
        NyBrevmottakerPersonMedIdentDto(
            personIdent = personIdent,
            mottakerRolle = mottakerRolle,
            navn = navn,
        )

    fun lagNyBrevmottakerOrganisasjonDto(
        organisasjonsnummer: String = "123",
        organisasjonsnavn: String = "Orgnavn",
        navnHosOrganisasjon: String = "navnHosOrganisasjon",
    ): NyBrevmottakerOrganisasjonDto =
        NyBrevmottakerOrganisasjonDto(
            organisasjonsnummer = organisasjonsnummer,
            organisasjonsnavn = organisasjonsnavn,
            navnHosOrganisasjon = navnHosOrganisasjon,
        )

    fun lagSlettbarBrevmottakerPersonUtenIdentDto(id: UUID = UUID.randomUUID()): SlettbarBrevmottakerPersonUtenIdentDto =
        SlettbarBrevmottakerPersonUtenIdentDto(
            id,
        )

    fun lagOpprettKlagebehandlingRequest(
        ident: String = "123",
        stønadstype: Stønadstype = Stønadstype.BARNETRYGD,
        eksternFagsakId: String = "321",
        fagsystem: Fagsystem = Fagsystem.BA,
        klageMottatt: LocalDate = LocalDate.now(),
        behandlendeEnhet: String = "1000",
        klageGjelderTilbakekreving: Boolean = false,
        behandlingsårsak: Klagebehandlingsårsak = Klagebehandlingsårsak.ORDINÆR,
    ): OpprettKlagebehandlingRequest =
        OpprettKlagebehandlingRequest(
            ident,
            stønadstype,
            eksternFagsakId,
            fagsystem,
            klageMottatt,
            behandlendeEnhet,
            klageGjelderTilbakekreving,
            behandlingsårsak,
        )
}
