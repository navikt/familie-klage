package no.nav.familie.klage.behandling

import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.distribusjon.DistribusjonResultatService
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
import java.util.UUID

internal class FerdigstillBehandlingControllerTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var distribusjonResultatService: DistribusjonResultatService

    @Autowired
    private lateinit var formService: FormService

    @Autowired
    private lateinit var vurderingService: VurderingService

    @Autowired
    private lateinit var brevService: BrevService

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    val fagsak = fagsakDomain().tilFagsak()
    val behandling = behandling(fagsak = fagsak)
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
    internal fun `skal ferdigstille behandling og oppdatere verdier i distribusjonResultat`() {
        ferdigstill(behandlingId = behandling.id)
        val distribusjonResultat = distribusjonResultatService.hentEllerOpprettDistribusjonResultat(behandlingId = behandling.id)
        Assertions.assertThat(distribusjonResultat.journalpostId).isNotNull
        Assertions.assertThat(distribusjonResultat.brevDistribusjonId).isNotNull
        Assertions.assertThat(distribusjonResultat.oversendtTilKabalTidspunkt).isNotNull
    }

    private fun ferdigstill(behandlingId: UUID) {
        restTemplate.exchange<Ressurs<Unit>>(
            localhost("/api/behandling/$behandlingId/ferdigstill"),
            HttpMethod.POST,
            HttpEntity(null, headers)
        )
    }
}
