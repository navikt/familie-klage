package no.nav.familie.klage.integrasjoner

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.klage.testutil.DomainUtil.fagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class FagsystemVedtakServiceTest {

    private val efSakClient = mockk<FamilieEFSakClient>()
    private val ksSakClient = mockk<FamilieKSSakClient>()
    private val baSakClient = mockk<FamilieBASakClient>()
    private val fagsakService = mockk<FagsakService>()
    private val service = FagsystemVedtakService(
        familieEFSakClient = efSakClient,
        familieKSSakClient = ksSakClient,
        familieBASakClient = baSakClient,
        fagsakService = fagsakService,
    )

    private val fagsakEF = fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD)
    private val fagsakBA = fagsak(stønadstype = Stønadstype.BARNETRYGD)
    private val fagsakKS = fagsak(stønadstype = Stønadstype.KONTANTSTØTTE)

    private val behandlingEF = behandling(fagsakEF)
    private val behandlingBA = behandling(fagsakBA)
    private val behandlingKS = behandling(fagsakKS)

    private val påklagetBehandlingId = "påklagetBehandlingId"

    private val vedtak = fagsystemVedtak(påklagetBehandlingId)

    @BeforeEach
    internal fun setUp() {
        every { fagsakService.hentFagsakForBehandling(behandlingEF.id) } returns fagsakEF
        every { fagsakService.hentFagsakForBehandling(behandlingBA.id) } returns fagsakBA
        every { fagsakService.hentFagsakForBehandling(behandlingKS.id) } returns fagsakKS

        every { efSakClient.hentVedtak(fagsakEF.eksternId) } returns listOf(vedtak)
        every { ksSakClient.hentVedtak(fagsakKS.eksternId) } returns listOf(vedtak)
        every { baSakClient.hentVedtak(fagsakBA.eksternId) } returns listOf(vedtak)
    }

    @Nested
    inner class HentFagsystemVedtak {

        @Test
        internal fun `skal kalle på ef-klient for ef-behandling`() {
            service.hentFagsystemVedtak(behandlingEF.id)

            verify { efSakClient.hentVedtak(any()) }
        }

        @Test
        internal fun `har ikke lagt inn støtte for barnetrygd`() {
            service.hentFagsystemVedtak(behandlingBA.id)

            verify { baSakClient.hentVedtak(any()) }
        }

        @Test
        internal fun `skal kalle på ks-klient for ks-behandling`() {
            service.hentFagsystemVedtak(behandlingKS.id)

            verify { ksSakClient.hentVedtak(any()) }
        }
    }

    @Nested
    inner class HentFagsystemVedtakForPåklagetBehandlingId {

        @Test
        internal fun `skal returnere fagsystemVedtak`() {
            val fagsystemVedtak = service.hentFagsystemVedtakForPåklagetBehandlingId(behandlingEF.id, påklagetBehandlingId)

            assertThat(fagsystemVedtak).isNotNull
            verify { efSakClient.hentVedtak(any()) }
        }

        @Test
        internal fun `skal kaste feil hvis fagsystemVedtak ikke finnes med forventet eksternBehandlingId`() {
            assertThatThrownBy {
                service.hentFagsystemVedtakForPåklagetBehandlingId(behandlingEF.id, "finnes ikke")
            }.hasMessageContaining("Finner ikke vedtak for behandling")
        }
    }
}
