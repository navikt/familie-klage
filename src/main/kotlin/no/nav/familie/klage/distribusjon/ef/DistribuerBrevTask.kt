package no.nav.familie.klage.distribusjon.ef

import no.nav.familie.klage.brev.ef.BrevService
import no.nav.familie.klage.brev.ef.domain.Brev
import no.nav.familie.klage.brev.ef.domain.BrevmottakereJournalpost
import no.nav.familie.klage.brev.ef.domain.BrevmottakereJournalposter
import no.nav.familie.klage.distribusjon.DistribusjonService
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = DistribuerBrevTask.TYPE,
    beskrivelse = "Distribuer brev etter klagebehandling",
)
class DistribuerBrevTask(
    private val brevService: BrevService,
    private val distribusjonService: DistribusjonService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val brev = brevService.hentBrev(behandlingId)
        val journalposter = mottakereJournalpost(brev)

        validerHarJournalposter(behandlingId, journalposter)

        val distribueringer = journalposter.fold(journalposter) { acc, journalpost ->
            distribuerOgLagreJournalposter(behandlingId, acc, journalpost)
        }

        feilHvis(distribueringer.any { it.distribusjonId == null }) {
            "Mangler distribusjonId for journalpost"
        }
    }

    private fun distribuerOgLagreJournalposter(
        behandlingId: UUID,
        acc: List<BrevmottakereJournalpost>,
        journalpost: BrevmottakereJournalpost,
    ): List<BrevmottakereJournalpost> {
        return if (journalpost.distribusjonId == null) {
            val distribusjonId = distribusjonService.distribuerBrev(journalpost.journalpostId, Fagsystem.EF)
            val nyeJournalposter = acc.map {
                if (it.journalpostId == journalpost.journalpostId) {
                    it.copy(distribusjonId = distribusjonId)
                } else {
                    it
                }
            }
            brevService.oppdaterMottakerJournalpost(behandlingId, BrevmottakereJournalposter(nyeJournalposter))
            nyeJournalposter
        } else {
            acc
        }
    }

    private fun validerHarJournalposter(
        behandlingId: UUID,
        journalposter: List<BrevmottakereJournalpost>,
    ) {
        feilHvis(journalposter.isEmpty()) {
            "Mangler journalposter for behandling=$behandlingId"
        }
    }

    private fun mottakereJournalpost(brev: Brev): List<BrevmottakereJournalpost> {
        return brev.mottakereJournalposter?.journalposter?.takeIf { it.isNotEmpty() }
            ?: error("Mangler journalposter koblet til brev=${brev.behandlingId}")
    }

    companion object {

        const val TYPE = "distribuerBrevTask"
    }
}
