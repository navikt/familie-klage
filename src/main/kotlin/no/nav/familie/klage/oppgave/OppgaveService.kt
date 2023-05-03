package no.nav.familie.klage.oppgave

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.erUnderArbeidAvSaksbehandler
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OppgaveService(private val behandleSakOppgaveRepository: BehandleSakOppgaveRepository, private val oppgaveClient: OppgaveClient, private val behandlingService: BehandlingService) {

    fun oppdaterOppgaveTilÅGjeldeTilbakekreving(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId)

        // Skal ikke oppdatere tema for oppgaver som alt er ferdigstilt
        if (!behandling.status.erUnderArbeidAvSaksbehandler()) return

        val eksisterendeOppgave = behandleSakOppgaveRepository.findByBehandlingId(behandlingId)
        val oppdatertOppgave = Oppgave(id = eksisterendeOppgave.oppgaveId, behandlingstema = Behandlingstema.Tilbakebetaling.value)

        oppgaveClient.oppdaterOppgave(oppdatertOppgave)
    }
}
