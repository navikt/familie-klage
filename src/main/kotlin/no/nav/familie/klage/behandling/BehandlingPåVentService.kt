package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.behandling.dto.SettPåVentRequest
import no.nav.familie.klage.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BehandlingPåVentService(
    private val behandlingService: BehandlingService,
) {

    @Transactional
    fun settPåVent(
        behandlingId: UUID,
        settPåVentRequest: SettPåVentRequest,
    ) {
        val behandling = behandlingService.hentBehandling(behandlingId)

        validerKanSettePåVent(behandling)

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
}
