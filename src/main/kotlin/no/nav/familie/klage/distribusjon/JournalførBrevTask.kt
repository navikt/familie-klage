package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.domain.BrevmottakereJournalpost
import no.nav.familie.klage.brev.domain.BrevmottakereJournalposter
import no.nav.familie.klage.distribusjon.JournalføringUtil.mapAvsenderMottaker
import no.nav.familie.klage.felles.util.TaskMetadata.saksbehandlerMetadataKey
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.personopplysninger.pdl.logger
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
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
    private val behandlingService: BehandlingService,
    private val brevService: BrevService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val saksbehandler = task.metadata[saksbehandlerMetadataKey].toString()

        val brev = brevService.hentBrev(behandlingId)

        val mottakere = mapAvsenderMottaker(brev)
        if (mottakere.isEmpty()) {
            journalførSøker(task, behandlingId, brev.brevPdf(), saksbehandler)
        } else {
            journalførBrevmottakere(task, brev, behandlingId, mottakere, saksbehandler)
        }
    }

    private fun Brev.brevPdf() = this.pdf?.bytes ?: error("Mangler brev-pdf for behandling=$behandlingId")

    private fun journalførSøker(task: Task, behandlingId: UUID, brevPdf: ByteArray, saksbehandler: String) {
        val ident = behandlingService.hentAktivIdent(behandlingId).second.hentAktivIdent()
        val journalpostId = distribusjonService.journalførBrev(behandlingId, brevPdf, saksbehandler, 0, null)
        brevService.oppdaterMottakerJournalpost(
            behandlingId,
            BrevmottakereJournalposter(listOf(BrevmottakereJournalpost(ident, journalpostId)))
        )
        opprettDistribuerBrevTask(task, journalpostId)
    }

    private fun journalførBrevmottakere(
        task: Task,
        brev: Brev,
        behandlingId: UUID,
        mottakere: List<AvsenderMottaker>,
        saksbehandler: String
    ) {
        validerUnikeMottakere(mottakere)
        val brevmottakereJournalpost =
            mottakere.foldIndexed(brev.mottakereJournalpost?.journalposter ?: emptyList()) { index, acc, avsenderMottaker ->
                if (acc.none { it.ident == avsenderMottaker.id }) {
                    val journalpostId =
                        distribusjonService.journalførBrev(behandlingId, brev.brevPdf(), saksbehandler, index, avsenderMottaker)
                    val resultat = BrevmottakereJournalpost(
                        ident = avsenderMottaker.id ?: error("Mangler id for mottaker=$avsenderMottaker"),
                        journalpostId = journalpostId
                    )
                    val nyeMottakere = acc + resultat
                    brevService.oppdaterMottakerJournalpost(behandlingId, BrevmottakereJournalposter(nyeMottakere))
                    nyeMottakere
                } else {
                    acc
                }
            }
        brevmottakereJournalpost.forEach { opprettDistribuerBrevTask(task, it.journalpostId) }
    }

    /**
     * Validerer at det kun finnes unike mottakere.
     * Det gjøres validering i innsending som også sjekker at det ikke blir lagret duplikat, så dette burde ikke skje
     */
    private fun validerUnikeMottakere(mottakere: List<AvsenderMottaker>) {
        feilHvis(mottakere.map { it.id }.size != mottakere.size) {
            "Har ikke støtte for duplikat av mottakeridenter $mottakere"
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

    private fun opprettDistribuerBrevTask(task: Task, journalpostId: String) {
        val sendTilKabalTask = Task(
            type = DistribuerBrevTask.TYPE,
            payload = journalpostId,
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
