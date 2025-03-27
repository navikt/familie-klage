package no.nav.familie.klage.vurdering

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.brev.BrevRepository
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.klage.testutil.DomainUtil.vurdering
import no.nav.familie.klage.vurdering.domain.Hjemmel
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.klage.vurdering.dto.tilDto
import no.nav.familie.kontrakter.felles.klage.Årsak
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID

class VurderingServiceTest {

    val vurderingRepository = mockk<VurderingRepository>()
    val stegService = mockk<StegService>()
    val brevRepository = mockk<BrevRepository>()
    val fagsakService = mockk<FagsakService>()
    val vurderingService = VurderingService(vurderingRepository, stegService, brevRepository, fagsakService)

    val omgjørVedtakVurdering = vurdering(
        behandlingId = UUID.randomUUID(),
        vedtak = Vedtak.OMGJØR_VEDTAK,
        hjemmel = null,
        årsak = Årsak.FEIL_I_LOVANDVENDELSE,
        begrunnelseOmgjøring = "begrunnelse",
    )

    val opprettholdVedtakVurdering = vurdering(
        behandlingId = UUID.randomUUID(),
        vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
        hjemmel = Hjemmel.FT_FEMTEN_FEM,
    )

    @BeforeEach
    fun setup() {
        every { vurderingRepository.findByIdOrNull(any()) } returns omgjørVedtakVurdering
        every { vurderingRepository.update(any()) } answers { firstArg() }
        every { fagsakService.hentFagsakForBehandling(any()) } returns fagsak()
        justRun { stegService.oppdaterSteg(any(), any(), any(), any()) }
        justRun { brevRepository.deleteById(any()) }
    }

    @Test
    internal fun `skal slette brev ved omgjøring`() {
        vurderingService.opprettEllerOppdaterVurdering(omgjørVedtakVurdering.tilDto())
        verify(exactly = 1) { brevRepository.deleteById(any()) }
    }

    @Test
    internal fun `skal ikke slette brev ved opprettholdelse`() {
        vurderingService.opprettEllerOppdaterVurdering(opprettholdVedtakVurdering.tilDto())
        verify(exactly = 0) { brevRepository.deleteById(any()) }
    }

    @Test
    fun `skal oppdatere steg ved omgjøring`() {
        vurderingService.opprettEllerOppdaterVurdering(omgjørVedtakVurdering.tilDto())
        verify(exactly = 1) { stegService.oppdaterSteg(any(), any(), StegType.BREV) }
    }

    @Test
    fun `skal oppdatere steg ved opprettholdelse av klage`() {
        vurderingService.opprettEllerOppdaterVurdering(opprettholdVedtakVurdering.tilDto())
        verify(exactly = 1) { stegService.oppdaterSteg(any(), any(), StegType.BREV) }
    }
}
