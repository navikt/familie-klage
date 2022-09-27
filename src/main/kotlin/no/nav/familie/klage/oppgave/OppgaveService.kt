package no.nav.familie.klage.oppgave

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.oppgave.OppgaveUtil.lagFristForOppgave
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class OppgaveService(
    private val oppgaveClient: OppgaveClient,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val behandleSakOppgaveRepository: BehandleSakOppgaveRepository
) {

    val BEHANDLINGSTYPE_KLAGE = "ae0058"

    fun opprettBehandleSakOppgave(behandlingId: UUID) {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)

        val oppgaveRequest = OpprettOppgaveRequest(
            ident = OppgaveIdentV2(ident = fagsak.hentAktivIdent(), gruppe = IdentGruppe.FOLKEREGISTERIDENT),
            saksId = fagsak.eksternId, // fagsakId fra fagsystem
            tema = fagsak.stønadstype.tilTema(), //
            oppgavetype = Oppgavetype.BehandleSak,
            fristFerdigstillelse = lagFristForOppgave(LocalDateTime.now()),
            beskrivelse = "Klagebehandling i ny løsning",
            enhetsnummer = behandling.behandlendeEnhet,
            behandlingstype = BEHANDLINGSTYPE_KLAGE,
            behandlesAvApplikasjon = "familie-klage",
            tilordnetRessurs = SikkerhetContext.hentSaksbehandler(strict = true),
            behandlingstema = null
        )

        val oppgaveId = oppgaveClient.opprettOppgave(opprettOppgaveRequest = oppgaveRequest)
        behandleSakOppgaveRepository.insert(
            BehandleSakOppgave(behandlingId = behandling.id, oppgaveId = oppgaveId)
        )
    }
}
