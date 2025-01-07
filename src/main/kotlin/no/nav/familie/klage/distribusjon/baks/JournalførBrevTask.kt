package no.nav.familie.klage.distribusjon.baks

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.baks.BrevService
import no.nav.familie.klage.distribusjon.DistribusjonService
import no.nav.familie.klage.distribusjon.SendTilKabalTask
import no.nav.familie.klage.distribusjon.ef.JournalførBrevTask
import no.nav.familie.klage.felles.util.TaskMetadata.saksbehandlerMetadataKey
import no.nav.familie.klage.personopplysninger.pdl.logger
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = JournalførBrevTask.TYPE,
    beskrivelse = "Journalfør brev etter klagebehandling",
)
class JournalførBrevTask(
    private val distribusjonService: DistribusjonService,
    private val taskService: TaskService,
    private val behandlingService: BehandlingService,
    private val brevService: BrevService,
    private val journalpostBrevmottakereUtleder: JournalpostBrevmottakereUtleder,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val brev = brevService.hentBrev(behandlingId)
        val journalpostBrevmottakere = journalpostBrevmottakereUtleder.utled(behandlingId)

        if (journalpostBrevmottakere.isEmpty()) {
            throw IllegalStateException("Må ha minimum en mottaker for task ${task.id}")
        }

        journalpostBrevmottakere.forEachIndexed { index, journalpostBrevmottaker ->
            val journalpostId = distribusjonService.journalførBrev(
                behandlingId,
                brev.brevPdf(),
                task.metadata[saksbehandlerMetadataKey].toString(),
                index,
                journalpostBrevmottaker.mapTilAvsenderMottaker(),
            )
            val distribuerBrevTask = DistribuerBrevTask.opprett(
                DistribuerBrevTask.Payload(
                    behandlingId,
                    journalpostId,
                    journalpostBrevmottaker,
                ),
                task.metadata,
            )
            taskService.save(distribuerBrevTask)
        }
    }

    override fun onCompletion(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingService.hentBehandling(behandlingId)
        if (behandling.resultat == BehandlingResultat.IKKE_MEDHOLD) {
            val sendTilKabalTask = SendTilKabalTask.opprett(task.payload, task.metadata)
            taskService.save(sendTilKabalTask)
        } else {
            logger.info("Skal ikke sende til kabal siden formkrav ikke er oppfylt eller saksbehandler har gitt medhold")
        }
    }

    companion object {
        const val TYPE = "baksJournalførBrevTask"
    }
}
