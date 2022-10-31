package no.nav.familie.klage.brev

import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.formkrav.FormRepository
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.testutil.BrukerContextUtil
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.klage.vurdering.VurderingRepository
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BrevServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    lateinit var brevService: BrevService

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var formRepository: FormRepository

    @Autowired
    lateinit var vurderingRepository: VurderingRepository

    private val fagsak = fagsak()
    private val fagsakFerdigstiltBehandling =
        fagsak(stønadstype = Stønadstype.SKOLEPENGER, fagsakPersonId = fagsak.fagsakPersonId)
    private val behandling = behandling(fagsak, steg = StegType.BREV)
    private val ferdigstiltBehandling = behandling(fagsakFerdigstiltBehandling, status = BehandlingStatus.FERDIGSTILT)

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)

        testoppsettService.lagreFagsak(fagsakFerdigstiltBehandling)
        testoppsettService.lagreBehandling(ferdigstiltBehandling)

        formRepository.insert(DomainUtil.oppfyltForm(behandling.id))
        vurderingRepository.insert(DomainUtil.vurdering(behandling.id))
        BrukerContextUtil.mockBrukerContext()
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Nested
    inner class lagEllerOppdaterBrev {

        @Test
        internal fun `skal ikke kunne lage eller oppdatere når behandlingen er låst`() {
            assertThatThrownBy { brevService.lagBrev(ferdigstiltBehandling.id) }
                .hasMessage("Kan ikke oppdatere brev når behandlingen er låst")
        }

        @Test
        internal fun `skal ikke kunne lage eller oppdatere når behandlingen ikke er i brevsteget`() {
            behandlingRepository.update(behandling.copy(steg = StegType.FORMKRAV))
            assertThatThrownBy { brevService.lagBrev(behandling.id) }
                .hasMessageContaining("Behandlingen er i feil steg ")
        }
    }

    @Nested
    inner class lagBrevSomPdf {

        @Test
        internal fun `kan ikke lage pdf 2 ganger`() {
            brevService.lagBrev(behandling.id)
            brevService.lagBrevPdf(behandling.id)

            assertThatThrownBy { brevService.lagBrevPdf(behandling.id) }
                .hasMessage("Det finnes allerede en lagret pdf")
        }
    }
}
