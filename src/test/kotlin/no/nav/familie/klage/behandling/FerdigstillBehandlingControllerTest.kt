package no.nav.familie.klage.behandling

import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.distribusjon.KlageresultatService
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.infrastruktur.config.RolleConfig
import no.nav.familie.klage.testutil.BrukerContextUtil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsakDomain
import no.nav.familie.klage.testutil.DomainUtil.form
import no.nav.familie.klage.testutil.DomainUtil.fritekstbrev
import no.nav.familie.klage.testutil.DomainUtil.tilFagsak
import no.nav.familie.klage.testutil.DomainUtil.vurdering
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import java.util.UUID

internal class FerdigstillBehandlingControllerTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var klageresultatService: KlageresultatService

    @Autowired
    private lateinit var formService: FormService

    @Autowired
    private lateinit var vurderingService: VurderingService

    @Autowired
    private lateinit var brevService: BrevService

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    val fagsak = fagsakDomain().tilFagsak()
    val behandling = behandling(fagsakId = fagsak.id)
    val form = form(fagsakId = fagsak.id, behandlingId = behandling.id)
    val vurdering = vurdering(behandlingId = behandling.id)
    val fritekstbrev = fritekstbrev(behandlingId = behandling.id)

    @BeforeEach
    internal fun setUp() {
        headers.setBearerAuth(lokalTestToken)
        BrukerContextUtil.mockBrukerContext(groups = listOf(rolleConfig.saksbehandlerRolle))

        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)

        formService.opprettEllerOppdaterForm(form)
        vurderingService.opprettEllerOppdaterVurdering(vurdering)

        brevService.lagEllerOppdaterBrev(fritekstbrev)
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `skal ferdigstille behandling og oppdatere verdier i klageresultat`() {
        ferdigstill(behandlingId = behandling.id)
        val klageresultat = klageresultatService.hentEllerOpprettKlageresultat(behandlingId = behandling.id)
        Assertions.assertThat(klageresultat.journalpostId).isNotNull
        Assertions.assertThat(klageresultat.distribusjonId).isNotNull
        Assertions.assertThat(klageresultat.oversendtTilKabalTidspunkt).isNotNull
    }

    private fun ferdigstill(behandlingId: UUID) {
        restTemplate.exchange<ResponseEntity<Ressurs<Nothing>>>(
            localhost("/api/behandling/$behandlingId/ferdigstill"),
            HttpMethod.POST,
            HttpEntity(null, headers)
        )
    }
}
