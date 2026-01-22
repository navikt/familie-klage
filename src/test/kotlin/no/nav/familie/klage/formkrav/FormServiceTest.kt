package no.nav.familie.klage.formkrav

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.behandlingshistorikk.domain.HistorikkHendelse
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.formkrav.dto.tilDto
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.lagBehandlingshistorikk
import no.nav.familie.klage.testutil.DomainUtil.oppfyltForm
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID

class FormServiceTest {
    private val formRepository = mockk<FormRepository>()
    private val stegService = mockk<StegService>()
    private val behandlingService = mockk<BehandlingService>()
    private val vurderingService = mockk<VurderingService>()
    private val taskService = mockk<TaskService>()
    private val behandlingshistorikkService = mockk<BehandlingshistorikkService>()
    private val fagsakService = mockk<FagsakService>()
    private val fagsakMock = mockk<Fagsak>()

    private val service =
        FormService(
            formRepository,
            stegService,
            behandlingService,
            behandlingshistorikkService,
            vurderingService,
            taskService,
            fagsakService,
        )

    private val behandlingId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler(any()) } returns "saksbehandler1"
        justRun { stegService.oppdaterSteg(any(), any(), any(), any()) }
        justRun { behandlingService.oppdaterPåklagetVedtak(any(), any()) }
        justRun { vurderingService.slettVurderingForBehandling(any()) }
        every { behandlingService.hentBehandling(any()) } returns behandling(id = behandlingId)
        every { formRepository.findByIdOrNull(any()) } returns Form(behandlingId)
        every { formRepository.update(any()) } answers { firstArg() }
        every { behandlingshistorikkService.hentBehandlingshistorikk(any()) } returns listOf(lagBehandlingshistorikk())
        every { fagsakMock.eksternId } returns "123"
        every { fagsakMock.fagsystem } returns Fagsystem.BA
        every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsakMock
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @Nested
    inner class OppdaterFormkrav {
        @Test
        fun `ikke alle vilkår besvart skal gå til steget formKrav`() {
            // Arrange
            val formkrav = ikkeFerdigutfylt()

            // Act
            service.oppdaterFormkrav(formkrav)

            // Assert
            verify { stegService.oppdaterSteg(behandlingId, any(), StegType.FORMKRAV) }
            verify { formRepository.update(any()) }
        }

        @Test
        fun `ikke valgt påklaget vedtak skal gå til steget formKrav`() {
            // Arrange
            val formkrav =
                oppfyltFormDto().copy(
                    påklagetVedtak =
                        DomainUtil
                            .påklagetVedtakDto()
                            .copy(påklagetVedtakstype = PåklagetVedtakstype.IKKE_VALGT),
                )

            // Act
            service.oppdaterFormkrav(formkrav)

            // Assert
            verify { vurderingService.slettVurderingForBehandling(behandlingId) }
            verify { stegService.oppdaterSteg(behandlingId, any(), StegType.FORMKRAV) }
            verify { formRepository.update(any()) }
        }

        @Test
        fun `påklaget vedtak og oppfylte vilkår skal gå videre til vurdering`() {
            // Arrange
            val formkrav = oppfyltFormDto()

            // Act
            service.oppdaterFormkrav(formkrav)

            // Assert
            verify { stegService.oppdaterSteg(behandlingId, any(), StegType.VURDERING) }
            verify { formRepository.update(any()) }
        }

        @Test
        fun `påklaget vedtak og oppfylte vilkår skal ikke behøve begrunnelse`() {
            // Arrange
            val formkrav = oppfyltFormDto().copy(saksbehandlerBegrunnelse = "")

            // Act
            service.oppdaterFormkrav(formkrav)

            // Assert
            verify { stegService.oppdaterSteg(behandlingId, any(), StegType.VURDERING) }
            verify { formRepository.update(any()) }
        }

        @Test
        fun `påklaget vedtak og underkjente vilkår uten begrunnelse skal gå til steget formkrav`() {
            // Arrange
            val formkrav = ikkeOppfyltFormDto().copy(saksbehandlerBegrunnelse = "")

            // Act
            service.oppdaterFormkrav(formkrav)

            // Assert
            verify { stegService.oppdaterSteg(behandlingId, any(), StegType.FORMKRAV) }
            verify { formRepository.update(any()) }
        }

        @Test
        fun `påklaget vedtak og underkjente vilkår med begrunnelse skal gå videre til brev`() {
            // Arrange
            val formkrav = ikkeOppfyltFormDto()

            // Act
            service.oppdaterFormkrav(formkrav)

            // Assert
            verify { stegService.oppdaterSteg(behandlingId, any(), StegType.BREV) }
            verify { vurderingService.slettVurderingForBehandling(behandlingId) }
            verify { formRepository.update(any()) }
        }

        @Test
        fun `ingen behandlingshistorikk av StegType FORMKRAV, skal opprette task for statistikk`() {
            // Arrange
            val formkrav = oppfyltFormDto()

            val behandlingshistorikk = lagBehandlingshistorikk(historikkHendelse = null, steg = StegType.OPPRETTET)

            every { behandlingshistorikkService.hentBehandlingshistorikk(any()) } returns listOf(behandlingshistorikk)
            every { taskService.save(any()) } returns mockk<Task>()

            // Act
            service.oppdaterFormkrav(formkrav)

            // Assert
            verify { taskService.save(any()) }
        }

        @Test
        fun `finnes behandlingshistorikk av StegType FORMKRAV, skal ikke opprette task for statistikk`() {
            // Arrange
            val formkrav = oppfyltFormDto()

            val behandlingshistorikk = lagBehandlingshistorikk(historikkHendelse = null, steg = StegType.FORMKRAV)
            every { SikkerhetContext.hentSaksbehandler(any()) } returns "saksbehandler"
            every { behandlingshistorikkService.hentBehandlingshistorikk(any()) } returns listOf(behandlingshistorikk)
            every { taskService.save(any()) } returns mockk<Task>()

            // Act
            service.oppdaterFormkrav(formkrav)

            // Assert
            verify(exactly = 0) { taskService.save(any()) }
        }

        @Test
        fun `finnes behandlingshistorikk av StegType FORMKRAV og historikkhendelse BEHANDLENDE_ENHET_ENDRET, skal ikke opprette task for statistikk`() {
            // Arrange
            val formkrav = oppfyltFormDto()

            val behandlingshistorikkFormkrav = lagBehandlingshistorikk(steg = StegType.FORMKRAV)
            val behandlingshistorikkEndretEnhet = lagBehandlingshistorikk(historikkHendelse = HistorikkHendelse.BEHANDLENDE_ENHET_ENDRET, steg = StegType.VURDERING)

            val behandlingshistorikk = listOf(behandlingshistorikkFormkrav, behandlingshistorikkEndretEnhet)

            every { SikkerhetContext.hentSaksbehandler(any()) } returns "saksbehandler"
            every { behandlingshistorikkService.hentBehandlingshistorikk(any()) } returns behandlingshistorikk
            every { taskService.save(any()) } returns mockk<Task>()

            // Act
            service.oppdaterFormkrav(formkrav)

            // Assert
            verify(exactly = 0) { taskService.save(any()) }
        }

        @Test
        fun `finnes historikkhendelse BEHANDLENDE_ENHET_ENDRET, skal ikke opprette task for statistikk`() {
            // Arrange
            val formkrav = oppfyltFormDto()
            val behandlingshistorikk = listOf(lagBehandlingshistorikk(historikkHendelse = HistorikkHendelse.BEHANDLENDE_ENHET_ENDRET, steg = StegType.VURDERING))

            every { SikkerhetContext.hentSaksbehandler(any()) } returns "saksbehandler"
            every { behandlingshistorikkService.hentBehandlingshistorikk(any()) } returns behandlingshistorikk
            every { taskService.save(any()) } returns mockk<Task>()

            // Act
            service.oppdaterFormkrav(formkrav)

            // Assert
            verify(exactly = 0) { taskService.save(any()) }
        }
    }

    private fun oppfyltFormDto() = oppfyltForm(behandlingId).tilDto(DomainUtil.påklagetVedtakDto())

    private fun ikkeOppfyltFormDto() =
        oppfyltForm(behandlingId).tilDto(DomainUtil.påklagetVedtakDto()).copy(
            klagePart = FormVilkår.IKKE_OPPFYLT,
            saksbehandlerBegrunnelse = "Ok",
            brevtekst = "brevtekst",
        )

    private fun ikkeFerdigutfylt() = oppfyltForm(behandlingId).tilDto(DomainUtil.påklagetVedtakDto()).copy(klagePart = FormVilkår.IKKE_SATT)
}
