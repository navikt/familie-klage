package no.nav.familie.klage.brev

import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.testutil.BrukerContextUtil
import no.nav.familie.klage.testutil.DomainUtil.avsnitt
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.klage.testutil.DomainUtil.fritekstbrev
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
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

    private val fagsak = fagsak()
    private val fagsakFerdigstiltBehandling = fagsak(stønadstype = Stønadstype.SKOLEPENGER, fagsakPersonId = fagsak.fagsakPersonId)
    private val behandling = behandling(fagsak)
    private val ferdigstiltBehandling = behandling(fagsakFerdigstiltBehandling, status = BehandlingStatus.FERDIGSTILT)

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)

        testoppsettService.lagreFagsak(fagsakFerdigstiltBehandling)
        testoppsettService.lagreBehandling(ferdigstiltBehandling)
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
            assertThatThrownBy { brevService.lagEllerOppdaterBrev(fritekstbrev(ferdigstiltBehandling.id)) }
                .hasMessage("Kan ikke oppdatere brev når behandlingen er låst")
        }

        @Test
        internal fun `skal slette tidligere avsnitt når man lagrer på nytt`() {
            val fritekstbrev1 = fritekstbrev(behandling.id)
            val fritekstbrev2 = fritekstbrev(behandling.id, avsnitt = listOf(avsnitt(), avsnitt()))

            brevService.lagEllerOppdaterBrev(fritekstbrev1)
            assertThat(brevService.hentMellomlagretBrev(behandling.id)?.avsnitt!!.map { it.avsnittId })
                .containsExactlyInAnyOrder(fritekstbrev1.avsnitt.single().avsnittId)

            brevService.lagEllerOppdaterBrev(fritekstbrev2)
            assertThat(brevService.hentMellomlagretBrev(behandling.id)?.avsnitt!!.map { it.avsnittId })
                .containsExactlyInAnyOrder(fritekstbrev2.avsnitt[0].avsnittId, fritekstbrev2.avsnitt[1].avsnittId)
        }
    }

    @Nested
    inner class hentMellomlagretBrev {

        @Test
        internal fun `skal ikke kunne hente når behandlingen er låst`() {
            assertThatThrownBy { brevService.hentMellomlagretBrev(ferdigstiltBehandling.id) }
                .hasMessage("Kan ikke hente mellomlagret brev når behandlingen er låst")
        }
    }

    @Nested
    inner class lagBrevSomPdf {

        @Test
        internal fun `skal ikke kunne lage brev når behandlingen er låst`() {
            brevService.lagEllerOppdaterBrev(fritekstbrev(behandling.id))
            behandlingRepository.updateStatus(behandling.id, BehandlingStatus.FERDIGSTILT)

            assertThatThrownBy { brevService.lagBrevSomPdf(behandling.id) }
                .hasMessage("Kan ikke lage pdf når behandlingen er låst")
        }

        @Test
        internal fun `kan ikke lagre brevet 2 ganger`() {
            brevService.lagEllerOppdaterBrev(fritekstbrev(behandling.id))
            brevService.lagBrevSomPdf(behandling.id)

            assertThatThrownBy { brevService.lagBrevSomPdf(behandling.id) }
                .hasMessage("Det finnes allerede en lagret pdf")
        }
    }
}