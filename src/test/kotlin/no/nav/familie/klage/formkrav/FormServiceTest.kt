package no.nav.familie.klage.formkrav

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.domain.FormVilkår
import no.nav.familie.klage.formkrav.dto.tilDto
import no.nav.familie.klage.testutil.DomainUtil.oppfyltForm
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID

internal class FormServiceTest {

    private val formRepository = mockk<FormRepository>()
    private val stegService = mockk<StegService>()
    private val service = FormService(formRepository, stegService)

    private val behandlingId = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        justRun { stegService.oppdaterSteg(any(), any()) }
        every { formRepository.findByIdOrNull(any()) } returns Form(behandlingId)
        every { formRepository.update(any()) } answers { firstArg() }
    }

    @Nested
    inner class oppdaterForm {

        @Test
        internal fun `ikke ferdigutfylt skal gå til steget formKrav`() {
            service.oppdaterForm(ikkeFerdigutfylt())

            verify { stegService.oppdaterSteg(behandlingId, StegType.FORMKRAV) }
            verify { formRepository.update(any()) }
        }

        @Test
        internal fun `ferdigutfylt og oppfylt skal gå videre til vurdering`() {
            service.oppdaterForm(oppfyltFormDto())

            verify { stegService.oppdaterSteg(behandlingId, StegType.VURDERING) }
            verify { formRepository.update(any()) }
        }

        @Test
        internal fun `ferdigutfylt men ikke oppfylt skal gå videre til brev`() {
            service.oppdaterForm(ikkeOppfyltFormDto())

            verify { stegService.oppdaterSteg(behandlingId, StegType.BREV) }
            verify { formRepository.update(any()) }
        }
    }

    private fun oppfyltFormDto() = oppfyltForm(behandlingId).tilDto()

    private fun ikkeOppfyltFormDto() = oppfyltForm(behandlingId).tilDto().copy(klagePart = FormVilkår.IKKE_OPPFYLT)

    private fun ikkeFerdigutfylt() = oppfyltForm(behandlingId).tilDto().copy(klagePart = FormVilkår.IKKE_SATT)
}
