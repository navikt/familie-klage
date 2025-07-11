package no.nav.familie.klage.testutil

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.dto.BehandlingDto
import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.brev.dto.AvsnittDto
import no.nav.familie.klage.brev.dto.FritekstBrevRequestDto
import no.nav.familie.klage.brev.dto.SignaturDto
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.brevmottaker.dto.NyBrevmottakerOrganisasjonDto
import no.nav.familie.klage.brevmottaker.dto.NyBrevmottakerPersonMedIdentDto
import no.nav.familie.klage.brevmottaker.dto.NyBrevmottakerPersonUtenIdentDto
import no.nav.familie.klage.brevmottaker.dto.SlettbarBrevmottakerPersonUtenIdentDto
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.personopplysninger.dto.Adressebeskyttelse
import no.nav.familie.klage.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.familie.klage.personopplysninger.dto.FullmaktDto
import no.nav.familie.klage.personopplysninger.dto.Kjønn
import no.nav.familie.klage.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.klage.personopplysninger.dto.VergemålDto
import no.nav.familie.kontrakter.felles.Regelverk
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
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
        mottakerRolle: MottakerRolle = MottakerRolle.BRUKER,
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

    fun lagPersonopplysningerDto(
        personIdent: String = "12345678903",
        navn: String = "Navn Navnesen",
        kjønn: Kjønn = Kjønn.MANN,
        adressebeskyttelse: Adressebeskyttelse? = null,
        folkeregisterpersonstatus: Folkeregisterpersonstatus? = null,
        dødsdato: LocalDate? = null,
        fullmakt: List<FullmaktDto> = emptyList(),
        egenAnsatt: Boolean = false,
        vergemål: List<VergemålDto> = emptyList(),
    ) = PersonopplysningerDto(
        personIdent = personIdent,
        navn = navn,
        kjønn = kjønn,
        adressebeskyttelse = adressebeskyttelse,
        folkeregisterpersonstatus = folkeregisterpersonstatus,
        dødsdato = dødsdato,
        fullmakt = fullmakt,
        egenAnsatt = egenAnsatt,
        vergemål = vergemål,
    )

    fun lagVergemålDto(
        embete: String? = null,
        type: String? = null,
        motpartsPersonident: String? = null,
        navn: String? = null,
        omfang: String? = null,
    ): VergemålDto =
        VergemålDto(
            embete = embete,
            type = type,
            motpartsPersonident = motpartsPersonident,
            navn = navn,
            omfang = omfang,
        )

    fun lagFullmaktDto(
        gyldigFraOgMed: LocalDate = LocalDate.now().minusDays(1),
        gyldigTilOgMed: LocalDate? = LocalDate.now(),
        navn: String? = "Navn Navnesen",
        motpartsPersonident: String = "30987654321",
        områder: List<String> = emptyList(),
    ): FullmaktDto =
        FullmaktDto(
            gyldigFraOgMed = gyldigFraOgMed,
            gyldigTilOgMed = gyldigTilOgMed,
            navn = navn,
            motpartsPersonident = motpartsPersonident,
            områder = områder,
        )

    fun lagSignaturDto(
        navn: String = "Enhetsnavn",
        enhet: String = "0001",
    ): SignaturDto =
        SignaturDto(
            navn = navn,
            enhet = enhet,
        )

    fun lagPåklagetVedtakDto(
        eksternFagsystemBehandlingId: String? = UUID.randomUUID().toString(),
        internKlagebehandlingId: String? = UUID.randomUUID().toString(),
        påklagetVedtakstype: PåklagetVedtakstype = PåklagetVedtakstype.VEDTAK,
        fagsystemVedtak: FagsystemVedtak? = null,
        manuellVedtaksdato: LocalDate? = null,
        regelverk: Regelverk? = null,
    ): PåklagetVedtakDto =
        PåklagetVedtakDto(
            eksternFagsystemBehandlingId = eksternFagsystemBehandlingId,
            internKlagebehandlingId = internKlagebehandlingId,
            påklagetVedtakstype = påklagetVedtakstype,
            fagsystemVedtak = fagsystemVedtak,
            manuellVedtaksdato = manuellVedtaksdato,
            regelverk = regelverk,
        )

    fun lagBehandlingDto(
        fagsak: Fagsak = DomainUtil.fagsak(),
        behandling: Behandling = DomainUtil.behandling(fagsak),
    ): BehandlingDto =
        BehandlingDto(
            id = behandling.id,
            fagsakId = behandling.fagsakId,
            steg = behandling.steg,
            status = behandling.status,
            sistEndret = behandling.sporbar.endret.endretTid,
            resultat = behandling.resultat,
            opprettet = behandling.sporbar.opprettetTid,
            vedtaksdato = behandling.vedtakDato,
            stønadstype = fagsak.stønadstype,
            klageinstansResultat = emptyList(),
            påklagetVedtak = lagPåklagetVedtakDto(internKlagebehandlingId = behandling.id.toString()),
            eksternFagsystemFagsakId = fagsak.eksternId,
            fagsystem = fagsak.fagsystem,
            klageMottatt = behandling.klageMottatt,
            fagsystemRevurdering = behandling.fagsystemRevurdering,
            årsak = behandling.årsak,
            behandlendeEnhet = behandling.behandlendeEnhet,
        )

    fun lagFritekstBrevRequestDto(
        overskrift: String = "Overskrift",
        avsnitt: List<AvsnittDto> = emptyList(),
        personIdent: String = "12345678903",
        navn: String = "Navn Navnesen",
    ): FritekstBrevRequestDto =
        FritekstBrevRequestDto(
            overskrift = overskrift,
            avsnitt = avsnitt,
            personIdent = personIdent,
            navn = navn,
        )
}
