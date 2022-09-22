package no.nav.familie.klage.infrastruktur.sikkerhet

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.felles.domain.AuditLogger
import no.nav.familie.klage.felles.domain.BehandlerRolle
import no.nav.familie.klage.infrastruktur.config.RolleConfigTestUtil
import no.nav.familie.klage.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.klage.testutil.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
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

    private val fagsak = fagsak()
    private val behandling = behandling(fagsak)

    @BeforeEach
    internal fun setUp() {
        every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
    }

    @Nested
    inner class TilgangGittRolle {

        @Test
        internal fun `saksbehandler har tilgang til behandling av fagsystem enslig forsørger`() {
            testWithBrukerContext(groups = listOf(rolleConfig.ef.veileder)) {
                tilgangService.harTilgangTilBehandlingGittRolle(behandling.id, BehandlerRolle.VEILEDER)
            }
        }
    }
}
