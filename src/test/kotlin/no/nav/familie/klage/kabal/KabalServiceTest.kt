package no.nav.familie.klage.kabal

import io.mockk.every
import io.mockk.justRun
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
import no.nav.familie.klage.integrasjoner.FamilieIntegrasjonerClient
import no.nav.familie.klage.kabal.domain.OversendtKlageAnkeV3
import no.nav.familie.klage.kabal.domain.OversendtKlageAnkeV4
import no.nav.familie.klage.kabal.domain.OversendtPartIdType
import no.nav.familie.klage.kabal.domain.OversendtType
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
import java.time.LocalDate
import java.util.UUID

internal class KabalServiceTest {
    private val kabalClient = mockk<KabalClient>()
    private val integrasjonerClient = mockk<FamilieIntegrasjonerClient>()
    private val lenkeConfig =
        LenkeConfig(efSakLenke = "BASEURL_EF", baSakLenke = "BASEURL_BA", ksSakLenke = "BASEURL_KS")
    private val kabalService = KabalService(kabalClient, integrasjonerClient, lenkeConfig)
    private val ingenBrevmottaker = Brevmottakere()

    private val saksbehandler = Saksbehandler(UUID.randomUUID(), "A123456", "Alfa", "Surname", "4415", "Skien")

    @BeforeEach
    internal fun setUp() {
        every { integrasjonerClient.hentSaksbehandlerInfo(any()) } answers {
            when (firstArg<String>()) {
                saksbehandler.navIdent -> saksbehandler
                else -> error("Fant ikke info om saksbehanlder ${firstArg<String>()}")
            }
        }
    }

