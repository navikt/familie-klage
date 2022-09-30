package no.nav.familie.klage.oppgave

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.oppgave.OpprettBehandleSakOppgaveTask.Companion.saksbehandlerMetadataKey
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
class OppgaveService(
    private val taskRepository: TaskRepository,
    private val behandlingService: BehandlingService
) {
    fun opprettBehandleSakOppgave(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val behandleSakOppgaveTask = Task(
            type = OpprettBehandleSakOppgaveTask.TYPE,
            payload = behandling.id.toString(),
            properties = Properties().apply {
                this[saksbehandlerMetadataKey] = SikkerhetContext.hentSaksbehandler(strict = true)
            }
        )
        taskRepository.save(behandleSakOppgaveTask)
    }

    fun lagFerdigstillOppgaveForBehandlingTask(behandling: Behandling) {
        val ferdigstillbehandlesakOppgave = Task(
            type = OpprettFerdigstillOppgaveTask.TYPE,
            payload = behandling.id.toString()
        )
        taskRepository.save(ferdigstillbehandlesakOppgave)
    }
}
