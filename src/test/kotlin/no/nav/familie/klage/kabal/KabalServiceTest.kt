package no.nav.familie.klage.kabal

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.brevmottaker.domain.MottakerRolle
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.infrastruktur.config.LenkeConfig
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.integrasjoner.FamilieIntegrasjonerClient
import no.nav.familie.klage.kabal.domain.OversendtKlageAnkeV3
import no.nav.familie.klage.kabal.domain.OversendtKlageAnkeV4
import no.nav.familie.klage.kabal.domain.OversendtPartIdType
import no.nav.familie.klage.kabal.domain.Ytelse
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.defaultIdent
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.klage.testutil.DomainUtil.fagsakDomain
import no.nav.familie.klage.testutil.DomainUtil.påklagetVedtakDetaljer
import no.nav.familie.klage.testutil.DomainUtil.vurdering
import no.nav.familie.klage.vurdering.domain.Hjemmel
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

internal class KabalServiceTest {
    val kabalClient = mockk<KabalClient>()
    val featureToggleService = mockk<FeatureToggleService>()
    val integrasjonerClient = mockk<FamilieIntegrasjonerClient>()
    val lenkeConfig = LenkeConfig(efSakLenke = "BASEURL_EF", baSakLenke = "BASEURL_BA", ksSakLenke = "BASEURL_KS")
    val kabalService = KabalService(kabalClient, integrasjonerClient, lenkeConfig, featureToggleService)
    val fagsak = fagsakDomain().tilFagsakMedPerson(setOf(PersonIdent("1")))
    val ingenBrevmottaker = Brevmottakere()

    val hjemmel = Hjemmel.FT_FEMTEN_FIRE

    val oversendelseSlotV3 = slot<OversendtKlageAnkeV3>()
    val oversendelseSlotV4 = slot<OversendtKlageAnkeV4>()
    val saksbehandlerA = Saksbehandler(UUID.randomUUID(), "A123456", "Alfa", "Surname", "4415")
    val saksbehandlerB = Saksbehandler(UUID.randomUUID(), "B987654", "Beta", "Etternavn", "4408")

    @BeforeEach
    internal fun setUp() {
        every { featureToggleService.isEnabled(any()) } returns true
        every { kabalClient.sendTilKabal(capture(oversendelseSlotV3)) } just Runs
        every { kabalClient.sendTilKabal(capture(oversendelseSlotV4)) } just Runs
        every { integrasjonerClient.hentSaksbehandlerInfo(any()) } answers {
            when (firstArg<String>()) {
                saksbehandlerA.navIdent -> saksbehandlerA
                saksbehandlerB.navIdent -> saksbehandlerB
                else -> error("Fant ikke info om saksbehanlder ${firstArg<String>()}")
            }
        }
    }

