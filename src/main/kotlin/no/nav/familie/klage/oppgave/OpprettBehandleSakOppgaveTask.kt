package no.nav.familie.klage.oppgave

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.felles.util.TaskMetadata.klageGjelderTilbakekrevingMetadataKey
import no.nav.familie.klage.felles.util.TaskMetadata.saksbehandlerMetadataKey
import no.nav.familie.klage.oppgave.OppgaveUtil.lagFristForOppgave
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettBehandleSakOppgaveTask.TYPE,
    beskrivelse = "Opprett behandle sak oppgave",
)
class OpprettBehandleSakOppgaveTask(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val oppgaveClient: OppgaveClient,
    private val behandleSakOppgaveRepository: BehandleSakOppgaveRepository,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        val klageGjelderTilbakekreving: Boolean =
            task.metadata.getProperty(klageGjelderTilbakekrevingMetadataKey).toBoolean()

        val oppgaveRequest = OpprettOppgaveRequest(
            ident = OppgaveIdentV2(ident = fagsak.hentAktivIdent(), gruppe = IdentGruppe.FOLKEREGISTERIDENT),
            saksId = fagsak.eksternId, // fagsakId fra fagsystem
            tema = fagsak.stønadstype.tilTema(), //
            oppgavetype = Oppgavetype.BehandleSak,
            fristFerdigstillelse = lagFristForOppgave(LocalDateTime.now()),
            beskrivelse = "Klagebehandling i ny løsning",
            enhetsnummer = behandling.behandlendeEnhet,
            behandlingstype = Behandlingstype.Klage.value,
            behandlesAvApplikasjon = "familie-klage",
            tilordnetRessurs = task.metadata.getProperty(saksbehandlerMetadataKey),
            behandlingstema = if (klageGjelderTilbakekreving) Behandlingstema.Tilbakebetaling.value else null,
        )
        try {
            opprettOppgaveOgLagre(oppgaveRequest, behandlingId)
        } catch (e: Exception) {
            if (navIdentHarIkkeTilgangTilEnheten(e)) {
                opprettOppgaveOgLagre(oppgaveRequest.copy(tilordnetRessurs = null), behandlingId)
            } else {
                throw e
            }
        }
    }

    private fun opprettOppgaveOgLagre(request: OpprettOppgaveRequest, behandlingId: UUID) {
        val oppgaveId = oppgaveClient.opprettOppgave(opprettOppgaveRequest = request)
        behandleSakOppgaveRepository.insert(
            BehandleSakOppgave(behandlingId = behandlingId, oppgaveId = oppgaveId),
        )
    }

    private fun navIdentHarIkkeTilgangTilEnheten(e: Exception): Boolean =
        e.message?.contains("navIdent har ikke tilgang til enheten") ?: false

    companion object {
        const val TYPE = "opprettBehandleSakoppgave"
    }
}
