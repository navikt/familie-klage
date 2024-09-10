package no.nav.familie.klage.oppgave

import no.nav.familie.klage.infrastruktur.config.OppslagSpringRunnerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

internal class BehandleSakOppgaveRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandleSakOppgaveRepository: BehandleSakOppgaveRepository

    @Test
    internal fun `skal ikke kaste feil hvis oppgave ikke finnes`() {
        val oppgave = behandleSakOppgaveRepository.findByBehandlingId(UUID.randomUUID())
        assertThat(oppgave).isNull()
    }
}
