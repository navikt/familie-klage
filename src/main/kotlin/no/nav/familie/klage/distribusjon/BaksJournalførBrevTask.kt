package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.baks.BaksBrevService
import no.nav.familie.klage.brev.baks.mottaker.BrevmottakerService
import no.nav.familie.klage.felles.util.TaskMetadata.saksbehandlerMetadataKey
import no.nav.familie.klage.personopplysninger.pdl.logger
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
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
class BaksJournalførBrevTask(
    private val distribusjonService: DistribusjonService,
    private val taskService: TaskService,
    private val behandlingService: BehandlingService,
    private val baksBrevService: BaksBrevService,
    private val brevmottakerService: BrevmottakerService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)

        // TODO : Pluss på bruker i tillegg til manuelt registrerte mottakere?
        val avsenderMottakere = brevmottakerService.hentBrevmottakere(behandlingId).map {
            AvsenderMottaker(
                id = it.id.toString(),
                navn = it.navn,
                idType = AvsenderMottakerIdType.FNR,
            )
        }

        if (avsenderMottakere.isEmpty()) {
            throw IllegalStateException("Må ha minimum en mottaker")
        }

        avsenderMottakere.forEachIndexed { index, avsenderMottaker ->
            val journalpostId = distribusjonService.journalførBrev(
                behandlingId,
                baksBrevService.hentBrev(behandlingId).brevPdf(),
                task.metadata[saksbehandlerMetadataKey].toString(),
                index,
                avsenderMottaker,
            )
            val distribuerBrevTask = BaksDistribuerBrevTask.opprett(
                BaksDistribuerBrevTask.Payload(behandlingId, journalpostId),
                task.metadata,
            )
            taskService.save(distribuerBrevTask)
        }
    }

    override fun onCompletion(task: Task) {
        val behandling = behandlingService.hentBehandling(UUID.fromString(task.payload))
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
