import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.klage.distribusjon.BaksDistribuerBrevDto
import no.nav.familie.klage.distribusjon.DistribusjonService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

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
        val baksDistribuerBrevDto = objectMapper.readValue(task.payload, BaksDistribuerBrevDto::class.java)
        distribusjonService.distribuerBrev(baksDistribuerBrevDto.journalpostId)
    }

    companion object {
        const val TYPE = "baksDistribuerBrevTask"
    }
}
