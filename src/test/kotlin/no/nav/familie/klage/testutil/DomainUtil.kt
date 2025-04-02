package no.nav.familie.klage.testutil

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.FagsystemRevurdering
import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.domain.BrevmottakereJournalposter
import no.nav.familie.klage.brev.dto.AvsnittDto
import no.nav.familie.klage.brev.dto.FritekstBrevRequestDto
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPerson
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.brevmottaker.domain.NyBrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.NyBrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.NyBrevmottakerPersonUtenIdent
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.fagsak.domain.FagsakDomain
import no.nav.familie.klage.fagsak.domain.FagsakPerson
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.felles.domain.Fil
import no.nav.familie.klage.felles.domain.Sporbar
import no.nav.familie.klage.felles.domain.SporbarUtils
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.formkrav.domain.FormkravFristUnntak
import no.nav.familie.klage.infrastruktur.config.DatabaseConfiguration
import no.nav.familie.klage.kabal.domain.KlageinstansResultat
import no.nav.familie.klage.personopplysninger.dto.Adressebeskyttelse
import no.nav.familie.klage.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.familie.klage.personopplysninger.dto.FullmaktDto
import no.nav.familie.klage.personopplysninger.dto.Kjønn
import no.nav.familie.klage.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.klage.personopplysninger.dto.VergemålDto
import no.nav.familie.klage.vurdering.domain.Hjemmel
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.klage.vurdering.domain.Vurdering
import no.nav.familie.klage.vurdering.dto.VurderingDto
import no.nav.familie.kontrakter.felles.Regelverk
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentstatus
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariant
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.kontrakter.felles.journalpost.RelevantDato
import no.nav.familie.kontrakter.felles.klage.BehandlingEventType
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import no.nav.familie.kontrakter.felles.klage.KlageinstansUtfall
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.kontrakter.felles.klage.Årsak
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

object DomainUtil {
    fun fagsakDomain(
        id: UUID = UUID.randomUUID(),
        stønadstype: Stønadstype = Stønadstype.OVERGANGSSTØNAD,
        personId: UUID = UUID.randomUUID(),
        fagsystem: Fagsystem = Fagsystem.EF,
        eksternId: String = Random.nextInt().toString(),
    ): FagsakDomain =
        FagsakDomain(
            id = id,
            fagsakPersonId = personId,
            stønadstype = stønadstype,
            eksternId = eksternId,
            fagsystem = fagsystem,
        )

    fun FagsakDomain.tilFagsak(personIdent: String = "11223344551") =
        this.tilFagsakMedPerson(setOf(PersonIdent(ident = personIdent)))

