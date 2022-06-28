package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.dto.BehandlingDto
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BehandlingService {

    fun opprettBehandlingDto(behandlingId: UUID): BehandlingDto {
        return behandlingDto(behandlingId)
    }

    fun opprettBehandlingerDto(behandlingIder: List<UUID>): List<BehandlingDto> {
        return behandlingIder.map { behandlingDto(it) }
    }
}