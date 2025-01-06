package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.baks.BaksBrevService
import no.nav.familie.klage.brev.baks.mottaker.BrevmottakerService
import no.nav.familie.klage.felles.util.TaskMetadata.saksbehandlerMetadataKey
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.personopplysninger.pdl.logger
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = JournalførBrevTask.TYPE,
    beskrivelse = "Journalfør brev etter klagebehandling",
)
class BaksJournalførBrevTask(
    private val distribusjonService: DistribusjonService,
    private val taskService: TaskService,
    private val behandlingService: BehandlingService,
    private val baksBrevService: BaksBrevService,
    private val brevmottakerService: BrevmottakerService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)

        val avsenderMottakere = brevmottakerService.hentBrevmottakere(behandlingId).map {
            AvsenderMottaker(
                id = it.id.toString(),
                navn = it.navn,
                idType = AvsenderMottakerIdType.FNR,
            )
        }

        feilHvis(avsenderMottakere.isEmpty()) {
            "Må hax minimum en mottaker"
        }

        avsenderMottakere.forEachIndexed { index, avsenderMottaker ->
            val journalpostId = distribusjonService.journalførBrev(
                behandlingId,
                baksBrevService.hentBrev(behandlingId).brevPdf(),
                task.metadata[saksbehandlerMetadataKey].toString(),
                index,
                avsenderMottaker,
            )
            opprettDistribuerBrevTask(BaksDistribuerBrevDto(behandlingId, journalpostId), task.metadata)
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
    }

    private fun opprettDistribuerBrevTask(payload: BaksDistribuerBrevDto, metadata: Properties) {
        val sendTilKabalTask = Task(
            type = BaksDistribuerBrevTask.TYPE,
            payload = objectMapper.writeValueAsString(payload),
            properties = metadata,
        )
        taskService.save(sendTilKabalTask)
    }

    private fun opprettSendTilKabalTask(task: Task) {
        val sendTilKabalTask = Task(
            type = SendTilKabalTask.TYPE,
            payload = task.payload,
            properties = task.metadata,
        )
        taskService.save(sendTilKabalTask)
    }

    companion object {
        const val TYPE = "baksJournalførBrevTask"
    }
}
