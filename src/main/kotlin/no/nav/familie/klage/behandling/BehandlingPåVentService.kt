package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.behandling.dto.SettPåVentRequest
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.klage.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BehandlingPåVentService(
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
) {

    @Transactional
    fun settPåVent(
        behandlingId: UUID,
        settPåVentRequest: SettPåVentRequest,
    ) {
        val behandling = behandlingService.hentBehandling(behandlingId)

        validerKanSettePåVent(behandling)

        opprettHistorikkInnslag(behandling = behandling, behandlingStatus = BehandlingStatus.SATT_PÅ_VENT)

        oppdaterVerdierPåOppgave(settPåVentRequest)

        behandlingService.oppdaterStatusPåBehandling(
            behandlingId = behandlingId,
            status = BehandlingStatus.SATT_PÅ_VENT,
        )
    }

    @Transactional
    fun taAvVent(behandlingId: UUID) {
        kanTaAvVent(behandlingId = behandlingId)
        behandlingService.oppdaterStatusPåBehandling(behandlingId = behandlingId, status = BehandlingStatus.UTREDES)
    }

    private fun oppdaterVerdierPåOppgave(settPåVentRequest: SettPåVentRequest) {
        oppgaveService.oppdaterOppgave(
            Oppgave(
                id = settPåVentRequest.oppgaveId,
                tilordnetRessurs = settPåVentRequest.saksbehandler,
                prioritet = settPåVentRequest.prioritet,
                fristFerdigstillelse = settPåVentRequest.frist,
                beskrivelse = settPåVentRequest.beskrivelse,
            ),
        )
    }

    private fun validerKanSettePåVent(
        behandling: Behandling,
    ) {
        brukerfeilHvis(behandling.status.erLåstForVidereBehandling()) {
            "Kan ikke sette behandling med status ${behandling.status} på vent"
        }
    }

    private fun kanTaAvVent(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId = behandlingId)

        brukerfeilHvis(boolean = behandling.status != BehandlingStatus.SATT_PÅ_VENT && behandling.status != BehandlingStatus.FERDIGSTILT) {
            "Kan ikke ta behandling med status ${behandling.status} av vent"
        }
    }

    private fun opprettHistorikkInnslag(behandling: Behandling, behandlingStatus: BehandlingStatus) {
        // TODO: stegType er kanskje feil, ikke bruk denne.
        behandlingshistorikkService.opprettBehandlingshistorikk(
            behandlingId = behandling.id,
            steg = behandling.steg,
            behandlingStatus = behandlingStatus
        )
    }
}