    fun behandling(
        fagsak: Fagsak = fagsak(),
        id: UUID = UUID.randomUUID(),
        eksternBehandlingId: UUID = UUID.randomUUID(),
        påklagetVedtak: PåklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.IKKE_VALGT, null),
        klageMottatt: LocalDate = LocalDate.now(),
        status: BehandlingStatus = BehandlingStatus.OPPRETTET,
        steg: StegType = StegType.FORMKRAV,
        behandlendeEnhet: String = "4489",
        resultat: BehandlingResultat = BehandlingResultat.IKKE_SATT,
        vedtakDato: LocalDateTime? = null,
        henlagtÅrsak: HenlagtÅrsak? = null,
        sporbar: Sporbar = Sporbar(),
        fagsystemRevurdering: FagsystemRevurdering? = null,
        årsak: Klagebehandlingsårsak = Klagebehandlingsårsak.ORDINÆR,
    ): Behandling =
        Behandling(
            id = id,
            eksternBehandlingId = eksternBehandlingId,
            fagsakId = fagsak.id,
            påklagetVedtak = påklagetVedtak,
            klageMottatt = klageMottatt,
            status = status,
            steg = steg,
            behandlendeEnhet = behandlendeEnhet,
            resultat = resultat,
            henlagtÅrsak = henlagtÅrsak,
            vedtakDato = vedtakDato,
            sporbar = sporbar,
            fagsystemRevurdering = fagsystemRevurdering,
            årsak = årsak,
        )

    fun vurdering(
        behandlingId: UUID,
        vedtak: Vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
        hjemmel: Hjemmel? = Hjemmel.FT_FEMTEN_FEM,
        innstillingKlageinstans: String? = if (vedtak == Vedtak.OPPRETTHOLD_VEDTAK) "En begrunnelse" else null,
        dokumentasjonOgUtredning: String? = if (vedtak == Vedtak.OPPRETTHOLD_VEDTAK) "En begrunnelse" else null,
        spørsmåletISaken: String? = if (vedtak == Vedtak.OPPRETTHOLD_VEDTAK) "En begrunnelse" else null,
        aktuelleRettskilder: String? = if (vedtak == Vedtak.OPPRETTHOLD_VEDTAK) "En begrunnelse" else null,
        klagersAnførsler: String? = if (vedtak == Vedtak.OPPRETTHOLD_VEDTAK) "En begrunnelse" else null,
        vurderingAvKlagen: String? = if (vedtak == Vedtak.OPPRETTHOLD_VEDTAK) "En begrunnelse" else null,
        årsak: Årsak? = null,
        begrunnelseOmgjøring: String? = null,
        interntNotat: String? = null,
    ) = Vurdering(
        behandlingId = behandlingId,
        vedtak = vedtak,
        årsak = årsak,
        begrunnelseOmgjøring = begrunnelseOmgjøring,
        hjemmel = hjemmel,
        innstillingKlageinstans = innstillingKlageinstans,
        dokumentasjonOgUtredning = dokumentasjonOgUtredning,
        spørsmåletISaken = spørsmåletISaken,
        aktuelleRettskilder = aktuelleRettskilder,
        klagersAnførsler = klagersAnførsler,
        vurderingAvKlagen = vurderingAvKlagen,
        interntNotat = interntNotat,
    )

    fun vurderingDto(
        behandlingId: UUID = UUID.randomUUID(),
        vedtak: Vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
        årsak: Årsak? = if (vedtak == Vedtak.OPPRETTHOLD_VEDTAK) null else Årsak.FEIL_I_LOVANDVENDELSE,
        begrunnelseOmgjøring: String? = null,
        hjemmel: Hjemmel? = if (vedtak == Vedtak.OPPRETTHOLD_VEDTAK) Hjemmel.BT_FEM else null,
        innstillingKlageinstans: String? = if (vedtak == Vedtak.OPPRETTHOLD_VEDTAK) "En begrunnelse" else null,
        dokumentasjonOgUtredning: String? = if (vedtak == Vedtak.OPPRETTHOLD_VEDTAK) "En begrunnelse" else null,
        spørsmåletISaken: String? = if (vedtak == Vedtak.OPPRETTHOLD_VEDTAK) "En begrunnelse" else null,
        aktuelleRettskilder: String? = if (vedtak == Vedtak.OPPRETTHOLD_VEDTAK) "En begrunnelse" else null,
        klagersAnførsler: String? = if (vedtak == Vedtak.OPPRETTHOLD_VEDTAK) "En begrunnelse" else null,
        vurderingAvKlagen: String? = if (vedtak == Vedtak.OPPRETTHOLD_VEDTAK) "En begrunnelse" else null,
        interntNotat: String? = null,
    ) = VurderingDto(
        behandlingId = behandlingId,
        vedtak = vedtak,
        årsak = årsak,
        begrunnelseOmgjøring = begrunnelseOmgjøring,
        hjemmel = hjemmel,
        innstillingKlageinstans = innstillingKlageinstans,
        dokumentasjonOgUtredning = dokumentasjonOgUtredning,
        spørsmåletISaken = spørsmåletISaken,
        aktuelleRettskilder = aktuelleRettskilder,
        klagersAnførsler = klagersAnførsler,
        vurderingAvKlagen = vurderingAvKlagen,
        interntNotat = interntNotat,
    )

    fun oppfyltForm(behandlingId: UUID) =
        Form(
            behandlingId = behandlingId,
            klagePart = FormVilkår.OPPFYLT,
            klagefristOverholdt = FormVilkår.OPPFYLT,
            klagefristOverholdtUnntak = FormkravFristUnntak.IKKE_SATT,
            klageKonkret = FormVilkår.OPPFYLT,
            klageSignert = FormVilkår.OPPFYLT,
        )

    val defaultIdent = "01010199999"
    val defaultIdenter = setOf(PersonIdent(defaultIdent))

    fun fagsak(
        identer: Set<PersonIdent> = defaultIdenter,
        stønadstype: Stønadstype = Stønadstype.OVERGANGSSTØNAD,
        id: UUID = UUID.randomUUID(),
        sporbar: Sporbar = Sporbar(),
        fagsakPersonId: UUID = UUID.randomUUID(),
    ): Fagsak = fagsak(stønadstype, id, FagsakPerson(id = fagsakPersonId, identer = identer), sporbar)

    fun fagsak(
        stønadstype: Stønadstype = Stønadstype.OVERGANGSSTØNAD,
        id: UUID = UUID.randomUUID(),
        person: FagsakPerson,
        sporbar: Sporbar = Sporbar(),
    ): Fagsak =
        Fagsak(
            id = id,
            fagsakPersonId = person.id,
            personIdenter = person.identer,
            stønadstype = stønadstype,
            sporbar = sporbar,
            eksternId = "1",
            fagsystem =
            when (stønadstype) {
                Stønadstype.OVERGANGSSTØNAD,
                Stønadstype.BARNETILSYN,
                Stønadstype.SKOLEPENGER,
                -> Fagsystem.EF

                Stønadstype.BARNETRYGD -> Fagsystem.BA
                Stønadstype.KONTANTSTØTTE -> Fagsystem.KS
            },
        )

    fun klageresultat(
        eventId: UUID = UUID.randomUUID(),
        type: BehandlingEventType = BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET,
        utfall: KlageinstansUtfall = KlageinstansUtfall.MEDHOLD,
        mottattEllerAvsluttetTidspunkt: LocalDateTime = SporbarUtils.now(),
        kildereferanse: UUID = UUID.randomUUID(),
        journalpostReferanser: List<String> = listOf("1", "2"),
        behandlingId: UUID = UUID.randomUUID(),
    ): KlageinstansResultat =
        KlageinstansResultat(
            eventId = eventId,
            type = type,
            utfall = utfall,
            mottattEllerAvsluttetTidspunkt = mottattEllerAvsluttetTidspunkt,
            kildereferanse = kildereferanse,
            journalpostReferanser = DatabaseConfiguration.StringListWrapper(verdier = journalpostReferanser),
            behandlingId = behandlingId,
        )

    fun journalpost(
        dokumenter: List<DokumentInfo> = emptyList(),
        relevanteDatoer: List<RelevantDato> = emptyList(),
    ) = Journalpost(
        journalpostId = UUID.randomUUID().toString(),
        journalposttype = Journalposttype.I,
        journalstatus = Journalstatus.MOTTATT,
        tema = "ENF",
        behandlingstema = null,
        tittel = "Tut og kjør",
        sak = null,
        bruker = null,
        avsenderMottaker = null,
        journalforendeEnhet = null,
        kanal = null,
        dokumenter = dokumenter,
        relevanteDatoer = relevanteDatoer,
        eksternReferanseId = null,
    )

    fun journalpostDokument(
        status: Dokumentstatus = Dokumentstatus.FERDIGSTILT,
        dokumentvarianter: List<Dokumentvariant>? = listOf(
            Dokumentvariant(
                Dokumentvariantformat.ARKIV,
                saksbehandlerHarTilgang = true,
            ),
        ),
    ) = DokumentInfo(
        dokumentInfoId = UUID.randomUUID().toString(),
        tittel = "Tittel",
        brevkode = null,
        dokumentstatus = status,
        dokumentvarianter = dokumentvarianter,
        logiskeVedlegg = listOf(),
    )

    fun påklagetVedtakDetaljer(
        eksternFagsystemBehandlingId: String = "123",
        fagsystemType: FagsystemType = FagsystemType.ORDNIÆR,
        vedtakstidspunkt: LocalDateTime = LocalDate.of(2022, 3, 1).atTime(8, 0),
        regelverk: Regelverk = Regelverk.NASJONAL,
    ) = PåklagetVedtakDetaljer(
        fagsystemType = fagsystemType,
        eksternFagsystemBehandlingId = eksternFagsystemBehandlingId,
        behandlingstype = "type",
        resultat = "resultat",
        vedtakstidspunkt = vedtakstidspunkt,
        regelverk = regelverk,
    )

    fun påklagetVedtakDto(): PåklagetVedtakDto =
        PåklagetVedtakDto(eksternFagsystemBehandlingId = null, påklagetVedtakstype = PåklagetVedtakstype.UTEN_VEDTAK)

    fun personopplysningerDto(
        personIdent: String = "123",
        adressebeskyttelse: Adressebeskyttelse? = null,
    ) = PersonopplysningerDto(
        personIdent = personIdent,
        navn = "navn",
        kjønn = Kjønn.MANN,
        adressebeskyttelse = adressebeskyttelse,
        folkeregisterpersonstatus = Folkeregisterpersonstatus.BOSATT,
        dødsdato = null,
        fullmakt = emptyList(),
        egenAnsatt = false,
        vergemål = emptyList(),
    )

    fun fagsystemVedtak(
        eksternBehandlingId: String,
        behandlingstype: String = "type",
        resultat: String = "resultat",
        vedtakstidspunkt: LocalDateTime = LocalDate.of(2022, 3, 1).atTime(8, 0),
        fagsystemType: FagsystemType = FagsystemType.ORDNIÆR,
        regelverk: Regelverk = Regelverk.NASJONAL,
    ) = FagsystemVedtak(
        eksternBehandlingId = eksternBehandlingId,
        behandlingstype = behandlingstype,
        resultat = resultat,
        vedtakstidspunkt = vedtakstidspunkt,
        fagsystemType = fagsystemType,
        regelverk = regelverk,
    )

    fun lagPersonopplysningerDto(
        personIdent: String = "123",
        navn: String = "Navn Navnesen",
        kjønn: Kjønn = Kjønn.MANN,
        adressebeskyttelse: Adressebeskyttelse? = null,
        folkeregisterpersonstatus: Folkeregisterpersonstatus = Folkeregisterpersonstatus.BOSATT,
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

    fun lagPåklagetVedtakDetaljer(
        fagsystemType: FagsystemType = FagsystemType.ORDNIÆR,
        eksternFagsystemBehandlingId: String = "1234",
        behandlingstype: String = "type",
        resultat: String = "resultat",
        vedtakstidspunkt: LocalDateTime = LocalDateTime.now(),
        regelverk: Regelverk = Regelverk.NASJONAL,
    ): PåklagetVedtakDetaljer {
        return PåklagetVedtakDetaljer(
            fagsystemType = fagsystemType,
            eksternFagsystemBehandlingId = eksternFagsystemBehandlingId,
            behandlingstype = behandlingstype,
            resultat = resultat,
            vedtakstidspunkt = vedtakstidspunkt,
            regelverk = regelverk,
        )
    }

    fun lagFritekstBrevRequestDto(
        overskrift: String = "overskrift",
        avsnitt: List<AvsnittDto> = listOf(
            AvsnittDto(
                deloverskrift = "deloverskrift",
                innhold = "innhold",
                skalSkjulesIBrevbygger = false,
            ),
        ),
        personIdent: String = "123",
        navn: String = "navn",
    ): FritekstBrevRequestDto {
        return FritekstBrevRequestDto(
            overskrift,
            avsnitt = avsnitt,
            personIdent,
            navn,
        )
    }

    fun lagPåklagetVedtak(
        påklagetVedtakstype: PåklagetVedtakstype = PåklagetVedtakstype.VEDTAK,
        påklagetVedtakDetaljer: PåklagetVedtakDetaljer? = lagPåklagetVedtakDetaljer(),
    ): PåklagetVedtak {
        return PåklagetVedtak(
            påklagetVedtakstype = påklagetVedtakstype,
            påklagetVedtakDetaljer = påklagetVedtakDetaljer,
        )
    }

    fun lagBrevmottakerPersonUtenIdent(
        id: UUID = UUID.randomUUID(),
        mottakerRolle: MottakerRolle = MottakerRolle.FULLMAKT,
        navn: String = "Navn Navnesen",
        adresselinje1: String = "Onkel Pølsemakers vei 10",
        adresselinje2: String? = null,
        postnummer: String? = "0010",
        poststed: String? = "Oslo",
        landkode: String = "NO",
    ): BrevmottakerPersonUtenIdent {
        return BrevmottakerPersonUtenIdent(
            id,
            mottakerRolle,
            navn,
            adresselinje1,
            adresselinje2,
            postnummer,
            poststed,
            landkode,
        )
    }

    fun lagBrevmottakerPersonMedIdent(
        personIdent: String = "23097825289",
        mottakerRolle: MottakerRolle = MottakerRolle.FULLMAKT,
        navn: String = "Navn Navnesen",
    ): BrevmottakerPersonMedIdent {
        return BrevmottakerPersonMedIdent(
            personIdent,
            mottakerRolle,
            navn,
        )
    }

    fun lagBrevmottakere(
        personer: List<BrevmottakerPerson> = emptyList(),
        organisasjoner: List<BrevmottakerOrganisasjon> = emptyList(),
    ): Brevmottakere {
        return Brevmottakere(
            personer = personer,
            organisasjoner = organisasjoner,
        )
    }

    fun lagBrev(
        behandlingId: UUID = UUID.randomUUID(),
        saksbehandlerHtml: String = "<html />",
        pdf: Fil? = null,
        mottakere: Brevmottakere? = null,
        mottakereJournalposter: BrevmottakereJournalposter? = null,
        sporbar: Sporbar = Sporbar(),
    ): Brev {
        return Brev(
            behandlingId = behandlingId,
            saksbehandlerHtml = saksbehandlerHtml,
            pdf = pdf,
            mottakere = mottakere,
            mottakereJournalposter = mottakereJournalposter,
            sporbar = sporbar,
        )
    }

    fun lagNyBrevmottakerPersonUtenIdent(
        mottakerRolle: MottakerRolle = MottakerRolle.FULLMAKT,
        navn: String = "Navn Navnesen",
        adresselinje1: String = "Adresselinje 1",
        adresselinje2: String? = null,
        postnummer: String? = "0010",
        poststed: String? = "Oslo",
        landkode: String = "NO",
    ): NyBrevmottakerPersonUtenIdent {
        return NyBrevmottakerPersonUtenIdent(
            mottakerRolle = mottakerRolle,
            navn = navn,
            adresselinje1 = adresselinje1,
            adresselinje2 = adresselinje2,
            postnummer = postnummer,
            poststed = poststed,
            landkode = landkode,
        )
    }

    fun lagNyBrevmottakerPersonMedIdent(
        personIdent: String = "23097825289",
        mottakerRolle: MottakerRolle = MottakerRolle.FULLMAKT,
        navn: String = "Navn Navnesen",
    ): NyBrevmottakerPersonMedIdent {
        return NyBrevmottakerPersonMedIdent(
            personIdent,
            mottakerRolle,
            navn,
        )
    }

    fun lagNyBrevmottakerOrganisasjon(
        organisasjonsnummer: String = "123",
        organisasjonsnavn: String = "Orgnavn",
        navnHosOrganisasjon: String = "navnHosOrganisasjon",
    ): NyBrevmottakerOrganisasjon {
        return NyBrevmottakerOrganisasjon(
            organisasjonsnummer = organisasjonsnummer,
            organisasjonsnavn = organisasjonsnavn,
            navnHosOrganisasjon = navnHosOrganisasjon,
        )
    }

    fun lagBrevmottakerOrganisasjon(
        organisasjonsnummer: String = "123",
        organisasjonsnavn: String = "Orgnavn",
        navnHosOrganisasjon: String = "navnHosOrganisasjon",
    ): BrevmottakerOrganisasjon {
        return BrevmottakerOrganisasjon(
            organisasjonsnummer = organisasjonsnummer,
            organisasjonsnavn = organisasjonsnavn,
            navnHosOrganisasjon = navnHosOrganisasjon,
        )
    }
}
