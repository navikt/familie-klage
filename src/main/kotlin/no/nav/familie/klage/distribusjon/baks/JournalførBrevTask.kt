package no.nav.familie.klage.distribusjon.baks

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.baks.BaksBrevService
import no.nav.familie.klage.brev.baks.mottaker.BrevmottakerService
import no.nav.familie.klage.brev.baks.mottaker.Mottakertype
import no.nav.familie.klage.distribusjon.DistribusjonService
import no.nav.familie.klage.distribusjon.SendTilKabalTask
import no.nav.familie.klage.distribusjon.ef.JournalførBrevTask
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
    taskStepType = JournalførBrevTask.TYPE,
    beskrivelse = "Journalfør brev etter klagebehandling",
)
class JournalførBrevTask(
    private val distribusjonService: DistribusjonService,
    private val taskService: TaskService,
    private val behandlingService: BehandlingService,
    private val baksBrevService: BaksBrevService,
    private val brevmottakerService: BrevmottakerService,
    private val personopplysningerService: PersonopplysningerService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val brev = baksBrevService.hentBrev(behandlingId)

        val journalpostBrevmottakere = utledJournalpostBrevmottaker(behandlingId)

        if (journalpostBrevmottakere.isEmpty()) {
            throw IllegalStateException("Må ha minimum en mottaker")
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

    private fun utledJournalpostBrevmottaker(behandlingId: UUID): List<JournalpostBrevmottaker> {
        val personopplysninger = personopplysningerService.hentPersonopplysninger(behandlingId)
        val manuelleBrevmottakere = brevmottakerService.hentBrevmottakere(behandlingId)

        if (manuelleBrevmottakere.isEmpty()) {
            return listOf(JournalpostBrevmottaker.opprett(personopplysninger.navn, Mottakertype.BRUKER))
        }

        if (manuelleBrevmottakere.any { it.mottakertype == Mottakertype.DØDSBO }) {
            val dødsbo = manuelleBrevmottakere.first { it.mottakertype == Mottakertype.DØDSBO }
            return listOf(JournalpostBrevmottaker.opprett(dødsbo))
        }

        val brukerMedUtenlandskAdresse = manuelleBrevmottakere.find {
            it.mottakertype == Mottakertype.BRUKER_MED_UTENLANDSK_ADRESSE
        }

        val bruker = if (brukerMedUtenlandskAdresse != null) {
            JournalpostBrevmottaker.opprett(brukerMedUtenlandskAdresse)
        } else {
            JournalpostBrevmottaker.opprett(personopplysninger.navn, Mottakertype.BRUKER)
        }

        val fullmektigEllerVerge = manuelleBrevmottakere
            .find { it.mottakertype == Mottakertype.FULLMEKTIG || it.mottakertype == Mottakertype.VERGE }
            ?.let { JournalpostBrevmottaker.opprett(it) }

        return listOfNotNull(bruker, fullmektigEllerVerge)
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
