package no.nav.familie.klage.brev

import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype.UTEN_VEDTAK
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype.VEDTAK
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.brev.ef.BrevService
import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.formkrav.FormRepository
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.testutil.BrukerContextUtil
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.klage.testutil.DomainUtil.påklagetVedtakDetaljer
import no.nav.familie.klage.vurdering.VurderingRepository
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BaksBrevServiceTest : OppslagSpringRunnerTest() {

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
    private val påklagetVedtak = PåklagetVedtak(VEDTAK, påklagetVedtakDetaljer("123"))
    private val behandlingPåklagetVedtak = behandling(fagsak, steg = StegType.BREV, påklagetVedtak = påklagetVedtak)
    private val ferdigstiltBehandling = behandling(
        fagsakFerdigstiltBehandling,
        status = BehandlingStatus.FERDIGSTILT,
        påklagetVedtak = påklagetVedtak,
    )
    private val fagsakBehandlingUtenPåklagetVedtak = fagsak(identer = setOf(PersonIdent("11010199999")))
    private val behandlingUtenPåklagetVedtak =
        behandling(
            fagsakBehandlingUtenPåklagetVedtak,
            steg = StegType.BREV,
            påklagetVedtak = påklagetVedtak.copy(påklagetVedtakstype = UTEN_VEDTAK),
        )

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandlingPåklagetVedtak)

        testoppsettService.lagreFagsak(fagsakFerdigstiltBehandling)
        testoppsettService.lagreBehandling(ferdigstiltBehandling)

        testoppsettService.lagreFagsak(fagsakBehandlingUtenPåklagetVedtak)
        testoppsettService.lagreBehandling(behandlingUtenPåklagetVedtak)

        formRepository.insert(DomainUtil.oppfyltForm(behandlingPåklagetVedtak.id))
        vurderingRepository.insert(DomainUtil.vurdering(behandlingPåklagetVedtak.id))

        formRepository.insert(DomainUtil.oppfyltForm(behandlingUtenPåklagetVedtak.id))
        vurderingRepository.insert(DomainUtil.vurdering(behandlingUtenPåklagetVedtak.id))
        BrukerContextUtil.mockBrukerContext()
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Nested
    inner class LagEllerOppdaterBrev {

        @Test
        internal fun `skal ikke kunne lage eller oppdatere når behandlingen er låst`() {
            assertThatThrownBy { brevService.lagBrev(ferdigstiltBehandling.id) }
                .hasMessage("Kan ikke oppdatere brev når behandlingen er låst")
        }

        @Test
        internal fun `skal ikke kunne lage eller oppdatere når behandlingen ikke er i brevsteget`() {
            behandlingRepository.update(behandlingPåklagetVedtak.copy(steg = StegType.FORMKRAV))
            assertThatThrownBy { brevService.lagBrev(behandlingPåklagetVedtak.id) }
                .hasMessageContaining("Behandlingen er i feil steg ")
        }

        @Test
        internal fun `skal kunne lage avvisningsbrev når behandlingen har påklaget vedtakstype uten vedtak`() {
            assertThat(brevService.lagBrev(behandlingUtenPåklagetVedtak.id)).isNotNull
        }
    }

    @Nested
    inner class LagBrevSomPdf {

        @Test
        internal fun `kan ikke lage pdf 2 ganger`() {
            brevService.lagBrev(behandlingPåklagetVedtak.id)
            brevService.lagBrevPdf(behandlingPåklagetVedtak.id)

            assertThatThrownBy { brevService.lagBrevPdf(behandlingPåklagetVedtak.id) }
                .hasMessage("Det finnes allerede en lagret pdf")
        }
    }
}
