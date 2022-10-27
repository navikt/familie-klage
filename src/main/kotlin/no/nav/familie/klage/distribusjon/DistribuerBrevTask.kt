package no.nav.familie.klage.distribusjon

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = DistribuerBrevTask.TYPE,
    beskrivelse = "Distribuer brev etter klagebehandling"
)
class DistribuerBrevTask(private val distribusjonService: DistribusjonService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val journalpostId = task.payload
        val distribusjonId = distribusjonService.distribuerBrev(journalpostId)
        task.metadata.apply {
            this["distribusjonId"] = distribusjonId
        }
    }

    companion object {

        const val TYPE = "distribuerBrevTask"
    }
}
