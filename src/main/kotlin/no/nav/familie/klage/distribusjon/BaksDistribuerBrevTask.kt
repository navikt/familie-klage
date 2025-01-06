import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.klage.distribusjon.DistribusjonService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = BaksDistribuerBrevTask.TYPE,
    beskrivelse = "Distribuer brev etter klagebehandling",
)
class BaksDistribuerBrevTask(
    private val distribusjonService: DistribusjonService,
    @Qualifier("objectMapper") private val objectMapper: ObjectMapper,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val payload = objectMapper.readValue(task.payload, Payload::class.java)
        distribusjonService.distribuerBrev(payload.journalpostId)
    }

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

    data class Payload(
        val behandlingId: UUID,
        val journalpostId: String,
    )
}
