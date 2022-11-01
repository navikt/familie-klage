package no.nav.familie.klage.formkrav

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.formkrav.dto.tilDto
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.oppfyltForm
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID

internal class FormServiceTest {

    private val formRepository = mockk<FormRepository>()
    private val stegService = mockk<StegService>()
    private val behandlingService = mockk<BehandlingService>()
    private val vurderingService = mockk<VurderingService>()
    private val taskRepository = mockk<TaskRepository>()
    private val behandlingshistorikkService = mockk<BehandlingshistorikkService>()
    private val service = FormService(
        formRepository,
        stegService,
        behandlingService,
        behandlingshistorikkService,
        vurderingService,
        taskRepository
    )

    private val behandlingId = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        mockkObject(SikkerhetContext)

        every { SikkerhetContext.hentSaksbehandler(any()) } returns "saksbehandler"
        justRun { stegService.oppdaterSteg(any(), any(), any(), any()) }
        justRun { behandlingService.oppdaterPåklagetVedtak(any(), any()) }
        justRun { vurderingService.slettVurderingForBehandling(any()) }
        every { behandlingService.hentBehandling(any()) } returns behandling(id = behandlingId)
        every { formRepository.findByIdOrNull(any()) } returns Form(behandlingId)
        every { formRepository.update(any()) } answers { firstArg() }
    }

    @Nested
    inner class oppdaterForm {

        @Test
        internal fun `ikke ferdigutfylt skal gå til steget formKrav`() {
            service.oppdaterForm(ikkeFerdigutfylt())

            verify { stegService.oppdaterSteg(behandlingId, any(), StegType.FORMKRAV) }
            verify { formRepository.update(any()) }
        }

        @Test
        internal fun `ferdigutfylt og oppfylt skal gå videre til vurdering`() {
            service.oppdaterForm(oppfyltFormDto())

            verify { stegService.oppdaterSteg(behandlingId, any(), StegType.VURDERING) }
            verify { formRepository.update(any()) }
        }

        @Test
        internal fun `ferdigutfylt men ikke oppfylt skal gå videre til brev`() {
            service.oppdaterForm(ikkeOppfyltFormDto())

            verify { stegService.oppdaterSteg(behandlingId, any(), StegType.BREV) }
            verify { vurderingService.slettVurderingForBehandling(behandlingId) }
            verify { formRepository.update(any()) }
        }

        @Test
        internal fun `ingen behandlingshistorikk av StegType FORMKRAV, skal opprette task for statistikk`() {
            val behandlingshistorikk = Behandlingshistorikk(
                UUID.randomUUID(),
                UUID.randomUUID(),
                StegType.OPPRETTET
            )
            every { behandlingshistorikkService.hentBehandlingshistorikk(any()) } returns listOf(
                behandlingshistorikk
            )
            every { taskRepository.save(any()) } returns mockk<Task>()
            service.oppdaterForm(oppfyltFormDto())
            verify { taskRepository.save(any()) }
        }

        @Test
        internal fun `finnes behandlingshistorikk av StegType FORMKRAV, skal ikke opprette task for statistikk`() {
            val behandlingshistorikk = Behandlingshistorikk(
                UUID.randomUUID(),
                UUID.randomUUID(),
                StegType.FORMKRAV
            )
            every { behandlingshistorikkService.hentBehandlingshistorikk(any()) } returns listOf(
                behandlingshistorikk
            )
            every { taskRepository.save(any()) } returns mockk<Task>()
            service.oppdaterForm(oppfyltFormDto())
            verify(exactly = 0) { taskRepository.save(any()) }
        }
    }

    private fun oppfyltFormDto() = oppfyltForm(behandlingId).tilDto(DomainUtil.påklagetVedtakDto())

    private fun ikkeOppfyltFormDto() =
        oppfyltForm(behandlingId).tilDto(DomainUtil.påklagetVedtakDto()).copy(klagePart = FormVilkår.IKKE_OPPFYLT)

    private fun ikkeFerdigutfylt() =
        oppfyltForm(behandlingId).tilDto(DomainUtil.påklagetVedtakDto()).copy(klagePart = FormVilkår.IKKE_SATT)
}
