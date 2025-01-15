package no.nav.familie.klage.behandlingshistorikk

import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.klage.behandlingshistorikk.domain.BehandlingshistorikkDto
import no.nav.familie.klage.behandlingshistorikk.domain.tilDto
import org.springframework.stereotype.Service
import java.util.*

@Service
class BehandlingshistorikkService(private val behandlingshistorikkRepository: BehandlingshistorikkRepository) {

    fun hentBehandlingshistorikk(id: UUID): List<BehandlingshistorikkDto> {
        val historikkListe = behandlingshistorikkRepository.findByBehandlingIdOrderByEndretTidDesc(id)
        return historikkListe.map { it.tilDto() }
    }

    fun opprettBehandlingshistorikk(
        behandlingId: UUID,
        steg: StegType,
    ): Behandlingshistorikk {
        return behandlingshistorikkRepository.insert(
            Behandlingshistorikk(
                behandlingId = behandlingId,
                steg = steg,
            ),
        )
    }
}
