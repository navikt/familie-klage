package no.nav.familie.klage.metrics.domain

import no.nav.familie.klage.fagsak.domain.PersonIdent
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.temporal.IsoFields

internal class MålerRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    lateinit var målerRepository: MålerRepository

    val år = LocalDate.now().get(IsoFields.WEEK_BASED_YEAR)
    val uke = LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    @BeforeEach
    internal fun setUp() {
        val fagsak = fagsak()
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagreBehandling(
            behandling(
                fagsak,
                status = BehandlingStatus.FERDIGSTILT,
                resultat = BehandlingResultat.MEDHOLD
            )
        )
        testoppsettService.lagreBehandling(
            behandling(
                fagsak,
                status = BehandlingStatus.FERDIGSTILT,
                resultat = BehandlingResultat.IKKE_MEDHOLD
            )
        )
        testoppsettService.lagreBehandling(
            behandling(
                fagsak,
                status = BehandlingStatus.OPPRETTET,
                resultat = BehandlingResultat.IKKE_SATT
            )
        )

        val fagsakBarnetilsyn = fagsak(identer = setOf(PersonIdent("123")), stønadstype = Stønadstype.BARNETILSYN)
        testoppsettService.lagreFagsak(fagsakBarnetilsyn)
        testoppsettService.lagreBehandling(
            behandling(
                fagsakBarnetilsyn,
                status = BehandlingStatus.FERDIGSTILT,
                resultat = BehandlingResultat.MEDHOLD
            )
        )
    }

    @Test
    internal fun `finnBehandlingerPerStatus`() {
        val data = målerRepository.finnBehandlingerPerStatus()

        assertThat(data).containsExactlyInAnyOrder(
            BehandlingerPerStatus(Stønadstype.OVERGANGSSTØNAD, BehandlingStatus.FERDIGSTILT, 2),
            BehandlingerPerStatus(Stønadstype.OVERGANGSSTØNAD, BehandlingStatus.OPPRETTET, 1),
            BehandlingerPerStatus(Stønadstype.BARNETILSYN, BehandlingStatus.FERDIGSTILT, 1)
        )
    }

    @Test
    internal fun `finnÅpneBehandlingerPerUke`() {
        val data = målerRepository.finnÅpneBehandlingerPerUke()

        assertThat(data).containsExactlyInAnyOrder(
            ÅpneBehandlingerFraUke(år, uke, Stønadstype.OVERGANGSSTØNAD, 1)
        )
    }

    @Test
    internal fun `finnVedtakPerUke`() {
        val data = målerRepository.antallVedtak()

        assertThat(data).containsExactlyInAnyOrder(
            AntallVedtak(Stønadstype.OVERGANGSSTØNAD, BehandlingResultat.MEDHOLD, 1),
            AntallVedtak(Stønadstype.OVERGANGSSTØNAD, BehandlingResultat.IKKE_MEDHOLD, 1),
            AntallVedtak(Stønadstype.BARNETILSYN, BehandlingResultat.MEDHOLD, 1)
        )
    }
}
