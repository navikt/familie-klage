package no.nav.familie.klage.kabal

import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class BehandlingFeilregistrertTaskTest {

    val behandlingFeilregistrertTask = BehandlingFeilregistrertTask()

    @Test
    internal fun `Behanlding feilregistrert task skal feile fordi den ikke er implementert enda`() {
        val task = BehandlingFeilregistrertTask.opprettTask(UUID.randomUUID())

        val feil = assertThrows<NotImplementedError> { behandlingFeilregistrertTask.doTask(task) }

        assertThat(feil.message).contains("Håndtering av feilregistret behandling fra kabal er ikke implementert enda")
    }
}
