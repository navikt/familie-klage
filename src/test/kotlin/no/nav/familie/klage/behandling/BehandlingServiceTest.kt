package no.nav.familie.klage.behandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.Klagebehandlingsresultat
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.behandling.enhet.BarnetrygdEnhet
import no.nav.familie.klage.behandling.enhet.KontantstøtteEnhet
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.integrasjoner.FagsystemVedtakService
import no.nav.familie.klage.kabal.KlageresultatRepository
import no.nav.familie.klage.oppgave.OppgaveTaskService
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.testutil.BrukerContextUtil.clearBrukerContext
import no.nav.familie.klage.testutil.BrukerContextUtil.mockBrukerContext
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.klage.testutil.DomainUtil.fagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class BehandlingServiceTest {
    val klageresultatRepository = mockk<KlageresultatRepository>()
    val fagsakService = mockk<FagsakService>()
    val behandlingRepository = mockk<BehandlingRepository>()
    val behandlinghistorikkService = mockk<BehandlingshistorikkService>()
    val taskService = mockk<TaskService>()
    val oppgaveTaskService = mockk<OppgaveTaskService>()
    val fagsystemVedtakService = mockk<FagsystemVedtakService>()
    val brevService = mockk<BrevService>()

    val behandlingService =
        BehandlingService(
            behandlingRepository,
            fagsakService,
            klageresultatRepository,
            fagsystemVedtakService,
        )

    val behandlingSlot = slot<Behandling>()

    @BeforeEach
    fun setUp() {
        mockBrukerContext()
        every {
            behandlingRepository.update(capture(behandlingSlot))
        } answers {
            behandlingSlot.captured
        }
        every { behandlinghistorikkService.opprettBehandlingshistorikk(any(), any(), any()) } returns mockk()
        every { oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(any(), any(), any()) } returns mockk()
        every { taskService.save(any()) } returns mockk<Task>()
    }

    @AfterEach
    fun tearDown() {
        clearBrukerContext()
    }

    @Nested
    inner class PåklagetVedtak {
        @Test
        internal fun `skal ikke kunne oppdatere påklaget vedtak dersom behandlingen er låst`() {
            val behandling = behandling(fagsak(), status = BehandlingStatus.VENTER)
            every { behandlingRepository.findByIdOrThrow(behandling.id) } returns behandling
            assertThrows<ApiFeil> {
                behandlingService.oppdaterPåklagetVedtak(
                    behandlingId = behandling.id,
                    PåklagetVedtakDto(null, null, PåklagetVedtakstype.UTEN_VEDTAK),
                )
            }
        }

        @Test
        internal fun `skal ikke kunne oppdatere påklaget vedtak med ugyldig tilstand`() {
            val behandling = behandling(fagsak(), status = BehandlingStatus.UTREDES)
            every { behandlingRepository.findByIdOrThrow(behandling.id) } returns behandling
            val ugyldigManglerBehandlingId = PåklagetVedtakDto(null, null, PåklagetVedtakstype.VEDTAK)
            val ugyldigManglerVedtaksdatoInfotrygd = PåklagetVedtakDto(null, null, PåklagetVedtakstype.INFOTRYGD_TILBAKEKREVING)
            val ugyldigManglerVedtaksdatoUtestengelse = PåklagetVedtakDto(null, null, PåklagetVedtakstype.UTESTENGELSE)
            val ugyldigManglerVedtaksdatoInfotrygdOrdinærtVedtak =
                PåklagetVedtakDto(null, null, PåklagetVedtakstype.INFOTRYGD_ORDINÆRT_VEDTAK)
            val ugyldigUtenVedtakMedBehandlingId = PåklagetVedtakDto("123", null, PåklagetVedtakstype.UTEN_VEDTAK)
            val ugyldigIkkeValgtMedBehandlingId = PåklagetVedtakDto("123", null, PåklagetVedtakstype.IKKE_VALGT)

            assertThrows<Feil> { behandlingService.oppdaterPåklagetVedtak(behandling.id, ugyldigManglerBehandlingId) }
            assertThrows<Feil> { behandlingService.oppdaterPåklagetVedtak(behandling.id, ugyldigUtenVedtakMedBehandlingId) }
            assertThrows<Feil> { behandlingService.oppdaterPåklagetVedtak(behandling.id, ugyldigIkkeValgtMedBehandlingId) }
            assertThrows<Feil> { behandlingService.oppdaterPåklagetVedtak(behandling.id, ugyldigManglerVedtaksdatoInfotrygd) }
            assertThrows<Feil> { behandlingService.oppdaterPåklagetVedtak(behandling.id, ugyldigManglerVedtaksdatoUtestengelse) }
            assertThrows<Feil> { behandlingService.oppdaterPåklagetVedtak(behandling.id, ugyldigManglerVedtaksdatoInfotrygdOrdinærtVedtak) }
        }

        @Test
        internal fun `skal kunne oppdatere påklaget vedtak med gyldige tilstander`() {
            val behandling = behandling(fagsak(), status = BehandlingStatus.UTREDES)
            every { behandlingRepository.findByIdOrThrow(behandling.id) } returns behandling
            mockHentFagsystemVedtak(behandling, "123")

            val medVedtak = PåklagetVedtakDto("123", null, PåklagetVedtakstype.VEDTAK)
            val utenVedtak = PåklagetVedtakDto(null, null, PåklagetVedtakstype.UTEN_VEDTAK)
            val ikkeValgt = PåklagetVedtakDto(null, null, PåklagetVedtakstype.IKKE_VALGT)
            val gjelderInfotrygd =
                PåklagetVedtakDto(null, null, PåklagetVedtakstype.INFOTRYGD_TILBAKEKREVING, manuellVedtaksdato = LocalDate.now())
            val utestengelse = PåklagetVedtakDto(null, null, PåklagetVedtakstype.UTESTENGELSE, manuellVedtaksdato = LocalDate.now())

            behandlingService.oppdaterPåklagetVedtak(behandling.id, ikkeValgt)
            verify(exactly = 1) { behandlingRepository.update(any()) }

            behandlingService.oppdaterPåklagetVedtak(behandling.id, utenVedtak)
            verify(exactly = 2) { behandlingRepository.update(any()) }

            behandlingService.oppdaterPåklagetVedtak(behandling.id, medVedtak)
            verify(exactly = 3) { behandlingRepository.update(any()) }

            behandlingService.oppdaterPåklagetVedtak(behandling.id, gjelderInfotrygd)
            verify(exactly = 4) { behandlingRepository.update(any()) }

            behandlingService.oppdaterPåklagetVedtak(behandling.id, utestengelse)
            verify(exactly = 5) { behandlingRepository.update(any()) }
        }
    }

    @Nested
    inner class Hentklagebehandlingsresultat {
        @Test
        fun `Skal ikke filtrere bort Klagebehandlingsresultat hvis behandlingResultat er IKKE_MEDHOLD_FORMKRAV_AVVIST`() {
            val behandlingId = UUID.randomUUID()
            val fagsak = fagsak()
            val klagebehandlingsresultat =
                listOf(
                    lagKlageBehandlingsresultat(BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST),
                )

            val behandling = behandling(id = behandlingId, fagsak = fagsak, status = BehandlingStatus.UTREDES)
            every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak
            every { behandlingService.finnKlagebehandlingsresultat(any(), any()) } returns klagebehandlingsresultat

            val filtrertListeKlager = behandlingService.hentKlagerIkkeMedholdFormkravAvvist(behandling.id)

            assertEquals(1, filtrertListeKlager.size)
            assertEquals(BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST, filtrertListeKlager[0].resultat)
        }

        @Test
        fun `Skal filtrere bort Klagebehandlingsresultat hvis behandlingResultat ikke er IKKE_MEDHOLD_FORMKRAV_AVVIST`() {
            val behandlingId = UUID.randomUUID()
            val fagsak = fagsak()
            val klagebehandlingsresultat =
                listOf(
                    lagKlageBehandlingsresultat(BehandlingResultat.MEDHOLD),
                    lagKlageBehandlingsresultat(BehandlingResultat.IKKE_SATT),
                    lagKlageBehandlingsresultat(BehandlingResultat.IKKE_MEDHOLD),
                    lagKlageBehandlingsresultat(BehandlingResultat.HENLAGT),
                )

            val behandling = behandling(id = behandlingId, fagsak = fagsak, status = BehandlingStatus.UTREDES)
            every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak
            every { behandlingService.finnKlagebehandlingsresultat(any(), any()) } returns klagebehandlingsresultat

            val filtrertListeKlager = behandlingService.hentKlagerIkkeMedholdFormkravAvvist(behandling.id)

            assertEquals(0, filtrertListeKlager.size)
        }
    }

    @Nested
    inner class OppdaterBehandlendeEnhet {
        @Test
        fun `skal kaste feil om behandlingen er ferdigstilt`() {
            // Arrange
            val fagsak = fagsak(stønadstype = Stønadstype.BARNETRYGD)
            val behandling =
                behandling(
                    fagsak = fagsak,
                    behandlendeEnhet = BarnetrygdEnhet.DRAMMEN.enhetsnummer,
                    status = BehandlingStatus.FERDIGSTILT,
                )

            every { behandlingRepository.findByIdOrThrow(behandling.id) } returns behandling

            // Act & assert
            val exception = assertThrows<Feil> {
                behandlingService.oppdaterBehandlendeEnhet(behandling.id, BarnetrygdEnhet.OSLO, fagsak.fagsystem)
            }
            assertThat(exception.message).isEqualTo("Kan ikke endre behandlende enhet på ferdigstilt behandling=${behandling.id}")
        }

        @Test
        fun `skal oppdatere behandlende enhet på behandling`() {
            // Arrange
            val fagsak = fagsak(stønadstype = Stønadstype.BARNETRYGD)
            val behandling =
                behandling(
                    fagsak = fagsak,
                    behandlendeEnhet = BarnetrygdEnhet.DRAMMEN.enhetsnummer,
                )

            every { behandlingRepository.findByIdOrThrow(behandling.id) } returns behandling

            // Act
            behandlingService.oppdaterBehandlendeEnhet(behandling.id, BarnetrygdEnhet.OSLO, fagsak.fagsystem)

            // Assert
            assertThat(behandlingSlot.captured.behandlendeEnhet).isEqualTo(BarnetrygdEnhet.OSLO.enhetsnummer)
        }

        @ParameterizedTest
        @EnumSource(BarnetrygdEnhet::class, names = ["MIDLERTIDIG_ENHET"], mode = EnumSource.Mode.INCLUDE)
        fun `skal kaste feil dersom ny behandlende enhet ikke er gyldig for BA`(barnetrygdEnhet: BarnetrygdEnhet) {
            // Arrange
            val fagsak = fagsak(stønadstype = Stønadstype.BARNETRYGD)
            val behandling =
                behandling(
                    fagsak = fagsak,
                    behandlendeEnhet = BarnetrygdEnhet.DRAMMEN.enhetsnummer,
                )

            every { behandlingRepository.findByIdOrThrow(behandling.id) } returns behandling

            // Act & Assert
            val apiFeil =
                assertThrows<ApiFeil> { behandlingService.oppdaterBehandlendeEnhet(behandling.id, barnetrygdEnhet, fagsak.fagsystem) }
            assertThat(
                apiFeil.feilmelding,
            ).isEqualTo(
                "Kan ikke oppdatere behandlende enhet til ${barnetrygdEnhet.enhetsnummer}. Dette er ikke et gyldig enhetsnummer for barnetrygd.",
            )
        }

        @ParameterizedTest
        @EnumSource(KontantstøtteEnhet::class, names = ["MIDLERTIDIG_ENHET"], mode = EnumSource.Mode.INCLUDE)
        fun `skal kaste feil dersom ny behandlende enhet ikke er gyldig for KS`(kontantstøtteEnhet: KontantstøtteEnhet) {
            // Arrange
            val fagsak = fagsak(stønadstype = Stønadstype.KONTANTSTØTTE)
            val behandling =
                behandling(
                    fagsak = fagsak,
                    behandlendeEnhet = KontantstøtteEnhet.DRAMMEN.enhetsnummer,
                )

            every { behandlingRepository.findByIdOrThrow(behandling.id) } returns behandling

            // Act & Assert
            val apiFeil =
                assertThrows<ApiFeil> { behandlingService.oppdaterBehandlendeEnhet(behandling.id, kontantstøtteEnhet, fagsak.fagsystem) }
            assertThat(
                apiFeil.feilmelding,
            ).isEqualTo(
                "Kan ikke oppdatere behandlende enhet til ${kontantstøtteEnhet.enhetsnummer}. Dette er ikke et gyldig enhetsnummer for kontantstøtte.",
            )
        }

        @ParameterizedTest
        @EnumSource(Stønadstype::class, names = ["OVERGANGSSTØNAD", "BARNETILSYN", "SKOLEPENGER"])
        fun `skal kaste feil dersom fagsystem er EF`(stønadstype: Stønadstype) {
            // Arrange
            val fagsak = fagsak(stønadstype = stønadstype)
            val behandling =
                behandling(
                    fagsak = fagsak,
                    behandlendeEnhet = "1234",
                )

            every { behandlingRepository.findByIdOrThrow(behandling.id) } returns behandling

            // Act & Assert
            val apiFeil =
                assertThrows<ApiFeil> { behandlingService.oppdaterBehandlendeEnhet(behandling.id, BarnetrygdEnhet.OSLO, fagsak.fagsystem) }
            assertThat(apiFeil.feilmelding).isEqualTo("Støtter ikke endring av enhet for fagsystem EF")
        }
    }

    fun mockHentFagsystemVedtak(
        behandling: Behandling,
        eksternBehandlingId: String,
    ) {
        val fagsystemVedtak = fagsystemVedtak(eksternBehandlingId = eksternBehandlingId)
        every {
            fagsystemVedtakService.hentFagsystemVedtakForPåklagetBehandlingId(behandling.id, eksternBehandlingId)
        } returns fagsystemVedtak
    }

    fun lagKlageBehandlingsresultat(resultat: BehandlingResultat): Klagebehandlingsresultat =
        Klagebehandlingsresultat(
            id = UUID.randomUUID(),
            fagsakId = UUID.randomUUID(),
            fagsakPersonId = UUID.randomUUID(),
            status = BehandlingStatus.FERDIGSTILT,
            opprettet = LocalDateTime.now(),
            mottattDato = LocalDate.now(),
            resultat = resultat,
            årsak = null,
            vedtaksdato = null,
            henlagtÅrsak = null,
        )
}
