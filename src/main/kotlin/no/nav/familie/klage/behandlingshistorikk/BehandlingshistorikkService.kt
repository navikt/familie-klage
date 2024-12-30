package no.nav.familie.klage.behandlingshistorikk

import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandlingshistorikk.domain.Behandlingshistorikk
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BehandlingshistorikkService(private val behandlingshistorikkRepository: BehandlingshistorikkRepository) {

    fun hentBehandlingshistorikk(id: UUID): List<Behandlingshistorikk> =
        behandlingshistorikkRepository.findByBehandlingIdOrderByEndretTidDesc(id)

    fun opprettBehandlingshistorikk(behandlingId: UUID, steg: StegType): Behandlingshistorikk {
        return behandlingshistorikkRepository.insert(
            Behandlingshistorikk(behandlingId = behandlingId, steg = steg),
        )
    }
}
