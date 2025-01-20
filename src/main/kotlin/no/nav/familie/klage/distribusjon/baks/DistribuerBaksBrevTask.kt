package no.nav.familie.klage.distribusjon.baks

import no.nav.familie.klage.distribusjon.DistribusjonService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.kontrakter.felles.Fagsystem
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
    taskStepType = DistribuerBaksBrevTask.TYPE,
    beskrivelse = "Distribuer baks brev etter klagebehandling",
)
class DistribuerBaksBrevTask(
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
        // TODO : Burde man forhindre å distribuere brev til de som allerede har fått brev? Ser ut som det er gjort noe slikt i EF koden
        distribusjonService.distribuerBrev(
            journalpostId = payload.journalpostId,
            bestillendeFagsystem = fagsystem,
            manuellAdresse = payload.manuellAdresse,
        )
    }

    data class Payload(
        val behandlingId: UUID,
        val journalpostId: String,
        val manuellAdresse: ManuellAdresse? = null,
    )

    companion object {
        const val TYPE = "distribuerBaksBrevTask"

        fun opprett(payload: Payload, metadata: Properties): Task {
            return Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(payload),
                properties = metadata,
            )
        }
    }
}
