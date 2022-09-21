package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.infrastruktur.exception.ApiFeil
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.tilFagsak
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime.now
import kotlin.test.assertFailsWith

internal class BehandlingServiceIntegrasjonsTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Test
    internal fun `skal oppdatere behandlingsresultat og vedtakstidspunkt`() {
        val fagsak = DomainUtil.fagsakDomain().tilFagsak("1234")
        val behandling = behandling(fagsak = fagsak)
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)

        val persistertBehandling = behandlingService.hentBehandling(behandlingId = behandling.id)
        Assertions.assertThat(persistertBehandling.vedtakDato).isNull()
        Assertions.assertThat(persistertBehandling.resultat).isEqualTo(BehandlingResultat.IKKE_SATT)

        behandlingService.oppdaterBehandlingsresultatOgVedtaksdato(behandling.id, BehandlingResultat.IKKE_MEDHOLD)
        val oppdatertBehandling = behandlingService.hentBehandling(behandlingId = behandling.id)
        Assertions.assertThat(oppdatertBehandling.vedtakDato).isEqualToIgnoringMinutes(now())
        Assertions.assertThat(oppdatertBehandling.resultat).isEqualTo(BehandlingResultat.IKKE_MEDHOLD)
    }

    @Test
    internal fun `skal ikke opprette ny klagebehandling dersom en behandling under arbeid allerede eksisterer på samme  fagsak`() {
        val fagsak = DomainUtil.fagsakDomain().tilFagsak("1234")
        val behandling = behandling(fagsak = fagsak)
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)
        Assertions.assertThat(behandlingService.hentBehandling(behandling.id).status).isEqualTo(BehandlingStatus.OPPRETTET)

        val feil = assertFailsWith<ApiFeil> {
            behandlingService.opprettBehandling(opprettKlagebehandlingRequest(fagsak, behandling))
        }
        Assertions.assertThat(feil.message)
            .contains("Det eksisterer allerede en klagebehandling som ikke er ferdigstilt på fagsak med id=")
    }

    @Test
    internal fun `skal kunne opprette ny klagebehandling dersom en behandling på samme fagsak venter på resultat fra kabal`() {
        val fagsak = DomainUtil.fagsakDomain().tilFagsak("1234")
        val behandling = behandling(fagsak = fagsak, status = BehandlingStatus.VENTER, steg = StegType.OVERFØRING_TIL_KABAL)
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)
        val førstegangsbehandling = behandlingService.hentBehandling(behandling.id)
        Assertions.assertThat(førstegangsbehandling.status).isEqualTo(BehandlingStatus.VENTER)

        val nyBehandlingId = behandlingService.opprettBehandling(opprettKlagebehandlingRequest(fagsak, behandling))

        val andregangsbehandling = behandlingService.hentBehandling(nyBehandlingId)
        Assertions.assertThat(andregangsbehandling.status).isEqualTo(BehandlingStatus.OPPRETTET)
        Assertions.assertThat(andregangsbehandling.eksternBehandlingId).isNotEqualTo(førstegangsbehandling.eksternBehandlingId)
    }

    private fun opprettKlagebehandlingRequest(fagsak: Fagsak, behandling: Behandling) =
        OpprettKlagebehandlingRequest(
            ident = "1234",
            stønadstype = fagsak.stønadstype,
            eksternBehandlingId = behandling.eksternFagsystemBehandlingId,
            eksternFagsakId = fagsak.eksternId,
            fagsystem = fagsak.fagsystem,
            klageMottatt = LocalDate.now().minusDays(1),
            behandlendeEnhet = "4489"
        )
}
