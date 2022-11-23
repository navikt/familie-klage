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
    private val fagsakService = mockk<FagsakService>()
    private val service = FagsystemVedtakService(efSakClient, fagsakService)

    private val fagsakEF = fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD)
    private val fagsakBA = fagsak(stønadstype = Stønadstype.BARNETRYGD)
    private val fagsakKS = fagsak(stønadstype = Stønadstype.KONTANTSTØTTE)

    private val behandlingEf = behandling(fagsakEF)
    private val behandlingBA = behandling(fagsakBA)
    private val behandlingKS = behandling(fagsakKS)

    private val påklagetBehandlingId = "påklagetBehandlingId"

    private val vedtak = fagsystemVedtak(påklagetBehandlingId)

    @BeforeEach
    internal fun setUp() {
        every { fagsakService.hentFagsakForBehandling(behandlingEf.id) } returns fagsakEF
        every { fagsakService.hentFagsakForBehandling(behandlingBA.id) } returns fagsakBA
        every { fagsakService.hentFagsakForBehandling(behandlingKS.id) } returns fagsakKS

        every { efSakClient.hentVedtak(fagsakEF.eksternId) } returns listOf(vedtak)
    }

    @Nested
    inner class hentFagsystemVedtak {

        @Test
        internal fun `skal kalle på ef-klient for ef-behandling`() {
            service.hentFagsystemVedtak(behandlingEf.id)

            verify { efSakClient.hentVedtak(any()) }
        }

        @Test
        internal fun `har ikke lagt inn støtte for barnetrygd`() {
            assertThatThrownBy {
                service.hentFagsystemVedtak(behandlingBA.id)
            }.hasMessageContaining("Ikke implementert henting av vedtak for BA og KS ")
        }

        @Test
        internal fun `har ikke lagt inn støtte for kontantstøtte`() {
            assertThatThrownBy {
                service.hentFagsystemVedtak(behandlingKS.id)
            }.hasMessageContaining("Ikke implementert henting av vedtak for BA og KS ")
        }
    }

    @Nested
    inner class hentFagsystemVedtakForPåklagetBehandlingId {

        @Test
        internal fun `skal returnere fagsystemVedtak`() {
            val fagsystemVedtak = service.hentFagsystemVedtakForPåklagetBehandlingId(behandlingEf.id, påklagetBehandlingId)

            assertThat(fagsystemVedtak).isNotNull
            verify { efSakClient.hentVedtak(any()) }
        }

        @Test
        internal fun `skal kaste feil hvis fagsystemVedtak ikke finnes med forventet eksternBehandlingId`() {
            assertThatThrownBy {
                service.hentFagsystemVedtakForPåklagetBehandlingId(behandlingEf.id, "finnes ikke")
            }.hasMessageContaining("Finner ikke vedtak for behandling")
        }
    }

    @Test
    internal fun name() {
        // denne skal feile for å ikke merge inn denne branch av feil før data er patchet for påklagetVedtakDetaljer
        TODO("Not yet implemented")
    }
}