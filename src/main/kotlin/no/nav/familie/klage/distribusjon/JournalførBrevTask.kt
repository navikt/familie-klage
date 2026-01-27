package no.nav.familie.klage.distribusjon

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brev.domain.Brev
import no.nav.familie.klage.brev.domain.BrevmottakereJournalposter
import no.nav.familie.klage.brevmottaker.BrevmottakerUtil.validerBrevmottakere
import no.nav.familie.klage.brevmottaker.domain.Brevmottaker
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerOrganisasjon
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonMedIdent
import no.nav.familie.klage.brevmottaker.domain.BrevmottakerPersonUtenIdent
import no.nav.familie.klage.distribusjon.JournalføringUtil.mapAvsenderMottaker
import no.nav.familie.klage.distribusjon.JournalføringUtil.mapBrevmottakerJournalpost
import no.nav.familie.klage.distribusjon.domain.BrevmottakerJournalpost
import no.nav.familie.klage.distribusjon.domain.BrevmottakerJournalpostMedIdent
import no.nav.familie.klage.distribusjon.domain.BrevmottakerJournalpostUtenIdent
import no.nav.familie.klage.fagsak.domain.Fagsak
import no.nav.familie.klage.felles.util.TaskMetadata.SAKSBEHANDLER_METADATA_KEY
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.personopplysninger.pdl.logger
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
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
class JournalførBrevTask(
    private val distribusjonService: DistribusjonService,
    private val taskService: TaskService,
    private val behandlingService: BehandlingService,
    private val brevService: BrevService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val saksbehandler = task.metadata[SAKSBEHANDLER_METADATA_KEY].toString()

        val brev = brevService.hentBrev(behandlingId)

        val mottakere = brev.mottakere ?: error("Mangler mottakere på brev for behandling=$behandlingId")
        validerBrevmottakere(behandlingId, mottakere)

        val journalposter = brev.mottakereJournalposter?.journalposter ?: emptyList()

        val journalposterMedNyePersoner =
            journalførBrevmottakere(
                brev = brev,
                mottakere = mottakere.personer,
                saksbehandler = saksbehandler,
                journalposter = journalposter,
            )

        // `journalførBrevmottakere` overskriver journalposter på brevet i databasen,
        // så vi må sende med journalpostene som ble lagt til i forrige kall
        journalførBrevmottakere(
            brev = brev,
            mottakere = mottakere.organisasjoner,
            saksbehandler = saksbehandler,
            journalposter = journalposterMedNyePersoner,
        )
    }

    private fun journalførBrevmottakere(
        brev: Brev,
        mottakere: List<Brevmottaker>,
        saksbehandler: String,
        journalposter: List<BrevmottakerJournalpost>,
    ): List<BrevmottakerJournalpost> =
        mottakere.fold(journalposter) { eksisterendeMottakere, brevmottaker ->
            journalførBrevmottaker(
                brevmottaker = brevmottaker,
                eksisterendeMottakere = eksisterendeMottakere,
                brev = brev,
                saksbehandler = saksbehandler,
            )
        }

    private fun journalførBrevmottaker(
        brevmottaker: Brevmottaker,
        eksisterendeMottakere: List<BrevmottakerJournalpost>,
        brev: Brev,
        saksbehandler: String,
    ): List<BrevmottakerJournalpost> {
        val behandlingId = brev.behandlingId
        val avsenderMottaker = mapAvsenderMottaker(brevmottaker)
        val brevPdf = brev.brevPdf()
        return if (eksisterendeMottakere.none { harLikId(it, brevmottaker) }) {
            val journalpostId =
                distribusjonService.journalførBrev(
                    behandlingId = behandlingId,
                    brev = brevPdf,
                    saksbehandler = saksbehandler,
                    index = eksisterendeMottakere.size,
                    mottaker = avsenderMottaker,
                )

            val nyMottaker =
                mapBrevmottakerJournalpost(
                    brevmottaker = brevmottaker,
                    avsenderMottaker = avsenderMottaker,
                    journalpostId = journalpostId,
                )

            val nyeMottakere = eksisterendeMottakere + nyMottaker
            brevService.oppdaterMottakerJournalpost(
                behandlingId = behandlingId,
                brevmottakereJournalposter = BrevmottakereJournalposter(nyeMottakere),
            )

            nyeMottakere
        } else {
            eksisterendeMottakere
        }
    }

    private fun harLikId(
        brevmottakerJournalpost: BrevmottakerJournalpost,
        brevmottaker: Brevmottaker,
    ) = when (brevmottakerJournalpost) {
        is BrevmottakerJournalpostMedIdent -> {
            when (brevmottaker) {
                is BrevmottakerOrganisasjon -> brevmottakerJournalpost.ident == brevmottaker.organisasjonsnummer
                is BrevmottakerPersonMedIdent -> brevmottakerJournalpost.ident == brevmottaker.personIdent
                is BrevmottakerPersonUtenIdent -> false
            }
        }

        is BrevmottakerJournalpostUtenIdent -> {
            brevmottaker is BrevmottakerPersonUtenIdent && brevmottakerJournalpost.id == brevmottaker.id
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
        val sendTilKabalTask =
            Task(
                type = DistribuerBrevTask.TYPE,
                payload = task.payload,
                properties = task.metadata,
            )
        taskService.save(sendTilKabalTask)
    }

    private fun opprettSendTilKabalTask(task: Task) {
        val sendTilKabalTask =
            Task(
                type = SendTilKabalTask.TYPE,
                payload = task.payload,
                properties = task.metadata,
            )
        taskService.save(sendTilKabalTask)
    }

    companion object {
        const val TYPE = "journalførBrevTask"

        fun opprettTask(
            fagsak: Fagsak,
            behandling: Behandling,
        ): Task =
            Task(
                type = TYPE,
                payload = behandling.id.toString(),
                properties =
                    Properties().apply {
                        this[SAKSBEHANDLER_METADATA_KEY] = SikkerhetContext.hentSaksbehandler(strict = true)
                        this["eksternFagsakId"] = fagsak.eksternId
                        this["fagsystem"] = fagsak.fagsystem.name
                    },
            )
    }
}