    @Nested
    inner class OversendtKlageAnkeV3Test {
        private val fagsak = fagsakDomain().tilFagsakMedPersonOgInstitusjon(setOf(PersonIdent("1")))
        private val hjemmel = Hjemmel.FT_FEMTEN_FIRE

        private val oversendelseSlot = slot<OversendtKlageAnkeV3>()

        @BeforeEach
        internal fun setUp() {
            justRun { kabalClient.sendTilKabal(capture(oversendelseSlot)) }
        }

        @Test
        fun sendTilKabal() {
            val påklagetVedtakDetaljer = påklagetVedtakDetaljer()
            val behandling =
                behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer))
            val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)

            kabalService.sendTilKabal(fagsak, behandling, vurdering, saksbehandler.navIdent, ingenBrevmottaker)

            val oversendelse = oversendelseSlot.captured
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
            assertThat(oversendelse.forrigeBehandlendeEnhet).isEqualTo(saksbehandler.enhet)
            assertThat(oversendelse.hindreAutomatiskSvarbrev).isFalse()
        }

        @Test
        internal fun `skal sette innsynUrl til saksoversikten hvis påklaget vedtakstype gjelder tilbakekreving`() {
            val påklagetVedtakDetaljer = påklagetVedtakDetaljer(fagsystemType = FagsystemType.TILBAKEKREVING)
            val behandling =
                behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer))
            val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)

            kabalService.sendTilKabal(fagsak, behandling, vurdering, saksbehandler.navIdent, ingenBrevmottaker)

            assertThat(oversendelseSlot.captured.innsynUrl)
                .isEqualTo("${lenkeConfig.efSakLenke}/fagsak/${fagsak.eksternId}/saksoversikt")
            assertThat(oversendelseSlot.captured.forrigeBehandlendeEnhet).isEqualTo(saksbehandler.enhet)
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

            kabalService.sendTilKabal(fagsak, behandling, vurdering, saksbehandler.navIdent, ingenBrevmottaker)

            assertThat(oversendelseSlot.captured.hindreAutomatiskSvarbrev).isTrue()
        }

        @Test
        internal fun `skal sette innsynUrl til saksoversikten hvis påklaget vedtak ikke er satt`() {
            val behandling = behandling(fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.UTEN_VEDTAK))
            val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)

            kabalService.sendTilKabal(fagsak, behandling, vurdering, saksbehandler.navIdent, ingenBrevmottaker)

            assertThat(oversendelseSlot.captured.innsynUrl)
                .isEqualTo("${lenkeConfig.efSakLenke}/fagsak/${fagsak.eksternId}/saksoversikt")
            assertThat(oversendelseSlot.captured.forrigeBehandlendeEnhet).isEqualTo(saksbehandler.enhet)
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
                    behandling(
                        fagsak,
                        påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer),
                    )
                val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)
                val verge = BrevmottakerPersonMedIdent("01234567890", MottakerRolle.VERGE, "Navn")
                val bruker = BrevmottakerPersonMedIdent(fagsak.hentAktivIdent(), MottakerRolle.BRUKER, "Navn")
                kabalService.sendTilKabal(
                    fagsak,
                    behandling,
                    vurdering,
                    saksbehandler.navIdent,
                    Brevmottakere(personer = listOf(verge, bruker)),
                )

                val oversendelse = oversendelseSlot.captured
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
                    behandling(
                        fagsak,
                        påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer),
                    )
                val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)
                val verge = BrevmottakerPersonMedIdent("01234567890", MottakerRolle.VERGE, "Navn")
                kabalService.sendTilKabal(
                    fagsak,
                    behandling,
                    vurdering,
                    saksbehandler.navIdent,
                    Brevmottakere(personer = listOf(verge)),
                )

                val oversendelse = oversendelseSlot.captured
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
                    behandling(
                        fagsak,
                        påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer),
                    )
                val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)
                val fullmektig = BrevmottakerPersonMedIdent("01234567890", MottakerRolle.FULLMAKT, "Navn")
                kabalService.sendTilKabal(
                    fagsak,
                    behandling,
                    vurdering,
                    saksbehandler.navIdent,
                    Brevmottakere(personer = listOf(fullmektig)),
                )

                val oversendelse = oversendelseSlot.captured
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
                    behandling(
                        fagsak,
                        påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer),
                    )
                val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)
                val fullmektig = BrevmottakerOrganisasjon("012345678", "Navn på org", "Navn på person")
                kabalService.sendTilKabal(
                    fagsak,
                    behandling,
                    vurdering,
                    saksbehandler.navIdent,
                    Brevmottakere(organisasjoner = listOf(fullmektig)),
                )

                val oversendelse = oversendelseSlot.captured
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
                    behandling(
                        fagsak,
                        påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer),
                    )
                val vurdering = vurdering(behandlingId = behandling.id, hjemmel = hjemmel)
                val fullmektigOrganisasjon = BrevmottakerOrganisasjon("012345678", "Navn på org", "Navn på person")
                val fullmektig = BrevmottakerPersonMedIdent("01234567890", MottakerRolle.FULLMAKT, "Fullmektig")
                val verge = BrevmottakerPersonMedIdent("98765432100", MottakerRolle.VERGE, "Verge")
                kabalService.sendTilKabal(
                    fagsak,
                    behandling,
                    vurdering,
                    saksbehandler.navIdent,
                    Brevmottakere(
                        personer = listOf(verge, fullmektig),
                        organisasjoner = listOf(fullmektigOrganisasjon),
                    ),
                )

                val oversendelse = oversendelseSlot.captured
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
    }

    @Nested
    inner class OversendtKlageAnkeV4Test {
        private val fagsak = fagsak(stønadstype = Stønadstype.BARNETRYGD)
        private val påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer())
        private val behandling = behandling(fagsak = fagsak, påklagetVedtak = påklagetVedtak)
        private val vurdering = vurdering(behandlingId = behandling.id, hjemmel = Hjemmel.BT_TO)

        private val bruker = BrevmottakerPersonMedIdent(defaultIdent, MottakerRolle.BRUKER, "Bruker Brukersen")

        private val fullmektigOrganisasjon =
            BrevmottakerOrganisasjon(
                organisasjonsnummer = "987654321",
                organisasjonsnavn = "Fullmektig AS",
                navnHosOrganisasjon = bruker.navn,
            )

        private val fullmektigMedIdent =
            BrevmottakerPersonMedIdent(
                personIdent = "12345678910",
                mottakerRolle = MottakerRolle.FULLMAKT,
                navn = "Fullmektig Fullmektigsen",
            )

        private val fullmektigUtenIdent =
            BrevmottakerPersonUtenIdent(
                id = UUID.randomUUID(),
                mottakerRolle = MottakerRolle.FULLMAKT,
                navn = "Fullmektig Fullmektigsen",
                adresselinje1 = "Adresselinje 1",
                adresselinje2 = "Adresselinje 2",
                postnummer = "1234",
                poststed = "Poststed",
                landkode = "NO",
            )

        private val verge =
            BrevmottakerPersonMedIdent(
                personIdent = "10987654321",
                mottakerRolle = MottakerRolle.VERGE,
                navn = "Verge Vergesen",
            )

        private val oversendelseSlot = slot<OversendtKlageAnkeV4>()

        @BeforeEach
        internal fun setUp() {
            justRun { kabalClient.sendTilKabal(capture(oversendelseSlot)) }
        }

        @Test
        fun sendTilKabal() {
            // Act
            kabalService.sendTilKabal(
                fagsak = fagsak,
                behandling = behandling,
                vurdering = vurdering,
                saksbehandlerIdent = saksbehandler.navIdent,
                brevmottakere = Brevmottakere(),
            )

            // Assert
            val oversendelse = oversendelseSlot.captured
            assertThat(oversendelse.type).isEqualTo(OversendtType.KLAGE)
            assertThat(oversendelse.sakenGjelder.id.type).isEqualTo(OversendtPartIdType.PERSON)
            assertThat(oversendelse.sakenGjelder.id.verdi).isEqualTo(fagsak.hentAktivIdent())
            assertThat(oversendelse.prosessfullmektig).isNull()
            assertThat(oversendelse.fagsak.fagsakId).isEqualTo(fagsak.eksternId)
            assertThat(oversendelse.fagsak.fagsystem).isEqualTo(Fagsystem.BA)
            assertThat(oversendelse.kildeReferanse).isEqualTo(behandling.eksternBehandlingId.toString())
            assertThat(oversendelse.dvhReferanse).isNull()
            assertThat(oversendelse.hjemler).containsAll(listOf(Hjemmel.BT_TO.kabalHjemmel))
            assertThat(oversendelse.forrigeBehandlendeEnhet).isEqualTo(saksbehandler.enhet)
            assertThat(oversendelse.tilknyttedeJournalposter).isEmpty()
            assertThat(oversendelse.brukersKlageMottattVedtaksinstans).isEqualTo(LocalDate.now())
            assertThat(oversendelse.ytelse).isEqualTo(Ytelse.BAR_BAR)
            assertThat(oversendelse.kommentar).isNull()
            assertThat(oversendelse.hindreAutomatiskSvarbrev).isFalse()
        }

        @Test
        internal fun `skal sette hindreAutomatiskSvarbrev til true dersom årsaken til behandlingen er henvendelse fra kabal`() {
            // Arrange
            val behandling =
                behandling(
                    fagsak = fagsak,
                    påklagetVedtak = påklagetVedtak,
                    årsak = Klagebehandlingsårsak.HENVENDELSE_FRA_KABAL,
                )
            val vurdering = vurdering(behandlingId = behandling.id, hjemmel = Hjemmel.BT_TO)

            // Act
            kabalService.sendTilKabal(fagsak, behandling, vurdering, saksbehandler.navIdent, ingenBrevmottaker)

            // Assert
            assertThat(oversendelseSlot.captured.hindreAutomatiskSvarbrev).isTrue()
        }

        @Test
        internal fun `skal feile hvis saksbehandlerinfo ikke finnes`() {
            // Act & Assert
            assertThrows<IllegalStateException> {
                kabalService.sendTilKabal(fagsak, behandling, vurdering, "UKJENT1234", ingenBrevmottaker)
            }
        }

        @Nested
        inner class VergeOgFullmektig {
            @Test
            fun `verge skal sendes med til kabal`() {
                // Act
                kabalService.sendTilKabal(
                    fagsak = fagsak,
                    behandling = behandling,
                    vurdering = vurdering,
                    saksbehandlerIdent = saksbehandler.navIdent,
                    brevmottakere = Brevmottakere(personer = listOf(verge, bruker)),
                )

                // Assert
                val prosessfullmektig = oversendelseSlot.captured.prosessfullmektig!!
                assertThat(prosessfullmektig.id?.verdi).isEqualTo(verge.personIdent)
                assertThat(prosessfullmektig.id?.type).isEqualTo(OversendtPartIdType.PERSON)
                assertThat(prosessfullmektig.navn).isEqualTo(verge.navn)
                assertThat(prosessfullmektig.adresse).isNull()
            }

            @Test
            fun `fullmektig med ident skal sendes med til kabal`() {
                // Act
                kabalService.sendTilKabal(
                    fagsak = fagsak,
                    behandling = behandling,
                    vurdering = vurdering,
                    saksbehandlerIdent = saksbehandler.navIdent,
                    brevmottakere = Brevmottakere(personer = listOf(fullmektigMedIdent, bruker)),
                )

                // Assert
                val prosessfullmektig = oversendelseSlot.captured.prosessfullmektig!!
                assertThat(prosessfullmektig.id?.verdi).isEqualTo(fullmektigMedIdent.personIdent)
                assertThat(prosessfullmektig.id?.type).isEqualTo(OversendtPartIdType.PERSON)
                assertThat(prosessfullmektig.navn).isEqualTo(fullmektigMedIdent.navn)
                assertThat(prosessfullmektig.adresse).isNull()
            }

            @Test
            fun `fullmektig uten ident skal sendes med til kabal`() {
                // Act
                kabalService.sendTilKabal(
                    fagsak = fagsak,
                    behandling = behandling,
                    vurdering = vurdering,
                    saksbehandlerIdent = saksbehandler.navIdent,
                    brevmottakere = Brevmottakere(personer = listOf(fullmektigUtenIdent, bruker)),
                )

                // Assert
                val prosessfullmektig = oversendelseSlot.captured.prosessfullmektig!!
                assertThat(prosessfullmektig.id).isNull()
                assertThat(prosessfullmektig.navn).isEqualTo(fullmektigUtenIdent.navn)
                assertThat(prosessfullmektig.adresse?.adresselinje1).isEqualTo(fullmektigUtenIdent.adresselinje1)
                assertThat(prosessfullmektig.adresse?.adresselinje2).isEqualTo(fullmektigUtenIdent.adresselinje2)
                assertThat(prosessfullmektig.adresse?.postnummer).isEqualTo(fullmektigUtenIdent.postnummer)
                assertThat(prosessfullmektig.adresse?.poststed).isEqualTo(fullmektigUtenIdent.poststed)
                assertThat(prosessfullmektig.adresse?.land).isEqualTo(fullmektigUtenIdent.landkode)
            }

            @Test
            fun `organisasjon skal sendes med til kabal`() {
                // Act
                kabalService.sendTilKabal(
                    fagsak = fagsak,
                    behandling = behandling,
                    vurdering = vurdering,
                    saksbehandlerIdent = saksbehandler.navIdent,
                    brevmottakere =
                        Brevmottakere(
                            personer = listOf(bruker),
                            organisasjoner = listOf(fullmektigOrganisasjon),
                        ),
                )

                // Assert
                val prosessfullmektig = oversendelseSlot.captured.prosessfullmektig!!
                assertThat(prosessfullmektig.id?.verdi).isEqualTo(fullmektigOrganisasjon.organisasjonsnummer)
                assertThat(prosessfullmektig.id?.type).isEqualTo(OversendtPartIdType.VIRKSOMHET)
                assertThat(prosessfullmektig.navn).isEqualTo(fullmektigOrganisasjon.organisasjonsnavn)
                assertThat(prosessfullmektig.adresse).isNull()
            }

            @Test
            fun `skal sende med fullmektig til kabal dersom det finnes både verge, fullmektig og organsiasjon`() {
                // Act
                kabalService.sendTilKabal(
                    fagsak = fagsak,
                    behandling = behandling,
                    vurdering = vurdering,
                    saksbehandlerIdent = saksbehandler.navIdent,
                    brevmottakere =
                        Brevmottakere(
                            personer = listOf(verge, fullmektigMedIdent),
                            organisasjoner = listOf(fullmektigOrganisasjon),
                        ),
                )

                val prosessfullmektig = oversendelseSlot.captured.prosessfullmektig!!
                assertThat(prosessfullmektig.id?.verdi).isEqualTo(fullmektigMedIdent.personIdent)
                assertThat(prosessfullmektig.id?.type).isEqualTo(OversendtPartIdType.PERSON)
                assertThat(prosessfullmektig.navn).isEqualTo(fullmektigMedIdent.navn)
                assertThat(prosessfullmektig.adresse).isNull()
            }
        }
    }
}
