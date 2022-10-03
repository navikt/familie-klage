package no.nav.familie.klage.behandling

import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.formkrav.dto.tilDto
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.infrastruktur.config.RolleConfig
import no.nav.familie.klage.oppgave.BehandleSakOppgave
import no.nav.familie.klage.oppgave.BehandleSakOppgaveRepository
import no.nav.familie.klage.testutil.BrukerContextUtil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsakDomain
import no.nav.familie.klage.testutil.DomainUtil.fritekstbrev
import no.nav.familie.klage.testutil.DomainUtil.oppfyltForm
import no.nav.familie.klage.testutil.DomainUtil.tilFagsak
import no.nav.familie.klage.testutil.DomainUtil.vurderingDto
import no.nav.familie.klage.vurdering.VurderingService
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.UUID
import kotlin.math.absoluteValue
import kotlin.random.Random

internal class FerdigstillBehandlingControllerTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var formService: FormService

    @Autowired
    private lateinit var vurderingService: VurderingService

    @Autowired
    private lateinit var brevService: BrevService

    @Autowired
    private lateinit var behandleSakOppgaveRepository: BehandleSakOppgaveRepository

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    val fagsak = fagsakDomain().tilFagsak()
    val behandling = behandling(fagsak = fagsak)
    val vurdering = vurderingDto(behandlingId = behandling.id)
    val fritekstbrev = fritekstbrev(behandlingId = behandling.id)

    @BeforeEach
    internal fun setUp() {
        headers.setBearerAuth(lokalTestToken)
        BrukerContextUtil.mockBrukerContext(groups = listOf(rolleConfig.ef.saksbehandler))

        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)

        formService.opprettInitielleFormkrav(behandling.id)
        formService.oppdaterForm(oppfyltForm(behandling.id).tilDto())
        vurderingService.opprettEllerOppdaterVurdering(vurdering)

        brevService.lagEllerOppdaterBrev(fritekstbrev)
        behandleSakOppgaveRepository.insert(BehandleSakOppgave(behandlingId = behandling.id, oppgaveId = Random.nextLong().absoluteValue))
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `skal ferdigstille behandling og opprette tasks for distribuering av data til dokarkiv og kabal`() {
        val ferdigstillResponse = ferdigstill(behandlingId = behandling.id)
        assertThat(ferdigstillResponse.statusCode).isEqualTo(HttpStatus.OK)
        // TODO: Trigge plukk  av task - forvent at journalførBrevTask ferdigstilles
        // TODO: Trigge plukk  av task - forvent at sendTilKabalTask og distribuerBrevTask ferdigstilles ?
    }

    private fun ferdigstill(behandlingId: UUID): ResponseEntity<Ressurs<Unit>> {
        return restTemplate.exchange<Ressurs<Unit>>(
            localhost("/api/behandling/$behandlingId/ferdigstill"),
            HttpMethod.POST,
            HttpEntity(null, headers)
        )
    }
}
