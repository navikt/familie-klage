package no.nav.familie.klage.behandling

import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.tilFagsak
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime.now

internal class BehandlingServiceIntegrasjonTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Test
    internal fun `skal oppdatere behandlingsresultat og vedtakstidspunkt`() {
        val fagsak = DomainUtil.fagsakDomain().tilFagsak("1234")
        val behandling = behandling(fagsak = fagsak)
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(behandling)

        val persistertBehandling = behandlingService.hentBehandling(behandlingId = behandling.id)
        assertThat(persistertBehandling.vedtakDato).isNull()
        assertThat(persistertBehandling.resultat).isEqualTo(BehandlingResultat.IKKE_SATT)

        behandlingService.oppdaterBehandlingsresultatOgVedtaksdato(behandling.id, BehandlingResultat.IKKE_MEDHOLD)
        val oppdatertBehandling = behandlingService.hentBehandling(behandlingId = behandling.id)
        assertThat(oppdatertBehandling.vedtakDato).isEqualToIgnoringMinutes(now())
        assertThat(oppdatertBehandling.resultat).isEqualTo(BehandlingResultat.IKKE_MEDHOLD)
    }
}
