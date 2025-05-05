package no.nav.familie.klage.behandling

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.klage.behandling.domain.FagsystemRevurdering
import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandlingsstatistikk.BehandlingsstatistikkTask
import no.nav.familie.klage.blankett.LagSaksbehandlingsblankettTask
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.distribusjon.DistribusjonService
import no.nav.familie.klage.distribusjon.JournalførBrevTask
import no.nav.familie.klage.distribusjon.SendTilKabalTask
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.integrasjoner.FagsystemVedtakService
import no.nav.familie.klage.integrasjoner.FeatureToggleMock
import no.nav.familie.klage.kabal.KabalService
import no.nav.familie.klage.oppgave.OppgaveTaskService
import no.nav.familie.klage.testutil.BrukerContextUtil
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.påklagetVedtakDetaljer
import no.nav.familie.klage.testutil.DomainUtil.tilFagsak
import no.nav.familie.klage.testutil.DomainUtil.vurdering
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import no.nav.familie.kontrakter.felles.klage.OpprettRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.Opprettet
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class FerdigstillBehandlingServiceTest {
    private val fagsakService = mockk<FagsakService>()
    private val behandlingService = mockk<BehandlingService>()
    private val distribusjonService = mockk<DistribusjonService>()
    private val kabalService = mockk<KabalService>()
    private val vurderingService = mockk<VurderingService>()

    private val formService = mockk<FormService>()
    private val stegService = mockk<StegService>()
    private val taskService = mockk<TaskService>()
    private val oppgaveTaskService = mockk<OppgaveTaskService>()
    private val brevService = mockk<BrevService>()
    private val fagsystemVedtakService = mockk<FagsystemVedtakService>()
    private val featureToggleService = FeatureToggleMock().featureToggleService()

    private val ferdigstillBehandlingService =
        FerdigstillBehandlingService(
            behandlingService = behandlingService,
            vurderingService = vurderingService,
            formService = formService,
            stegService = stegService,
            taskService = taskService,
            oppgaveTaskService = oppgaveTaskService,
            brevService = brevService,
            fagsystemVedtakService = fagsystemVedtakService,
            featureToggleService = featureToggleService,
            fagsakService = fagsakService,
        )
    private val fagsak = DomainUtil.fagsakDomain().tilFagsak()
    private val behandling = DomainUtil.behandling(fagsak = fagsak, steg = StegType.BREV, status = BehandlingStatus.UTREDES)
    private val vurdering = vurdering(behandlingId = behandling.id)
    private val journalpostId = "1234"
    private val brevDistribusjonId = "9876"

    private val saveTaskSlot = mutableListOf<Task>()

    private val stegSlot = slot<StegType>()
    private val behandlingsresultatSlot = slot<BehandlingResultat>()
    private val fagsystemRevurderingSlot = mutableListOf<FagsystemRevurdering?>()

    @BeforeEach
    fun setUp() {
        fagsystemRevurderingSlot.clear()
        BrukerContextUtil.mockBrukerContext("halla")
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { fagsakService.hentFagsakForBehandling(any()) } returns fagsak
        every { distribusjonService.journalførBrev(any(), any(), any(), any(), any()) } returns journalpostId
        every { distribusjonService.distribuerBrev(any()) } returns brevDistribusjonId
        every { vurderingService.hentVurdering(any()) } returns vurdering
        every { kabalService.sendTilKabal(any(), any(), any(), any(), any()) } just Runs
        justRun { stegService.oppdaterSteg(any(), any(), capture(stegSlot), any()) }
        every { formService.formkravErOppfyltForBehandling(any()) } returns true
        justRun { behandlingService.oppdaterBehandlingMedResultat(any(), capture(behandlingsresultatSlot), null) }
        justRun {
            behandlingService.oppdaterBehandlingMedResultat(
                any(),
                capture(behandlingsresultatSlot),
                captureNullable(fagsystemRevurderingSlot),
            )
        }
        every { taskService.save(capture(saveTaskSlot)) } answers { firstArg() }
        every { oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(behandling.id, any(), any()) } just Runs
        justRun { brevService.lagBrevPdf(any()) }
        every { fagsystemVedtakService.opprettRevurdering(any()) } returns OpprettRevurderingResponse(Opprettet("opprettetId"))
    }

    @AfterEach
    fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    fun `skal ferdigstille behandling, ikke medhold`() {
        // Act
        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)

        // Assert
        assertThat(behandlingsresultatSlot.captured).isEqualTo(BehandlingResultat.IKKE_MEDHOLD)
        assertThat(fagsystemRevurderingSlot.single()).isNull()
        assertThat(stegSlot.captured).isEqualTo(StegType.KABAL_VENTER_SVAR)

        verify(exactly = 4) { taskService.save(any()) }
        assertThat(saveTaskSlot.map { it.type }).containsExactly(
            JournalførBrevTask.TYPE,
            LagSaksbehandlingsblankettTask.TYPE,
            BehandlingsstatistikkTask.TYPE,
            BehandlingsstatistikkTask.TYPE,
        )
        verify { oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(behandling.id, any(), any()) }
    }

    @Test
    fun `skal ikke opprette og distribuere brev ved ferdigstillelse av behandling med årsak henvendelse fra kabal`() {
        // Arrange
        every { behandlingService.hentBehandling(any()) } returns behandling.copy(årsak = Klagebehandlingsårsak.HENVENDELSE_FRA_KABAL)

        // Act
        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)

        // Assert
        assertThat(behandlingsresultatSlot.captured).isEqualTo(BehandlingResultat.IKKE_MEDHOLD)
        assertThat(fagsystemRevurderingSlot.single()).isNull()
        assertThat(stegSlot.captured).isEqualTo(StegType.KABAL_VENTER_SVAR)

        verify(exactly = 4) { taskService.save(any()) }
        assertThat(saveTaskSlot.map { it.type }).containsExactly(
            SendTilKabalTask.TYPE,
            LagSaksbehandlingsblankettTask.TYPE,
            BehandlingsstatistikkTask.TYPE,
            BehandlingsstatistikkTask.TYPE,
        )
        verify { oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(behandling.id, fagsak.eksternId, fagsak.fagsystem) }
    }

    @Test
    fun `skal ikke sende til kabal hvis formkrav ikke er oppfylt`() {
        // Arrange
        every { formService.formkravErOppfyltForBehandling(any()) } returns false

        // Act
        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)

        // Assert
        assertThat(stegSlot.captured).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
        assertThat(behandlingsresultatSlot.captured).isEqualTo(BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST)
        assertThat(fagsystemRevurderingSlot.single()).isNull()

        verify { taskService.save(any()) }
        verify { brevService.lagBrevPdf(any()) }
    }

    @Test
    fun `skal ikke sende til kabal hvis klage tas til følge`() {
        // Arrange
        every { vurderingService.hentVurdering(any()) } returns vurdering.copy(vedtak = Vedtak.OMGJØR_VEDTAK)

        // Act
        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)

        // Assert
        assertThat(stegSlot.captured).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
        assertThat(behandlingsresultatSlot.captured).isEqualTo(BehandlingResultat.MEDHOLD)
        assertThat(fagsystemRevurderingSlot.single()).isNull()

        verify(exactly = 2) { taskService.save(any()) }
        verify(exactly = 0) { fagsystemVedtakService.opprettRevurdering(behandling) }
        assertThat(saveTaskSlot.map { it.type }).containsExactly(
            LagSaksbehandlingsblankettTask.TYPE,
            BehandlingsstatistikkTask.TYPE,
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = StegType::class,
        names = ["BEHANDLING_FERDIGSTILT", "FORMKRAV", "OVERFØRING_TIL_KABAL", "VURDERING"],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun `skal feile dersom behandlingen er på feil steg`(
        stegType: StegType,
    ) {
        // Arrange
        every { behandlingService.hentBehandling(any()) } returns behandling.copy(steg = stegType)

        // Act
        assertThrows<Feil> {
            ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)
        }
    }

    @ParameterizedTest
    @EnumSource(
        value = BehandlingStatus::class,
        names = ["FERDIGSTILT", "VENTER"],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun `skal feile dersom behandlingen har feil status`(status: BehandlingStatus) {
        // Arrange
        every { behandlingService.hentBehandling(any()) } returns behandling.copy(status = status)

        // Act & assert
        assertThrows<Feil> {
            ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)
        }
    }

    @Test
    fun `skal opprette revurdering automatisk påklaget vedtak er vedtak i fagsystemet`() {
        // Arrange
        val behandling = behandling.copy(påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.VEDTAK, påklagetVedtakDetaljer()))

        every { vurderingService.hentVurdering(any()) } returns vurdering.copy(vedtak = Vedtak.OMGJØR_VEDTAK)
        every { behandlingService.hentBehandling(any()) } returns behandling

        // Act
        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId = behandling.id)

        // Assert
        assertThat(fagsystemRevurderingSlot.single()).isNotNull
        verify(exactly = 1) { fagsystemVedtakService.opprettRevurdering(behandling) }
    }
}
