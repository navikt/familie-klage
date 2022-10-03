package no.nav.familie.klage.distribusjon

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.kabal.KabalService
import no.nav.familie.klage.oppgave.OppgaveTaskService
import no.nav.familie.klage.testutil.BrukerContextUtil
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.tilFagsak
import no.nav.familie.klage.testutil.DomainUtil.vurdering
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class FerdigstillBehandlingServiceTest {

    val fagsakService = mockk<FagsakService>()
    val behandlingService = mockk<BehandlingService>()
    val distribusjonService = mockk<DistribusjonService>()
    val kabalService = mockk<KabalService>()
    val vurderingService = mockk<VurderingService>()
    val formService = mockk<FormService>()
    val stegService = mockk<StegService>()
    val taskRepository = mockk<TaskRepository>()
    val oppgaveTaskService = mockk<OppgaveTaskService>()

    val ferdigstillBehandlingService = FerdigstillBehandlingService(
        behandlingService = behandlingService,
        vurderingService = vurderingService,
        formService = formService,
        stegService = stegService,
        taskRepository = taskRepository,
        oppgaveTaskService = oppgaveTaskService
    )
    val fagsak = DomainUtil.fagsakDomain().tilFagsak()
    val behandling = DomainUtil.behandling(fagsak = fagsak, steg = StegType.BREV, status = BehandlingStatus.UTREDES)
    val vurdering = vurdering(behandlingId = behandling.id)
    val journalpostId = "1234"
    val brevDistribusjonId = "9876"

    @BeforeEach
    internal fun setUp() {
        BrukerContextUtil.mockBrukerContext("halla")
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { fagsakService.hentFagsakForBehandling(any()) } returns fagsak
        every { distribusjonService.journalførBrev(any(), any()) } returns journalpostId
        every { distribusjonService.distribuerBrev(any()) } returns brevDistribusjonId
        every { vurderingService.hentVurdering(any()) } returns vurdering
        every { kabalService.sendTilKabal(any(), any(), any()) } just Runs
        every { stegService.oppdaterSteg(any(), any()) } just Runs
        every { formService.formkravErOppfyltForBehandling(any()) } returns true
        every { vurderingService.klageTasIkkeTilFølge(any()) } returns true
        every { behandlingService.oppdaterBehandlingsresultatOgVedtaksdato(any(), any()) } just Runs
        every { taskRepository.save(any()) } answers { firstArg() }
        every { oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(behandling) } just Runs
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `skal ferdigstille behandling`() {
        val stegSlot = slot<StegType>()
        val behandlingsresultatSlot = slot<BehandlingResultat>()
        every { stegService.oppdaterSteg(any(), capture(stegSlot)) } just Runs
        every { behandlingService.oppdaterBehandlingsresultatOgVedtaksdato(any(), capture(behandlingsresultatSlot)) } just Runs

        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)

        assertThat(behandlingsresultatSlot.captured).isEqualTo(BehandlingResultat.IKKE_MEDHOLD)
        assertThat(stegSlot.captured).isEqualTo(StegType.KABAL_VENTER_SVAR)
        verify { taskRepository.save(any()) }
        verify { oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(behandling) }
    }

    @Test
    internal fun `skal ikke sende til kabal hvis formkrav ikke er oppfylt`() {
        val stegSlot = slot<StegType>()
        val behandlingsresultatSlot = slot<BehandlingResultat>()
        every { stegService.oppdaterSteg(any(), capture(stegSlot)) } just Runs
        every { formService.formkravErOppfyltForBehandling(any()) } returns false
        every { behandlingService.oppdaterBehandlingsresultatOgVedtaksdato(any(), capture(behandlingsresultatSlot)) } just Runs
        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)
        assertThat(stegSlot.captured).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
        assertThat(behandlingsresultatSlot.captured).isEqualTo(BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST)
        verify { taskRepository.save(any()) }
    }

    @Test
    internal fun `skal ikke sende til kabal hvis klage tas til følge`() {
        val stegSlot = slot<StegType>()
        val behandlingsresultatSlot = slot<BehandlingResultat>()

        every { behandlingService.oppdaterBehandlingsresultatOgVedtaksdato(any(), capture(behandlingsresultatSlot)) } just Runs
        every { stegService.oppdaterSteg(any(), capture(stegSlot)) } just Runs
        every { vurderingService.klageTasIkkeTilFølge(any()) } returns false
        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)
        assertThat(stegSlot.captured).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
        assertThat(behandlingsresultatSlot.captured).isEqualTo(BehandlingResultat.MEDHOLD)

        verify(exactly = 0) { taskRepository.save(any()) }
    }

    @Test
    internal fun `skal feile dersom behandlingen er på feil steg`() {
        listOf(
            StegType.BEHANDLING_FERDIGSTILT,
            StegType.FORMKRAV,
            StegType.OVERFØRING_TIL_KABAL,
            StegType.VURDERING
        ).forEach { steg ->
            every { behandlingService.hentBehandling(any()) } returns behandling.copy(steg = steg)
            assertThrows<Feil> {
                ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)
            }
        }
    }

    @Test
    internal fun `skal feile dersom behandlingen har feil status`() {
        listOf(
            BehandlingStatus.FERDIGSTILT,
            BehandlingStatus.VENTER
        ).forEach { status ->
            every { behandlingService.hentBehandling(any()) } returns behandling.copy(status = status)
            assertThrows<Feil> {
                ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)
            }
        }
    }
}
