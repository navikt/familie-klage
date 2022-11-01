package no.nav.familie.klage.formkrav

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.formkrav.dto.tilDto
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.oppfyltForm
import no.nav.familie.klage.vurdering.VurderingService
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
    private val service = FormService(formRepository, stegService, behandlingService, vurderingService)

    private val behandlingId = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
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
        internal fun `ikke alle vilkår besvart skal gå til steget formKrav`() {
            service.oppdaterFormkrav(ikkeFerdigutfylt())

            verify { stegService.oppdaterSteg(behandlingId, any(), StegType.FORMKRAV) }
            verify { formRepository.update(any()) }
        }

        @Test
        internal fun `ikke valgt påklaget vedtak skal gå til steget formKrav`() {
            service.oppdaterFormkrav(oppfyltFormDto().copy(påklagetVedtak = DomainUtil.påklagetVedtakDto()
                    .copy(påklagetVedtakstype = PåklagetVedtakstype.IKKE_VALGT)))

            verify { stegService.oppdaterSteg(behandlingId, any(), StegType.FORMKRAV) }
            verify { formRepository.update(any()) }
        }

        @Test
        internal fun `påklaget vedtak og oppfylte vilkår skal gå videre til vurdering`() {
            service.oppdaterFormkrav(oppfyltFormDto())

            verify { stegService.oppdaterSteg(behandlingId, any(), StegType.VURDERING) }
            verify { formRepository.update(any()) }
        }

        @Test
        internal fun `påklaget vedtak og oppfylte vilkår skal ikke behøve begrunnelse`() {
            service.oppdaterFormkrav(oppfyltFormDto().copy(saksbehandlerBegrunnelse = ""))

            verify { stegService.oppdaterSteg(behandlingId, any(), StegType.VURDERING) }
            verify { formRepository.update(any()) }
        }

        @Test
        internal fun `påklaget vedtak og underkjente vilkår uten begrunnelse skal gå til steget formkrav`() {
            service.oppdaterFormkrav(ikkeOppfyltFormDto().copy(saksbehandlerBegrunnelse = ""))

            verify { stegService.oppdaterSteg(behandlingId, any(), StegType.FORMKRAV) }
            verify { formRepository.update(any()) }
        }

        @Test
        internal fun `påklaget vedtak og underkjente vilkår med begrunnelse skal gå videre til brev`() {
            service.oppdaterFormkrav(ikkeOppfyltFormDto())

            verify { stegService.oppdaterSteg(behandlingId, any(), StegType.BREV) }
            verify { vurderingService.slettVurderingForBehandling(behandlingId) }
            verify { formRepository.update(any()) }
        }
    }

    private fun oppfyltFormDto() = oppfyltForm(behandlingId).tilDto(DomainUtil.påklagetVedtakDto())

    private fun ikkeOppfyltFormDto() = oppfyltForm(behandlingId).tilDto(DomainUtil.påklagetVedtakDto()).copy(klagePart = FormVilkår.IKKE_OPPFYLT,
                                                                                                             saksbehandlerBegrunnelse = "Ok",
                                                                                                             brevtekst = "brevtekst")

    private fun ikkeFerdigutfylt() = oppfyltForm(behandlingId).tilDto(DomainUtil.påklagetVedtakDto()).copy(klagePart = FormVilkår.IKKE_SATT)
}
