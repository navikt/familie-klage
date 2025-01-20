package no.nav.familie.klage.distribusjon.baks

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.baks.BaksBrevService
import no.nav.familie.klage.brev.baks.brevmottaker.BrevmottakerService
import no.nav.familie.klage.distribusjon.DistribusjonService
import no.nav.familie.klage.distribusjon.SendTilKabalTask
import no.nav.familie.klage.felles.util.TaskMetadata.saksbehandlerMetadataKey
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
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
    taskStepType = JournalførBaksBrevTask.TYPE,
    beskrivelse = "Journalfør baks brev etter klagebehandling",
)
class JournalførBaksBrevTask(
    private val distribusjonService: DistribusjonService,
    private val taskService: TaskService,
    private val behandlingService: BehandlingService,
    private val baksBrevService: BaksBrevService,
    private val personopplysningerService: PersonopplysningerService,
    private val brevmottakerService: BrevmottakerService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val personopplysninger = personopplysningerService.hentPersonopplysninger(behandlingId)
        val brevmottakere = brevmottakerService.hentBrevmottakere(behandlingId)
        val brev = baksBrevService.hentBrev(behandlingId)

        val journalførbareBrevmottakere = utledJournalførbareBrevmottakere(
            personopplysninger.navn,
            brevmottakere,
        )

        if (journalførbareBrevmottakere.isEmpty()) {
            throw IllegalStateException("Må ha minimum en journalførbar brevmottaker i task ${task.id}")
        }

        journalførbareBrevmottakere.forEachIndexed { index, journalførbarBrevmottaker ->
            val journalpostId = distribusjonService.journalførBrev(
                behandlingId,
                brev.pdfSomBytes(),
                task.metadata[saksbehandlerMetadataKey].toString(),
                index,
                journalførbarBrevmottaker.mapTilAvsenderMottaker(),
            )
            val distribuerBaksBrevTask = DistribuerBaksBrevTask.opprett(
                DistribuerBaksBrevTask.Payload(
                    behandlingId,
                    journalpostId,
                    journalførbarBrevmottaker.adresse?.mapTilManuellAdresse(),
                ),
                task.metadata,
            )
            taskService.save(distribuerBaksBrevTask)
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
        const val TYPE = "journalførBaksBrevTask"
    }
}
