package no.nav.familie.klage.oppgave

import no.nav.familie.klage.felles.util.TaskMetadata.klageGjelderTilbakekrevingMetadataKey
import no.nav.familie.klage.felles.util.TaskMetadata.saksbehandlerMetadataKey
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
class OppgaveTaskService(private val taskService: TaskService) {
    fun opprettBehandleSakOppgave(behandlingId: UUID, klageGjelderTilbakekreving: Boolean) {
        val behandleSakOppgaveTask = Task(
            type = OpprettBehandleSakOppgaveTask.TYPE,
            payload = behandlingId.toString(),
            properties = Properties().apply {
                this[saksbehandlerMetadataKey] = SikkerhetContext.hentSaksbehandler(strict = true)
                this[klageGjelderTilbakekrevingMetadataKey] = klageGjelderTilbakekreving.toString()
            },
        )
        taskService.save(behandleSakOppgaveTask)
    }

    fun lagFerdigstillOppgaveForBehandlingTask(behandlingId: UUID) {
        val ferdigstillbehandlesakOppgave = Task(
            type = OpprettFerdigstillOppgaveTask.TYPE,
            payload = behandlingId.toString(),
        )
        taskService.save(ferdigstillbehandlesakOppgave)
    }
}
