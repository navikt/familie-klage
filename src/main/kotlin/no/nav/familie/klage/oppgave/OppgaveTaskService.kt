package no.nav.familie.klage.oppgave

import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.oppgave.OpprettBehandleSakOppgaveTask.Companion.saksbehandlerMetadataKey
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
class OppgaveTaskService(private val taskRepository: TaskRepository) {
    fun opprettBehandleSakOppgave(behandlingId: UUID) {
        val behandleSakOppgaveTask = Task(
            type = OpprettBehandleSakOppgaveTask.TYPE,
            payload = behandlingId.toString(),
            properties = Properties().apply {
                this[saksbehandlerMetadataKey] = SikkerhetContext.hentSaksbehandler(strict = true)
            }
        )
        taskRepository.save(behandleSakOppgaveTask)
    }

    fun lagFerdigstillOppgaveForBehandlingTask(behandlingId: UUID) {
        val ferdigstillbehandlesakOppgave = Task(
            type = OpprettFerdigstillOppgaveTask.TYPE,
            payload = behandlingId.toString()
        )
        taskRepository.save(ferdigstillbehandlesakOppgave)
    }
}
