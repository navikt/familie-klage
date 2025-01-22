package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brevmottaker.BrevmottakerUtil.validerMinimumEnMottaker
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.domain.BrevmottakereJournalpost
import no.nav.familie.klage.brev.domain.BrevmottakereJournalpostMedIdent
import no.nav.familie.klage.brev.domain.BrevmottakereJournalpostUtenIdent
import no.nav.familie.klage.brev.domain.BrevmottakereJournalposter
import no.nav.familie.klage.brevmottaker.domain.Brevmottaker
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.distribusjon.JournalføringUtil.mapAvsenderMottaker
import no.nav.familie.klage.felles.util.TaskMetadata.saksbehandlerMetadataKey
import no.nav.familie.klage.personopplysninger.pdl.logger
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
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
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val saksbehandler = task.metadata[saksbehandlerMetadataKey].toString()
        behandlingService.hentBehandling(behandlingId).årsak

        val brev = brevService.hentBrev(behandlingId)

        val mottakere = brev.mottakere ?: error("Mangler mottakere på brev for behandling=$behandlingId")
        validerMinimumEnMottaker(mottakere)
        journalførBrevmottakere(brev, mottakere, saksbehandler)
    }

    private fun journalførBrevmottakere(
        brev: Brev,
        mottakere: Brevmottakere,
        saksbehandler: String,
    ) {
        val behandlingId = brev.behandlingId
        val journalposter = brev.mottakereJournalposter?.journalposter ?: emptyList()
        val brevPdf = brev.brevPdf()
        mottakere.personer.foldIndexed(journalposter) { index, eksisterendeBrevmottakereJournalpost, brevmottakerPerson ->
            brevmottakereJournalposts(
                brevmottakerPerson,
                eksisterendeBrevmottakereJournalpost,
                behandlingId,
                brevPdf,
                saksbehandler,
                index,
            )
        }
        // TODO : Denne kan overskrive den over, burde ikke være sånn (?)
        mottakere.organisasjoner.foldIndexed(journalposter) { index, eksisterendeBrevmottakereJournalpost, brevmottakerOrganisasjon ->
            brevmottakereJournalposts(
                brevmottakerOrganisasjon,
                eksisterendeBrevmottakereJournalpost,
                behandlingId,
                brevPdf,
                saksbehandler,
                index,
            )
        }
    }

    private fun brevmottakereJournalposts(
        brevmottaker: Brevmottaker,
        eksisterendeBrevmottakereJournalpost: List<BrevmottakereJournalpost>,
        behandlingId: UUID,
        brevPdf: ByteArray,
        saksbehandler: String,
        index: Int,
    ): List<BrevmottakereJournalpost> {
        val avsenderMottaker = mapAvsenderMottaker(brevmottaker)
        return if (eksisterendeBrevmottakereJournalpost.none { harLikId(it, avsenderMottaker) }) {
            val journalpostId = distribusjonService.journalførBrev(
                behandlingId,
                brevPdf,
                saksbehandler,
                index,
                avsenderMottaker,
            )
            val nyBrevmottakereJournalpost = when (brevmottaker) {
                is BrevmottakerPersonMedIdent,
                is BrevmottakerOrganisasjon,
                -> BrevmottakereJournalpostMedIdent(
                    ident = avsenderMottaker.id ?: error("Mangler id for mottaker=$avsenderMottaker"),
                    journalpostId = journalpostId,
                )

                is BrevmottakerPersonUtenIdent,
                -> BrevmottakereJournalpostUtenIdent(
                    idForBrevmottakereUtenIdent = brevmottaker.id,
                    journalpostId = journalpostId,
                )
            }
            val nyeMottakere = eksisterendeBrevmottakereJournalpost + nyBrevmottakereJournalpost
            brevService.oppdaterMottakerJournalpost(behandlingId, BrevmottakereJournalposter(nyeMottakere))
            nyeMottakere
        } else {
            eksisterendeBrevmottakereJournalpost
        }
    }

    private fun harLikId(
        it: BrevmottakereJournalpost,
        avsenderMottaker: AvsenderMottaker,
    ) = when (it) {
        is BrevmottakereJournalpostMedIdent -> it.ident === avsenderMottaker.id
        is BrevmottakereJournalpostUtenIdent -> it.idForBrevmottakereUtenIdent.toString() === avsenderMottaker.id
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
            properties = task.metadata,
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

        const val TYPE = "journalførBrevTask"
    }
}
