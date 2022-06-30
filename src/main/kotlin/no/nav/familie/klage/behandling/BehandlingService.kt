package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.BehandlingStatus
import no.nav.familie.klage.behandling.domain.BehandlingSteg
import no.nav.familie.klage.behandling.domain.Fagsystem
import no.nav.familie.klage.behandling.dto.BehandlingDto
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class BehandlingService(private val behandlingsRepository: BehandlingsRepository) {

    fun opprettBehandlingDto(behandlingId: UUID): BehandlingDto {
        return behandlingDto(behandlingId)
    }

    fun opprettBehandling(fagsakId: UUID, fagsystem: Fagsystem): Behandling {
        return behandlingsRepository.insert(
            Behandling(
                fagsakId = fagsakId,
                steg = BehandlingSteg.FORMALKRAV,
                status = BehandlingStatus.OPPRETTET,
                endretTid = LocalDateTime.now(),
                opprettetTid = LocalDateTime.now(),
                fagsystem = fagsystem
            )
        )
    }
}