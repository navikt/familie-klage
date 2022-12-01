package no.nav.familie.klage.behandlingshistorikk

import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BehandlingshistorikkRepositoryTest: OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingshistorikkRepository: BehandlingshistorikkRepository

    @Test
    internal fun `henter og lagrer behandlingshistorikk`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagreBehandling(behandling(fagsak))

        val behandlingshistorikk = Behandlingshistorikk(
            behandlingId = behandling.id,
            steg = StegType.VURDERING,
            resultat = "Resultat"
        )
        behandlingshistorikkRepository.insert(behandlingshistorikk)

        val hentetBehandlingshistorikk = behandlingshistorikkRepository.findByIdOrThrow(behandlingshistorikk.id)
        assertThat(hentetBehandlingshistorikk.id).isEqualTo(behandlingshistorikk.id)
        assertThat(hentetBehandlingshistorikk.behandlingId).isEqualTo(behandlingshistorikk.behandlingId)
        assertThat(hentetBehandlingshistorikk.steg).isEqualTo(behandlingshistorikk.steg)
        assertThat(hentetBehandlingshistorikk.resultat).isEqualTo(behandlingshistorikk.resultat)
    }
}