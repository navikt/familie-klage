package no.nav.familie.klage.behandling

import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.testutil.DomainUtil
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.tilFagsak
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime.now

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
}
