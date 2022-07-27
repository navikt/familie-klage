package no.nav.familie.klage.behandlingshistorikk

import no.nav.familie.klage.behandlingshistorikk.domain.Behandlingshistorikk
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BehandlingshistorikkService(private val behandlingshistorikkRepository: BehandlingshistorikkRepository) {

    fun hentBehandlingshistorikker(id: UUID): List<Behandlingshistorikk> = behandlingshistorikkRepository.findByBehandlingIdOrderByEndretTidDesc(id)


    fun opprettBehandlingshistorikk(behandlingshistorikk: Behandlingshistorikk): Behandlingshistorikk = behandlingshistorikkRepository
        .insert(
            Behandlingshistorikk(
                behandlingId = behandlingshistorikk.behandlingId,
                steg = behandlingshistorikk.steg,
                opprettetAv = behandlingshistorikk.opprettetAv,
            )
        )
}