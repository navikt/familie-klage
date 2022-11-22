package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import no.nav.familie.klage.repository.findByIdOrThrow
import no.nav.familie.klage.testutil.DomainUtil.behandling
import no.nav.familie.klage.testutil.DomainUtil.fagsak
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.Month
import java.util.UUID

internal class PatchPåklagetBehandlingDetaljerControllerTest: OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var controller: PatchPåklagetBehandlingDetaljerController

    private val fagsak = fagsak()
    private val behandling = behandling(fagsak)
    private val behandlingMedPåklagetVedtak = behandling(fagsak, påklagetVedtak = PåklagetVedtak("123", PåklagetVedtakstype.VEDTAK))
    private val behandlingMedPåklagetVedtakSomIkkeFinnes =
        behandling(fagsak, påklagetVedtak = PåklagetVedtak(UUID.randomUUID().toString(), PåklagetVedtakstype.VEDTAK))

    @BeforeEach
    internal fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
    }

    @Test
    internal fun `skal oppdatere påklagetVedtakDetaljer men ikke sporbar`() {
        val behandling = testoppsettService.lagreBehandling(behandlingMedPåklagetVedtak)

        Thread.sleep(2000) // Sånn att sporbar-sjekken blir riktig
        controller.ferdigstillBehandling(false)

        val oppdatertBehandling = behandlingRepository.findByIdOrThrow(behandlingMedPåklagetVedtak.id)
        assertThat(oppdatertBehandling.påklagetVedtak.påklagetVedtakDetaljer).isEqualTo(
            PåklagetVedtakDetaljer(
                FagsystemType.ORDNIÆR,
                "123",
                "Førstegangsbehandling",
                "Innvilget",
                LocalDateTime.of(2022, Month.AUGUST, 1, 8, 0)
            )
        )
        assertThat(oppdatertBehandling.sporbar).isEqualTo(behandling.sporbar)
    }

    @Test
    internal fun `skal ikke oppdatere hvis det ikke finnes noe å oppdatere`() {
        testoppsettService.lagreBehandling(behandling)
        controller.ferdigstillBehandling(false)

        val oppdatertBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        assertThat(oppdatertBehandling.påklagetVedtak.påklagetVedtakDetaljer).isNull()
    }

    @Test
    internal fun `skal feile hvis man ikke finner vedtaket`() {
        testoppsettService.lagreBehandling(behandlingMedPåklagetVedtakSomIkkeFinnes)
        assertThatThrownBy {
            controller.ferdigstillBehandling(false)
        }.hasMessageContaining("Finner ikke vedtak til behandling")
    }
}