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
    private val fagsakFerdigstiltBehandling =
        fagsak(stønadstype = Stønadstype.SKOLEPENGER, fagsakPersonId = fagsak.fagsakPersonId)
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
        internal fun `skal slette tidligere avsnitt når man lagrer på nytt`() {
            val fritekstbrev1 = fritekstbrev(behandling.id, avsnitt = listOf(avsnitt(), avsnitt()))
            val fritekstbrev2 = fritekstbrev(behandling.id)

            brevService.lagEllerOppdaterBrev(fritekstbrev1)
            assertThat(brevService.hentMellomlagretBrev(behandling.id)!!.avsnitt).hasSize(2)

            brevService.lagEllerOppdaterBrev(fritekstbrev2)
            assertThat(brevService.hentMellomlagretBrev(behandling.id)!!.avsnitt).hasSize(1)
        }

        @Test
        internal fun `skal generere et nytt avsnittId for avsnitt som sendes inn fra frontend`() {
            val avsnitt = avsnitt()
            val fritekstbrev = fritekstbrev(behandling.id, avsnitt = listOf(avsnitt))

            brevService.lagEllerOppdaterBrev(fritekstbrev)
            val lagredeAvsnitt = brevService.hentMellomlagretBrev(behandling.id)!!.avsnitt
            assertThat(lagredeAvsnitt).hasSize(1)
            assertThat(lagredeAvsnitt[0].avsnittId).isNotEqualTo(avsnitt.avsnittId)

            brevService.lagEllerOppdaterBrev(fritekstbrev)
            val lagredeAvsnitt2 = brevService.hentMellomlagretBrev(behandling.id)!!.avsnitt
            assertThat(lagredeAvsnitt).hasSize(1)
            assertThat(lagredeAvsnitt2[0].avsnittId).isNotEqualTo(avsnitt.avsnittId)
            assertThat(lagredeAvsnitt2[0].avsnittId).isNotEqualTo(lagredeAvsnitt[0].avsnittId)
        }
    }
}