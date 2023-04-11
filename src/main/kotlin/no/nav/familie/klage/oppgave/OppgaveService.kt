package no.nav.familie.klage.oppgave

import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import org.springframework.stereotype.Service
import java.util.*

@Service
class OppgaveService(private val behandleSakOppgaveRepository: BehandleSakOppgaveRepository, private val oppgaveClient: OppgaveClient) {

    fun oppdaterOppgaveTilÅGjeldeTilbakekreving(behandlingId: UUID): Long {
        val eksisterendeOppgave = behandleSakOppgaveRepository.findByBehandlingId(behandlingId)
        val oppdatertOppgave = Oppgave(id = eksisterendeOppgave.oppgaveId, behandlingstema = Behandlingstema.Tilbakebetaling.value)

        return oppgaveClient.oppdaterOppgave(oppdatertOppgave)
    }
}
