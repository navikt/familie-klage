package no.nav.familie.klage.oppgave

import no.nav.familie.klage.felles.util.TaskMetadata.klageGjelderTilbakekrevingMetadataKey
import no.nav.familie.klage.felles.util.TaskMetadata.saksbehandlerMetadataKey
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
class OppgaveTaskService(private val taskService: TaskService) {
    fun opprettBehandleSakOppgave(behandlingId: UUID, klageGjelderTilbakekreving: Boolean, eksternFagsakId: String, eksternBehandlingId: String, fagsystem: Fagsystem) {
        val behandleSakOppgaveTask = Task(
            type = OpprettBehandleSakOppgaveTask.TYPE,
            payload = behandlingId.toString(),
            properties = Properties().apply {
                this[saksbehandlerMetadataKey] = SikkerhetContext.hentSaksbehandler(strict = true)
                this[klageGjelderTilbakekrevingMetadataKey] = klageGjelderTilbakekreving.toString()
                this["eksternFagsakId"] = eksternFagsakId
                this["eksternBehandlingId"] = eksternBehandlingId
                this["fagsystem"] = fagsystem.name
            },
        )
        taskService.save(behandleSakOppgaveTask)
    }

    fun lagFerdigstillOppgaveForBehandlingTask(behandlingId: UUID, eksternFagsakId: String, eksternBehandlingId: String, fagsystem: Fagsystem) {
        val ferdigstillbehandlesakOppgave = Task(
            type = OpprettFerdigstillOppgaveTask.TYPE,
            payload = behandlingId.toString(),
            properties = Properties().apply {
                this["eksternFagsakId"] = eksternFagsakId
                this["eksternBehandlingId"] = eksternBehandlingId
                this["fagsystem"] = fagsystem.name
            }
        )
        taskService.save(ferdigstillbehandlesakOppgave)
    }
}
