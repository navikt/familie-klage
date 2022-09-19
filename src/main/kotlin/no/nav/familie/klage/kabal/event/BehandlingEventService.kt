package no.nav.familie.klage.kabal.event

import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.fagsak.FagsakRepository
import no.nav.familie.klage.kabal.BehandlingEvent
import no.nav.familie.klage.kabal.BehandlingEventType
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BehandlingEventService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val taskRepository: TaskRepository,
    private val stegService: StegService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun handleEvent(behandlingEvent: BehandlingEvent) {
        val eksternBehandlingId = UUID.fromString(behandlingEvent.kildeReferanse)
        val behandling = behandlingRepository.findByEksternBehandlingId(eksternBehandlingId)

        when (behandlingEvent.type) {
            BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET -> behandleKlageAvsluttet(behandling, behandlingEvent)
            else -> behandleAnke(behandling, behandlingEvent)
        }
    }

    private fun behandleAnke(behandling: Behandling, behandlingEvent: BehandlingEvent) {
        opprettOppgaveTask(behandlingEvent, behandling)
    }

    private fun behandleKlageAvsluttet(behandling: Behandling, behandlingEvent: BehandlingEvent) {
        when (behandling.status) {
            BehandlingStatus.FERDIGSTILT -> logger.error("Mottatt event på ferdigstilt behandling $behandlingEvent - event kan være lest fra før") // TODO korrigeringer - kan vi få det?
            else -> {
                opprettOppgaveTask(behandlingEvent, behandling)
                ferdigstillKlagebehandling(behandling)
            }
        }
    }

    private fun opprettOppgaveTask(behandlingEvent: BehandlingEvent, behandling: Behandling) {

        val fagsakDomain = fagsakRepository.finnFagsakForBehandlingId(behandling.id) ?: error("Finner ikke fagsak for behandlingId: ${behandling.id}")
        val oppgaveTekst = "${behandlingEvent.detaljer.oppgaveTekst()} Gjelder: ${fagsakDomain.stønadstype}"
        val klageBehandlingEksternId = UUID.fromString(behandlingEvent.kildeReferanse)

        val opprettOppgavePayload = OpprettOppgavePayload(klageBehandlingEksternId, oppgaveTekst, fagsakDomain.fagsystem)
        val opprettOppgaveTask = OpprettOppgaveTask.opprettTask(opprettOppgavePayload)
        taskRepository.save(opprettOppgaveTask)
    }

    private fun ferdigstillKlagebehandling(behandling: Behandling) {
        stegService.oppdaterSteg(behandling.id, StegType.BEHANDLING_FERDIGSTILT)
    }
}
