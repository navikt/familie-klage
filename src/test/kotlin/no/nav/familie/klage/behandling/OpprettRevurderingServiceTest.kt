package no.nav.familie.klage.behandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.integrasjoner.FagsystemVedtakService
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.påklagetVedtakDetaljer
import no.nav.familie.klage.testutil.DomainUtil.tilFagsak
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.KanOppretteRevurderingResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID

internal class OpprettRevurderingServiceTest {

    val behandlingService = mockk<BehandlingService>()
    val fagsystemVedtakService = mockk<FagsystemVedtakService>()
    val service = OpprettRevurderingService(behandlingService, fagsystemVedtakService)

    val fagsak = DomainUtil.fagsakDomain().tilFagsak()
    val behandlingId = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        every { fagsystemVedtakService.kanOppretteRevurdering(behandlingId) } returns
                KanOppretteRevurderingResponse(true, null)
    }

    @Test
    internal fun `kan opprette revurdering for vedtak i infotrygd`() {
        every { behandlingService.hentBehandling(behandlingId) } returns
                behandling(fagsak = fagsak, påklagetVedtak = PåklagetVedtak(PåklagetVedtakstype.INFOTRYGD_ORDINÆRT_VEDTAK, null))

        val kanOppretteRevurdering = service.kanOppretteRevurdering(behandlingId)

        assertThat(kanOppretteRevurdering.kanOpprettes).isTrue
        verify { fagsystemVedtakService.kanOppretteRevurdering(behandlingId) }
    }

    @EnumSource(
        value = FagsystemType::class,
        names = ["ORDNIÆR", "SANKSJON_1_MND"],
        mode = EnumSource.Mode.INCLUDE
    )
    @ParameterizedTest
    internal fun `kan opprette revurdering for vedtak i fagsystem`(fagsystemType: FagsystemType) {
        val påklagetVedtak = PåklagetVedtak(
            PåklagetVedtakstype.VEDTAK,
            påklagetVedtakDetaljer(fagsystemType = fagsystemType)
        )
        every { behandlingService.hentBehandling(behandlingId) } returns
                behandling(fagsak = fagsak, påklagetVedtak = påklagetVedtak)

        val kanOppretteRevurdering = service.kanOppretteRevurdering(behandlingId)

        assertThat(kanOppretteRevurdering.kanOpprettes).isTrue
        verify { fagsystemVedtakService.kanOppretteRevurdering(behandlingId) }
    }

    @EnumSource(
        value = FagsystemType::class,
        names = ["ORDNIÆR", "SANKSJON_1_MND"],
        mode = EnumSource.Mode.EXCLUDE
    )
    @ParameterizedTest
    internal fun `skal ikke opprette revurdering for annet vedtak enn ordinær og sanksjon`(fagsystemType: FagsystemType) {
        val påklagetVedtak = PåklagetVedtak(
            PåklagetVedtakstype.VEDTAK,
            påklagetVedtakDetaljer(fagsystemType = fagsystemType)
        )
        every { behandlingService.hentBehandling(behandlingId) } returns
                behandling(fagsak = fagsak, påklagetVedtak = påklagetVedtak)

        val kanOppretteRevurdering = service.kanOppretteRevurdering(behandlingId)

        assertThat(kanOppretteRevurdering.kanOpprettes).isFalse
        verify(exactly = 0) { fagsystemVedtakService.kanOppretteRevurdering(behandlingId) }
    }

    @EnumSource(
        value = PåklagetVedtakstype::class,
        names = ["VEDTAK", "INFOTRYGD_ORDINÆRT_VEDTAK"],
        mode = EnumSource.Mode.EXCLUDE
    )
    @ParameterizedTest
    internal fun `skal ikke opprette revurdering for andre påklagede vedtakstyper enn vedtak`(påklagetVedtakstype: PåklagetVedtakstype) {
        every { behandlingService.hentBehandling(behandlingId) } returns
                behandling(fagsak = fagsak, påklagetVedtak = PåklagetVedtak(påklagetVedtakstype))

        val kanOppretteRevurdering = service.kanOppretteRevurdering(behandlingId)

        assertThat(kanOppretteRevurdering.kanOpprettes).isFalse
        verify(exactly = 0) { fagsystemVedtakService.kanOppretteRevurdering(behandlingId) }
    }
}