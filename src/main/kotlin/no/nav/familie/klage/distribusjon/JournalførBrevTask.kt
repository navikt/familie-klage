package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.felles.util.TaskMetadata.saksbehandlerMetadataKey
import no.nav.familie.klage.personopplysninger.pdl.logger
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = JournalførBrevTask.TYPE,
    beskrivelse = "Journalfør brev etter klagebehandling"
)
class JournalførBrevTask(
    private val distribusjonService: DistribusjonService,
    private val taskRepository: TaskRepository,
    private val behandlingService: BehandlingService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val saksbehandler = task.metadata.getProperty(saksbehandlerMetadataKey)
        val journalpostId = distribusjonService.journalførBrev(behandlingId, saksbehandler)
        task.metadata.apply {
            this["journalpostId"] = journalpostId
        }
    }

    override fun onCompletion(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingService.hentBehandling(behandlingId)
        if (behandling.resultat == BehandlingResultat.IKKE_MEDHOLD) {
            opprettSendTilKabalTask(task)
        } else {
            logger.info("Skal ikke sende til kabal siden formkrav ikke er oppfylt eller saksbehandler har gitt medhold")
        }

        opprettDistribuerBrevTask(task)
    }

    private fun opprettDistribuerBrevTask(task: Task) {
        val sendTilKabalTask = Task(
            type = DistribuerBrevTask.TYPE,
            payload = task.payload,
            properties = task.metadata
        )
        taskRepository.save(sendTilKabalTask)
    }

    private fun opprettSendTilKabalTask(task: Task) {
        val sendTilKabalTask = Task(
            type = SendTilKabalTask.TYPE,
            payload = task.payload,
            properties = task.metadata
        )
        taskRepository.save(sendTilKabalTask)
    }

    companion object {
        const val TYPE = "journalførBrevTask"
    }
}
