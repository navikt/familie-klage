package no.nav.familie.klage.behandling

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandlingsstatistikk.BehandlingsstatistikkTask
import no.nav.familie.klage.blankett.LagSaksbehandlingsblankettTask
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.distribusjon.DistribusjonService
import no.nav.familie.klage.distribusjon.JournalførBrevTask
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
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
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
    val taskService = mockk<TaskService>()
    val oppgaveTaskService = mockk<OppgaveTaskService>()
    val brevService = mockk<BrevService>()

    val ferdigstillBehandlingService = FerdigstillBehandlingService(
        behandlingService = behandlingService,
        vurderingService = vurderingService,
        formService = formService,
        stegService = stegService,
        taskService = taskService,
        oppgaveTaskService = oppgaveTaskService,
        brevService = brevService
    )
    val fagsak = DomainUtil.fagsakDomain().tilFagsak()
    val behandling = DomainUtil.behandling(fagsak = fagsak, steg = StegType.BREV, status = BehandlingStatus.UTREDES)
    val vurdering = vurdering(behandlingId = behandling.id)
    val journalpostId = "1234"
    val brevDistribusjonId = "9876"

    val saveTaskSlot = mutableListOf<Task>()

    @BeforeEach
    internal fun setUp() {
        BrukerContextUtil.mockBrukerContext("halla")
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { fagsakService.hentFagsakForBehandling(any()) } returns fagsak
        every { distribusjonService.journalførBrev(any(), any(), any(), any(), any()) } returns journalpostId
        every { distribusjonService.distribuerBrev(any()) } returns brevDistribusjonId
        every { vurderingService.hentVurdering(any()) } returns vurdering
        every { kabalService.sendTilKabal(any(), any(), any()) } just Runs
        every { stegService.oppdaterSteg(any(), any(), any(), any()) } just Runs
        every { formService.formkravErOppfyltForBehandling(any()) } returns true
        every { behandlingService.oppdaterBehandlingsresultatOgVedtaksdato(any(), any()) } just Runs
        every { taskService.save(capture(saveTaskSlot)) } answers { firstArg() }
        every { oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(behandling.id) } just Runs
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `skal ferdigstille behandling`() {
        val stegSlot = slot<StegType>()
        val behandlingsresultatSlot = slot<BehandlingResultat>()
        every { stegService.oppdaterSteg(any(), any(), capture(stegSlot), any()) } just Runs
        justRun { brevService.lagBrevPdf(any()) }
        every {
            behandlingService.oppdaterBehandlingsresultatOgVedtaksdato(
                any(),
                capture(behandlingsresultatSlot)
            )
        } just Runs

        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)

        assertThat(behandlingsresultatSlot.captured).isEqualTo(BehandlingResultat.IKKE_MEDHOLD)
        assertThat(stegSlot.captured).isEqualTo(StegType.KABAL_VENTER_SVAR)
        verify(exactly = 3) { taskService.save(any()) }
        assertThat(saveTaskSlot.map { it.type }).containsExactly(JournalførBrevTask.TYPE, LagSaksbehandlingsblankettTask.TYPE, BehandlingsstatistikkTask.TYPE)
        verify { oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(behandling.id) }
    }

    @Test
    internal fun `skal ikke sende til kabal hvis formkrav ikke er oppfylt`() {
        val stegSlot = slot<StegType>()
        val behandlingsresultatSlot = slot<BehandlingResultat>()
        justRun { brevService.lagBrevPdf(any()) }
        every { stegService.oppdaterSteg(any(), any(), capture(stegSlot), any()) } just Runs
        every { formService.formkravErOppfyltForBehandling(any()) } returns false
        every {
            behandlingService.oppdaterBehandlingsresultatOgVedtaksdato(
                any(),
                capture(behandlingsresultatSlot)
            )
        } just Runs
        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)
        assertThat(stegSlot.captured).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
        assertThat(behandlingsresultatSlot.captured).isEqualTo(BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST)
        verify { taskService.save(any()) }
    }

    @Test
    internal fun `skal ikke sende til kabal hvis klage tas til følge`() {
        val stegSlot = slot<StegType>()
        val behandlingsresultatSlot = slot<BehandlingResultat>()

        every {
            behandlingService.oppdaterBehandlingsresultatOgVedtaksdato(
                any(),
                capture(behandlingsresultatSlot)
            )
        } just Runs
        every { stegService.oppdaterSteg(any(), any(), capture(stegSlot), any()) } just Runs
        every { vurderingService.hentVurdering(any()) } returns vurdering.copy(vedtak = Vedtak.OMGJØR_VEDTAK)
        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)
        assertThat(stegSlot.captured).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
        assertThat(behandlingsresultatSlot.captured).isEqualTo(BehandlingResultat.MEDHOLD)

        verify(exactly = 2) { taskService.save(any()) }
        assertThat(saveTaskSlot.map { it.type }).containsExactly(LagSaksbehandlingsblankettTask.TYPE, BehandlingsstatistikkTask.TYPE)
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