    @Test
    fun sendTilKabal() {
        val påklagetVedtakDetaljer = påklagetVedtakDetaljer()
        val behandling =
            behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer))
        val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)

        kabalService.sendTilKabal(fagsak, behandling, vurdering, saksbehandlerA.navIdent, ingenBrevmottaker)

        val oversendelse = oversendelseSlotV3.captured
        assertThat(oversendelse.fagsak?.fagsakId).isEqualTo(fagsak.eksternId)
        assertThat(oversendelse.fagsak?.fagsystem).isEqualTo(Fagsystem.EF)
        assertThat(oversendelse.hjemler).containsAll(listOf(hjemmel.kabalHjemmel))
        assertThat(oversendelse.kildeReferanse).isEqualTo(behandling.eksternBehandlingId.toString())
        assertThat(oversendelse.innsynUrl)
            .isEqualTo("${lenkeConfig.efSakLenke}/fagsak/${fagsak.eksternId}/${påklagetVedtakDetaljer.eksternFagsystemBehandlingId}")
        assertThat(oversendelse.tilknyttedeJournalposter).isEmpty()
        assertThat(oversendelse.brukersHenvendelseMottattNavDato).isEqualTo(behandling.klageMottatt)
        assertThat(oversendelse.innsendtTilNav).isEqualTo(behandling.klageMottatt)
        assertThat(oversendelse.klager.id.verdi).isEqualTo(fagsak.hentAktivIdent())
        assertThat(oversendelse.sakenGjelder).isNull()
        assertThat(oversendelse.kilde).isEqualTo(Fagsystem.EF)
        assertThat(oversendelse.ytelse).isEqualTo(Ytelse.ENF_ENF)
        assertThat(oversendelse.kommentar).isNull()
        assertThat(oversendelse.dvhReferanse).isNull()
        assertThat(oversendelse.forrigeBehandlendeEnhet).isEqualTo(saksbehandlerA.enhet)
        assertThat(oversendelse.hindreAutomatiskSvarbrev).isFalse()
    }

    @Test
    internal fun `skal sette innsynUrl til saksoversikten hvis påklaget vedtakstype gjelder tilbakekreving`() {
        val påklagetVedtakDetaljer = påklagetVedtakDetaljer(fagsystemType = FagsystemType.TILBAKEKREVING)
        val behandling =
            behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer))
        val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)

        kabalService.sendTilKabal(fagsak, behandling, vurdering, saksbehandlerB.navIdent, ingenBrevmottaker)

        assertThat(oversendelseSlotV3.captured.innsynUrl)
            .isEqualTo("${lenkeConfig.efSakLenke}/fagsak/${fagsak.eksternId}/saksoversikt")
        assertThat(oversendelseSlotV3.captured.forrigeBehandlendeEnhet).isEqualTo(saksbehandlerB.enhet)
    }

    @Test
    internal fun `skal sette hindreAutomatiskSvarbrev til true dersom årsaken til behandlingen er henvendelse fra kabal`() {
        val påklagetVedtakDetaljer = påklagetVedtakDetaljer()
        val behandling =
            behandling(
                fagsak,
                påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer),
                årsak = Klagebehandlingsårsak.HENVENDELSE_FRA_KABAL,
            )
        val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)

        kabalService.sendTilKabal(fagsak, behandling, vurdering, saksbehandlerB.navIdent, ingenBrevmottaker)

        assertThat(oversendelseSlotV3.captured.hindreAutomatiskSvarbrev).isTrue()
    }

    @Test
    internal fun `skal sette innsynUrl til saksoversikten hvis påklaget vedtak ikke er satt`() {
        val behandling = behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.UTEN_VEDTAK))
        val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)

        kabalService.sendTilKabal(fagsak, behandling, vurdering, saksbehandlerB.navIdent, ingenBrevmottaker)

        assertThat(oversendelseSlotV3.captured.innsynUrl)
            .isEqualTo("${lenkeConfig.efSakLenke}/fagsak/${fagsak.eksternId}/saksoversikt")
        assertThat(oversendelseSlotV3.captured.forrigeBehandlendeEnhet).isEqualTo(saksbehandlerB.enhet)
    }

    @Test
    internal fun `skal feile hvis saksbehandlerinfo ikke finnes`() {
        val behandling = behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.UTEN_VEDTAK))
        val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)

        assertThrows<IllegalStateException> {
            kabalService.sendTilKabal(fagsak, behandling, vurdering, "UKJENT1234", ingenBrevmottaker)
        }
    }

    @Nested
    inner class VergeOgFullmektig {
        @Test
        fun `dersom det både er verge og bruker skal motta kopi av brevet skal verge sendes over til kabal og motta svartidsbrevet`() {
            val påklagetVedtakDetaljer = påklagetVedtakDetaljer()
            val behandling =
                behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer))
            val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)
            val verge = BrevmottakerPersonMedIdent("01234567890", MottakerRolle.VERGE, "Navn")
            val bruker = BrevmottakerPersonMedIdent(fagsak.hentAktivIdent(), MottakerRolle.BRUKER, "Navn")
            kabalService.sendTilKabal(
                fagsak,
                behandling,
                vurdering,
                saksbehandlerA.navIdent,
                Brevmottakere(personer = listOf(verge, bruker)),
            )

            val oversendelse = oversendelseSlotV3.captured
            assertThat(
                oversendelse.klager.klagersProsessfullmektig
                    ?.id
                    ?.verdi,
            ).isEqualTo(verge.personIdent)
            assertThat(
                oversendelse.klager.klagersProsessfullmektig
                    ?.id
                    ?.type,
            ).isEqualTo(OversendtPartIdType.PERSON)
            assertThat(oversendelse.klager.klagersProsessfullmektig?.skalKlagerMottaKopi).isFalse()
        }

        @Test
        fun `skal sende med verge til kabal uten kopi til bruker`() {
            val påklagetVedtakDetaljer = påklagetVedtakDetaljer()
            val behandling =
                behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer))
            val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)
            val verge = BrevmottakerPersonMedIdent("01234567890", MottakerRolle.VERGE, "Navn")
            kabalService.sendTilKabal(
                fagsak,
                behandling,
                vurdering,
                saksbehandlerA.navIdent,
                Brevmottakere(personer = listOf(verge)),
            )

            val oversendelse = oversendelseSlotV3.captured
            assertThat(
                oversendelse.klager.klagersProsessfullmektig
                    ?.id
                    ?.verdi,
            ).isEqualTo(verge.personIdent)
            assertThat(
                oversendelse.klager.klagersProsessfullmektig
                    ?.id
                    ?.type,
            ).isEqualTo(OversendtPartIdType.PERSON)
            assertThat(oversendelse.klager.klagersProsessfullmektig?.skalKlagerMottaKopi).isFalse()
        }

        @Test
        fun `skal sende med fullmektig til kabal `() {
            val påklagetVedtakDetaljer = påklagetVedtakDetaljer()
            val behandling =
                behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer))
            val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)
            val fullmektig = BrevmottakerPersonMedIdent("01234567890", MottakerRolle.FULLMAKT, "Navn")
            kabalService.sendTilKabal(
                fagsak,
                behandling,
                vurdering,
                saksbehandlerA.navIdent,
                Brevmottakere(personer = listOf(fullmektig)),
            )

            val oversendelse = oversendelseSlotV3.captured
            assertThat(
                oversendelse.klager.klagersProsessfullmektig
                    ?.id
                    ?.verdi,
            ).isEqualTo(fullmektig.personIdent)
            assertThat(
                oversendelse.klager.klagersProsessfullmektig
                    ?.id
                    ?.type,
            ).isEqualTo(OversendtPartIdType.PERSON)
            assertThat(oversendelse.klager.klagersProsessfullmektig?.skalKlagerMottaKopi).isFalse()
        }

        @Test
        fun `skal sende med organisasjonsfullemktig til kabal `() {
            val påklagetVedtakDetaljer = påklagetVedtakDetaljer()
            val behandling =
                behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer))
            val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)
            val fullmektig = BrevmottakerOrganisasjon("012345678", "Navn på org", "Navn på person")
            kabalService.sendTilKabal(
                fagsak,
                behandling,
                vurdering,
                saksbehandlerA.navIdent,
                Brevmottakere(organisasjoner = listOf(fullmektig)),
            )

            val oversendelse = oversendelseSlotV3.captured
            assertThat(
                oversendelse.klager.klagersProsessfullmektig
                    ?.id
                    ?.verdi,
            ).isEqualTo(fullmektig.organisasjonsnummer)
            assertThat(
                oversendelse.klager.klagersProsessfullmektig
                    ?.id
                    ?.type,
            ).isEqualTo(OversendtPartIdType.VIRKSOMHET)
            assertThat(oversendelse.klager.klagersProsessfullmektig?.skalKlagerMottaKopi).isFalse()
        }

        @Test
        fun `skal sende med fullmektig til kabal dersom det finnes både verge, fullmektig og organsiasjon`() {
            val påklagetVedtakDetaljer = påklagetVedtakDetaljer()
            val behandling =
                behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer))
            val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)
            val fullmektigOrganisasjon = BrevmottakerOrganisasjon("012345678", "Navn på org", "Navn på person")
            val fullmektig = BrevmottakerPersonMedIdent("01234567890", MottakerRolle.FULLMAKT, "Fullmektig")
            val verge = BrevmottakerPersonMedIdent("98765432100", MottakerRolle.VERGE, "Verge")
            kabalService.sendTilKabal(
                fagsak,
                behandling,
                vurdering,
                saksbehandlerA.navIdent,
                Brevmottakere(personer = listOf(verge, fullmektig), organisasjoner = listOf(fullmektigOrganisasjon)),
            )

            val oversendelse = oversendelseSlotV3.captured
            assertThat(
                oversendelse.klager.klagersProsessfullmektig
                    ?.id
                    ?.verdi,
            ).isEqualTo(fullmektig.personIdent)
            assertThat(
                oversendelse.klager.klagersProsessfullmektig
                    ?.id
                    ?.type,
            ).isEqualTo(OversendtPartIdType.PERSON)
            assertThat(oversendelse.klager.klagersProsessfullmektig?.skalKlagerMottaKopi).isFalse()
        }
    }

    @Nested
    inner class OversendtKlageAnkeV4Test {
        private val baFagsak = fagsak(stønadstype = Stønadstype.BARNETRYGD)
        private val baBehandling = behandling(fagsak = baFagsak)
        private val baHjemmel = Hjemmel.BT_TO
        private val baVurdering = vurdering(behandlingId = baBehandling.id, hjemmel = baHjemmel)
        private val bruker = BrevmottakerPersonMedIdent(defaultIdent, MottakerRolle.BRUKER, "Bruker Brukersen")

        private val fullmektigOrganisasjon = BrevmottakerOrganisasjon(
            organisasjonsnummer = "987654321",
            organisasjonsnavn = "Fullmektig AS",
            navnHosOrganisasjon = bruker.navn,
        )

        private val fullmektigMedIdent = BrevmottakerPersonMedIdent(
            personIdent = "12345678910",
            mottakerRolle = MottakerRolle.FULLMAKT,
            navn = "Fullmektig Fullmektigsen",
        )

        private val fullmektigUtenIdent = BrevmottakerPersonUtenIdent(
            id = UUID.randomUUID(),
            mottakerRolle = MottakerRolle.FULLMAKT,
            navn = "Fullmektig Fullmektigsen",
            adresselinje1 = "Adresselinje 1",
            adresselinje2 = "Adresselinje 2",
            postnummer = "1234",
            poststed = "Poststed",
            landkode = "NO",
        )

        @Test
        internal fun `sendTilKabal skal kaste feil dersom toggle er skrudd av`() {
            // Arrange
            every { featureToggleService.isEnabled(any()) } returns false

            // Act & Assert
            val feilmelding = assertThrows<Feil> {
                kabalService.sendTilKabal(
                    fagsak = baFagsak,
                    behandling = baBehandling,
                    vurdering = baVurdering,
                    saksbehandlerIdent = saksbehandlerA.navIdent,
                    brevmottakere = Brevmottakere(personer = listOf(bruker, fullmektigUtenIdent)),
                )
            }

            assertThat(feilmelding.message).isEqualTo("V4 av oversendelse til Kabal er foreløpig ikke støttet.")
        }

        @Test
        internal fun `skal bruke OversendtKlageAnkeV3 hvis bare bruker er brevmottaker`() {
            // Act
            kabalService.sendTilKabal(
                fagsak = baFagsak,
                behandling = baBehandling,
                vurdering = baVurdering,
                saksbehandlerIdent = saksbehandlerA.navIdent,
                brevmottakere = Brevmottakere(personer = listOf(bruker)),
            )

            // Assert
            assertThat(oversendelseSlotV3.isCaptured).isTrue()
            with(oversendelseSlotV3.captured) {
                assertThat(klager.id.verdi).isEqualTo(baFagsak.hentAktivIdent())
                assertThat(klager.klagersProsessfullmektig).isNull()
            }
        }

        @Test
        internal fun `skal bruke OversendtKlageAnkeV3 hvis brevmottaker er organisasjon`() {
            // Act
            kabalService.sendTilKabal(
                fagsak = baFagsak,
                behandling = baBehandling,
                vurdering = baVurdering,
                saksbehandlerIdent = saksbehandlerA.navIdent,
                brevmottakere = Brevmottakere(
                    personer = listOf(bruker),
                    organisasjoner = listOf(fullmektigOrganisasjon),
                ),
            )

            // Assert
            assertThat(oversendelseSlotV3.isCaptured).isTrue()
            with(oversendelseSlotV3.captured.klager) {
                assertThat(id.type).isEqualTo(OversendtPartIdType.PERSON)
                assertThat(id.verdi).isEqualTo(baFagsak.hentAktivIdent())
                assertThat(klagersProsessfullmektig?.id?.type).isEqualTo(OversendtPartIdType.VIRKSOMHET)
                assertThat(klagersProsessfullmektig?.id?.verdi).isEqualTo(fullmektigOrganisasjon.organisasjonsnummer)
            }
        }

        @Test
        internal fun `skal bruke OversendtKlageAnkeV3 hvis alle brevmottakere har ident`() {
            // Act
            kabalService.sendTilKabal(
                fagsak = baFagsak,
                behandling = baBehandling,
                vurdering = baVurdering,
                saksbehandlerIdent = saksbehandlerA.navIdent,
                brevmottakere = Brevmottakere(personer = listOf(bruker, fullmektigMedIdent)),
            )

            // Assert
            assertThat(oversendelseSlotV3.isCaptured).isTrue()
            with(oversendelseSlotV3.captured.klager) {
                assertThat(id.type).isEqualTo(OversendtPartIdType.PERSON)
                assertThat(id.verdi).isEqualTo(baFagsak.hentAktivIdent())
                assertThat(klagersProsessfullmektig?.id?.type).isEqualTo(OversendtPartIdType.PERSON)
                assertThat(klagersProsessfullmektig?.id?.verdi).isEqualTo(fullmektigMedIdent.personIdent)
            }
        }

        @Test
        internal fun `skal bruke OversendtKlageAnkeV4 hvis minst én brevmottaker ikke har ident`() {
            // Act
            kabalService.sendTilKabal(
                fagsak = baFagsak,
                behandling = baBehandling,
                vurdering = baVurdering,
                saksbehandlerIdent = saksbehandlerA.navIdent,
                brevmottakere = Brevmottakere(personer = listOf(bruker, fullmektigUtenIdent)),
            )

            // Assert
            assertThat(oversendelseSlotV4.isCaptured).isTrue()
            with(oversendelseSlotV4.captured) {
                assertThat(sakenGjelder.id.type).isEqualTo(OversendtPartIdType.PERSON)
                assertThat(sakenGjelder.id.verdi).isEqualTo(baFagsak.hentAktivIdent())
                assertThat(prosessfullmektig?.id).isNull()
                assertThat(prosessfullmektig?.navn).isEqualTo(fullmektigUtenIdent.navn)
                with(prosessfullmektig?.adresse!!) {
                    assertThat(adresselinje1).isEqualTo(fullmektigUtenIdent.adresselinje1)
                    assertThat(adresselinje2).isEqualTo(fullmektigUtenIdent.adresselinje2)
                    assertThat(postnummer).isEqualTo(fullmektigUtenIdent.postnummer)
                    assertThat(poststed).isEqualTo(fullmektigUtenIdent.poststed)
                    assertThat(land).isEqualTo(fullmektigUtenIdent.landkode)
                }
            }
        }
    }
}
