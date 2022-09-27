package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.tilFagsak
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import kotlin.test.assertFailsWith

internal class OpprettBehandlingServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var opprettBehandlingService: OpprettBehandlingService

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Test
    internal fun `skal ikke opprette ny klagebehandling dersom en behandling under arbeid allerede eksisterer på samme  fagsak`() {
        val fagsak = DomainUtil.fagsakDomain().tilFagsak("1234")
        val behandling = behandling(fagsak = fagsak)
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)
        assertThat(behandlingService.hentBehandling(behandling.id).status).isEqualTo(BehandlingStatus.OPPRETTET)

        val feil = assertFailsWith<ApiFeil> {
            opprettBehandlingService.opprettBehandling(opprettKlagebehandlingRequest(fagsak, behandling))
        }
        assertThat(feil.message)
                .contains("Det eksisterer allerede en klagebehandling som ikke er ferdigstilt på fagsak med id=")
    }

    @Test
    internal fun `skal kunne opprette ny klagebehandling dersom en behandling på samme fagsak venter på resultat fra kabal`() {
        val fagsak = DomainUtil.fagsakDomain().tilFagsak("1234")
        val behandling = behandling(fagsak = fagsak, status = BehandlingStatus.VENTER, steg = StegType.OVERFØRING_TIL_KABAL)
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)
        val førstegangsbehandling = behandlingService.hentBehandling(behandling.id)
        assertThat(førstegangsbehandling.status).isEqualTo(BehandlingStatus.VENTER)

        val nyBehandlingId = opprettBehandlingService.opprettBehandling(opprettKlagebehandlingRequest(fagsak, behandling))

        val andregangsbehandling = behandlingService.hentBehandling(nyBehandlingId)
        assertThat(andregangsbehandling.status).isEqualTo(BehandlingStatus.OPPRETTET)
        assertThat(andregangsbehandling.eksternBehandlingId).isNotEqualTo(førstegangsbehandling.eksternBehandlingId)
    }

    @Test
    internal fun `skal ikke kunne opprette klage med krav mottatt frem i tid`() {
        val fagsak = DomainUtil.fagsakDomain().tilFagsak("1234")
        val behandling = behandling(fagsak = fagsak, status = BehandlingStatus.VENTER, steg = StegType.OVERFØRING_TIL_KABAL)
        val request = opprettKlagebehandlingRequest(fagsak, behandling, LocalDate.now().plusDays(1))

        val feil = assertThrows<Feil> { opprettBehandlingService.opprettBehandling(request) }

        assertThat(feil.frontendFeilmelding).contains("Kan ikke opprette klage med krav mottatt frem i tid for behandling med eksternBehandlingId=")
    }

    private fun opprettKlagebehandlingRequest(fagsak: Fagsak,
                                              behandling: Behandling,
                                              klageMottatt: LocalDate = LocalDate.now().minusDays(1)) =
            OpprettKlagebehandlingRequest(
                    ident = "1234",
                    stønadstype = fagsak.stønadstype,
                    eksternBehandlingId = behandling.eksternFagsystemBehandlingId,
                    eksternFagsakId = fagsak.eksternId,
                    fagsystem = fagsak.fagsystem,
                    klageMottatt = klageMottatt,
                    behandlendeEnhet = "4489"
            )
}
