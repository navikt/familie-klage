package no.nav.familie.klage.kabal

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandlingFeilregistrertTask.TYPE,
    beskrivelse = "Håndter feilregistret klage fra kabal",
    maxAntallFeil = 1,
)
class BehandlingFeilregistrertTask : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        throw NotImplementedError("Håndtering av feilregistret behandling fra kabal er ikke implementert enda")
    }

    companion object {

        const val TYPE = "BehandlingFeilregistrert"

        fun opprettTask(behandlingId: UUID): Task =
            Task(TYPE, behandlingId.toString())
    }
}
