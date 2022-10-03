package no.nav.familie.klage.infrastruktur.sikkerhet

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.felles.domain.AuditLogger
import no.nav.familie.klage.felles.domain.BehandlerRolle
import no.nav.familie.klage.infrastruktur.config.RolleConfigTestUtil
import no.nav.familie.klage.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.klage.testutil.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

internal class TilgangServiceTest {

    private val personopplysningerIntegrasjonerClient = mockk<PersonopplysningerIntegrasjonerClient>()
    private val rolleConfig = RolleConfigTestUtil.rolleConfig
    private val cacheManager = ConcurrentMapCacheManager()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()

    private val tilgangService = TilgangService(
        personopplysningerIntegrasjonerClient,
        rolleConfig,
        cacheManager,
        auditLogger,
        behandlingService,
        fagsakService
    )

    private val fagsakEf = fagsak()
    private val behandlingEf = behandling(fagsakEf)
    private val fagsakBa = fagsak(stønadstype = Stønadstype.BARNETRYGD)
    private val behandlingBa = behandling(fagsakBa)

    @BeforeEach
    internal fun setUp() {
        mockFagsakOgBehandling(fagsakEf, behandlingEf)
        mockFagsakOgBehandling(fagsakBa, behandlingBa)
    }

    private fun mockFagsakOgBehandling(fagsak: Fagsak, behandling: Behandling) {
        every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
    }

    @Nested
    inner class TilgangGittRolle {

        @Test
        internal fun `saksbehandler har tilgang til behandling av fagsystem barnetrygd`() {
            testWithBrukerContext(groups = listOf(rolleConfig.ba.saksbehandler)) {
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingBa.id, BehandlerRolle.SAKSBEHANDLER)).isTrue
            }
        }

        @Test
        internal fun `ef-saksbehandler har ikke tilgang til behandling av fagsystem barnetrygd`() {
            testWithBrukerContext(groups = listOf(rolleConfig.ef.saksbehandler)) {
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingBa.id, BehandlerRolle.SAKSBEHANDLER)).isFalse
            }
        }

        @Test
        internal fun `veileder har ikke tilgang som saksbehandler eller beslutter`() {
            testWithBrukerContext(groups = listOf(rolleConfig.ba.veileder)) {
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingBa.id, BehandlerRolle.VEILEDER)).isTrue
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingBa.id, BehandlerRolle.SAKSBEHANDLER)).isFalse
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingBa.id, BehandlerRolle.BESLUTTER)).isFalse
            }
        }

        @Test
        internal fun `saksbehandler har tilgang som veileder og saksbehandler, men ikke beslutter`() {
            testWithBrukerContext(groups = listOf(rolleConfig.ef.saksbehandler)) {
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingEf.id, BehandlerRolle.VEILEDER)).isTrue
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingEf.id, BehandlerRolle.SAKSBEHANDLER)).isTrue
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingEf.id, BehandlerRolle.BESLUTTER)).isFalse
            }
        }

        @Test
        internal fun `beslutter har tilgang som saksbehandler, beslutter og veileder`() {
            testWithBrukerContext(groups = listOf(rolleConfig.ef.beslutter)) {
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingEf.id, BehandlerRolle.VEILEDER)).isTrue
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingEf.id, BehandlerRolle.SAKSBEHANDLER)).isTrue
                assertThat(tilgangService.harTilgangTilBehandlingGittRolle(behandlingEf.id, BehandlerRolle.BESLUTTER)).isTrue
            }
        }
    }
}
