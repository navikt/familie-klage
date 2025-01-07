import no.nav.familie.klage.distribusjon.DistribusjonService
import no.nav.familie.klage.distribusjon.JournalpostBrevmottaker
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.dokdist.AdresseType
import no.nav.familie.kontrakter.felles.dokdist.ManuellAdresse
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = BaksDistribuerBrevTask.TYPE,
    beskrivelse = "Distribuer brev etter klagebehandling",
)
class BaksDistribuerBrevTask(
    private val fagsakService: FagsakService,
    private val distribusjonService: DistribusjonService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val payload = objectMapper.readValue(task.payload, Payload::class.java)
        val fagsak = fagsakService.hentFagsakForBehandling(payload.behandlingId)
        val fagsystem = when (fagsak.fagsystem) {
            no.nav.familie.kontrakter.felles.klage.Fagsystem.BA -> Fagsystem.BA
            no.nav.familie.kontrakter.felles.klage.Fagsystem.KS -> Fagsystem.KONT
            no.nav.familie.kontrakter.felles.klage.Fagsystem.EF -> throw IllegalStateException("EF er ikke støttet i denne tasken")
        }
        distribusjonService.distribuerBrev(
            journalpostId = payload.journalpostId,
            bestillendeFagsystem = fagsystem,
            manuellAdresse = payload.journalpostBrevmottaker.adresse?.mapTilManuellAdresse(),
        )
    }

    private fun JournalpostBrevmottaker.Adresse.mapTilManuellAdresse(): ManuellAdresse {
        return ManuellAdresse(
            adresseType = if (this.landkode == "NO") AdresseType.norskPostadresse else AdresseType.utenlandskPostadresse,
            adresselinje1 = this.adresselinje1,
            adresselinje2 = this.adresselinje2,
            adresselinje3 = null,
            postnummer = this.postnummer,
            poststed = this.poststed,
            land = this.landkode,
        )
    }

    data class Payload(
        val behandlingId: UUID,
        val journalpostId: String,
        val journalpostBrevmottaker: JournalpostBrevmottaker,
    )

    companion object {
        fun opprett(payload: Payload, metadata: Properties): Task {
            return Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(payload),
                properties = metadata,
            )
        }

        const val TYPE = "baksDistribuerBrevTask"
    }
}
