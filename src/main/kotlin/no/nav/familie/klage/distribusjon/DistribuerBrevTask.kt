package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.domain.BrevmottakereJournalposter
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.brevmottaker.domain.Brevmottakere
import no.nav.familie.klage.distribusjon.domain.BrevmottakerJournalpost
import no.nav.familie.klage.distribusjon.domain.BrevmottakerJournalpostMedIdent
import no.nav.familie.klage.distribusjon.domain.BrevmottakerJournalpostUtenIdent
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.klage.infrastruktur.featuretoggle.Toggle.SKAL_BRUKE_NY_LØYPE_FOR_JOURNALFØRING
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.dokdist.AdresseType.norskPostadresse
import no.nav.familie.kontrakter.felles.dokdist.AdresseType.utenlandskPostadresse
import no.nav.familie.kontrakter.felles.dokdist.ManuellAdresse
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
    private val fagsakService: FagsakService,
    private val featureToggleService: FeatureToggleService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val brev = brevService.hentBrev(behandlingId)
        val journalposter = mottakereJournalpost(brev)

        validerHarJournalposter(behandlingId, journalposter)

        val distribueringer =
            if (featureToggleService.isEnabled(SKAL_BRUKE_NY_LØYPE_FOR_JOURNALFØRING)) {
                val fagsystem = fagsakService.hentFagsakForBehandling(behandlingId).fagsystem.tilFellesFagsystem()
                journalposter.fold(journalposter) { acc, journalpost ->
                    distribuerOgLagreJournalposter(behandlingId, acc, journalpost, brev.mottakere, fagsystem)
                }
            } else {
                journalposter.fold(journalposter) { acc, journalpost ->
                    distribuerOgLagreJournalposter(behandlingId, acc, journalpost)
                }
            }

        feilHvis(distribueringer.any { it.distribusjonId == null }) {
            "Mangler distribusjonId for journalpost"
        }
    }

    private fun distribuerOgLagreJournalposter(
        behandlingId: UUID,
        acc: List<BrevmottakerJournalpost>,
        journalpost: BrevmottakerJournalpost,
    ): List<BrevmottakerJournalpost> =
        if (journalpost.distribusjonId == null) {
            val distribusjonId = distribusjonService.distribuerBrev(journalpost.journalpostId)
            val nyeJournalposter =
                acc.map {
                    if (it.journalpostId == journalpost.journalpostId) {
                        it.medDistribusjonsId(distribusjonId = distribusjonId)
                    } else {
                        it
                    }
                }
            brevService.oppdaterMottakerJournalpost(behandlingId, BrevmottakereJournalposter(nyeJournalposter))
            nyeJournalposter
        } else {
            acc
        }

    private fun distribuerOgLagreJournalposter(
        behandlingId: UUID,
        acc: List<BrevmottakerJournalpost>,
        journalpost: BrevmottakerJournalpost,
        brevmottakere: Brevmottakere?,
        fagsystem: Fagsystem,
    ): List<BrevmottakerJournalpost> =
        if (journalpost.distribusjonId == null) {
            val adresse = finnAdresseForJournalpost(journalpost, brevmottakere)
            val distribusjonId = distribusjonService.distribuerBrev(
                journalpostId = journalpost.journalpostId,
                adresse = adresse,
                fagsystem = fagsystem,
            )
            val nyeJournalposter = acc.map {
                if (it.journalpostId == journalpost.journalpostId) {
                    it.medDistribusjonsId(distribusjonId = distribusjonId)
                } else {
                    it
                }
            }
            brevService.oppdaterMottakerJournalpost(behandlingId, BrevmottakereJournalposter(nyeJournalposter))
            nyeJournalposter
        } else {
            acc
        }

    private fun validerHarJournalposter(
        behandlingId: UUID,
        journalposter: List<BrevmottakerJournalpost>,
    ) {
        feilHvis(journalposter.isEmpty()) {
            "Mangler journalposter for behandling=$behandlingId"
        }
    }

    private fun mottakereJournalpost(brev: Brev): List<BrevmottakerJournalpost> =
        brev.mottakereJournalposter?.journalposter?.takeIf { it.isNotEmpty() }
            ?: error("Mangler journalposter koblet til brev=${brev.behandlingId}")

    private fun finnAdresseForJournalpost(
        journalpost: BrevmottakerJournalpost,
        brevmottakere: Brevmottakere?,
    ): ManuellAdresse? {
        return when (journalpost) {
            is BrevmottakerJournalpostMedIdent -> null
            is BrevmottakerJournalpostUtenIdent -> {
                val brevmottaker = brevmottakere?.personer
                    ?.filterIsInstance<BrevmottakerPersonUtenIdent>()?.find { it.id == journalpost.id }
                    ?: throw Feil("Mangler brevmottaker for journalpost=${journalpost.journalpostId}")

                with(brevmottaker) {
                    ManuellAdresse(
                        adresseType = if (landkode == "NO") norskPostadresse else utenlandskPostadresse,
                        adresselinje1 = adresselinje1,
                        adresselinje2 = adresselinje2,
                        postnummer = postnummer,
                        poststed = poststed,
                        land = landkode,
                    )
                }
            }
        }
    }

    companion object {
        const val TYPE = "distribuerBrevTask"
    }
}
