package no.nav.familie.klage.henlegg

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.integrasjoner.FagsystemVedtakService
import no.nav.familie.klage.kabal.KlageresultatRepository
import no.nav.familie.klage.oppgave.OppgaveTaskService
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.testutil.BrukerContextUtil.clearBrukerContext
import no.nav.familie.klage.testutil.BrukerContextUtil.mockBrukerContext
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak.TRUKKET_TILBAKE
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.http.HttpStatus

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class HenleggBehandlingServiceTest {

    val fagsakService = mockk<FagsakService>()
    val behandlingRepository = mockk<BehandlingRepository>()
    val behandlinghistorikkService = mockk<BehandlingshistorikkService>()
    val taskService = mockk<TaskService>()
    val oppgaveTaskService = mockk<OppgaveTaskService>()
    val brevService = mockk<BrevService>()
    val klageresultatRepository = mockk<KlageresultatRepository>()
    val fagsystemVedtakService = mockk<FagsystemVedtakService>()

    val behandlingService = BehandlingService(
        behandlingRepository,
        fagsakService,
        klageresultatRepository,
        fagsystemVedtakService,
    )

    val henleggBehandlingService = HenleggBehandlingService(
        behandlingRepository,
        behandlingService,
        behandlinghistorikkService,
        oppgaveTaskService,
        taskService,
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
        every { oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(any()) } returns mockk()
        every { taskService.save(any()) } returns mockk<Task>()
    }

    @AfterEach
    fun tearDown() {
        clearBrukerContext()
    }

    @Nested
    inner class HenleggBehandling {

        private fun henleggOgForventOk(behandling: Behandling, henlagtÅrsak: HenlagtÅrsak) {
            every {
                behandlingRepository.findByIdOrThrow(any())
            } returns behandling

            henleggBehandlingService.henleggBehandling(behandling.id, HenlagtDto(henlagtÅrsak))
            assertThat(behandlingSlot.captured.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
            assertThat(behandlingSlot.captured.resultat).isEqualTo(BehandlingResultat.HENLAGT)
            assertThat(behandlingSlot.captured.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
            assertThat(behandlingSlot.captured.vedtakDato).isNotNull
        }

        private fun henleggOgForventApiFeilmelding(behandling: Behandling, henlagtÅrsak: HenlagtÅrsak) {
            every {
                behandlingRepository.findByIdOrThrow(any())
            } returns behandling

            val feil: ApiFeil = org.junit.jupiter.api.assertThrows {
                henleggBehandlingService.henleggBehandling(behandling.id, HenlagtDto(henlagtÅrsak))
            }

            assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        internal fun `skal kunne henlegge behandling`() {
            val behandling = behandling(fagsak(), status = BehandlingStatus.UTREDES)
            henleggOgForventOk(behandling, henlagtÅrsak = HenlagtÅrsak.FEILREGISTRERT)
            verify(exactly = 1) { oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(any()) }
        }

        @Test
        internal fun `skal ikke kunne henlegge behandling som er oversendt kabal`() {
            val behandling = behandling(fagsak(), status = BehandlingStatus.VENTER)
            henleggOgForventApiFeilmelding(behandling, HenlagtÅrsak.FEILREGISTRERT)
        }

        @Test
        internal fun `skal ikke kunne henlegge behandling som er ferdigstilt`() {
            val behandling = behandling(fagsak(), status = BehandlingStatus.FERDIGSTILT)
            henleggOgForventApiFeilmelding(behandling, TRUKKET_TILBAKE)
        }

        @Test
        internal fun `henlegg og forvent historikkinnslag`() {
            val behandling = behandling(fagsak(), status = BehandlingStatus.UTREDES)
            henleggOgForventOk(behandling, TRUKKET_TILBAKE)
            verify {
                behandlinghistorikkService.opprettBehandlingshistorikk(
                    behandlingId = any(),
                    steg = StegType.BEHANDLING_FERDIGSTILT,
                    historikkHendelse = null,
                )
            }
            verify(exactly = 1) { oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(any()) }
        }
    }
}
