package no.nav.familie.klage.blankett

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.dto.tilDto
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.formkrav.dto.tilDto
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.klage.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.klage.testutil.DomainUtil.oppfyltForm
import no.nav.familie.klage.testutil.DomainUtil.vurderingDto
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.klage.vurdering.domain.Hjemmel
import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.kontrakter.felles.klage.Årsak
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BlankettServiceTest {

    private val behandlingService = mockk<BehandlingService>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val formService = mockk<FormService>()
    private val vurderingService = mockk<VurderingService>()
    private val blankettClient = mockk<BlankettClient>()

    private val service = BlankettService(
        behandlingService,
        personopplysningerService,
        formService,
        vurderingService,
        blankettClient
    )

    private val blankettRequestSpot = slot<BlankettPdfRequest>()
    private val fagsak = fagsak()
    private val behandling = behandling(
        påklagetVedtak = PåklagetVedtak("eksternId", PåklagetVedtakstype.VEDTAK)
    ).tilDto(fagsak, emptyList())

    @BeforeEach
    internal fun setUp() {
        val behandlingId = behandling.id
        every { behandlingService.hentBehandlingDto(behandlingId) } returns behandling
        every { behandlingService.hentAktivIdent(behandlingId) } returns Pair("ident", fagsak)
        val personopplysningerDto = mockk<PersonopplysningerDto>()
        every { personopplysningerDto.navn } returns "navn"
        every { personopplysningerService.hentPersonopplysninger(behandlingId) } returns personopplysningerDto
        every { formService.hentFormDto(behandlingId) } returns oppfyltForm(behandlingId).tilDto(mockk())
        every { vurderingService.hentVurderingDto(behandlingId) } returns vurderingDto(
            vedtak = Vedtak.OPPRETTHOLD_VEDTAK,
            årsak = Årsak.FEIL_I_LOVANDVENDELSE,
            hjemmel = Hjemmel.BT_FEM,
            interntNotat = "interntNotat",
            innstillingKlageinstans = "innstillingKlageinstans"
        )
        every { blankettClient.genererBlankett(capture(blankettRequestSpot)) } returns byteArrayOf()
    }

    @Test
    internal fun `validerer json-request`() {
        service.lagBlankett(behandling.id)

        val blankettRequest = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(blankettRequestSpot.captured)
        val expected = this::class.java.classLoader.getResource("blankett/request.json")!!.readText()
        assertThat(blankettRequest).isEqualTo(expected)
    }
}
