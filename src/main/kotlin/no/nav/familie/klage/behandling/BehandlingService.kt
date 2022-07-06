package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.BehandlingStatus
import no.nav.familie.klage.behandling.domain.BehandlingSteg
import no.nav.familie.klage.behandling.domain.Fagsystem
import no.nav.familie.klage.behandling.dto.BehandlingDto
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.klage.behandlingshistorikk.domain.Steg
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class BehandlingService(
        private val behandlingsRepository: BehandlingsRepository,
        private val behandlingshistorikkService: BehandlingshistorikkService,
    ) {

    fun opprettBehandlingDto(behandlingId: UUID): BehandlingDto {
        return behandlingDto(behandlingId)
    }

    fun opprettBehandling(): Behandling {
        val fagsakId = UUID.randomUUID()
        val behandling = behandlingsRepository.insert(
            Behandling(
                fagsakId = fagsakId,
                steg = BehandlingSteg.FORMALKRAV,
                status = BehandlingStatus.OPPRETTET,
                endretTid = LocalDateTime.now(),
                opprettetTid = LocalDateTime.now(),
                fagsystem = Fagsystem.EF,
            )
        )

        behandlingshistorikkService.opprettBehandlingshistorikk(
            behandlingshistorikk = Behandlingshistorikk(
                behandlingId = behandling.id,
                steg = Steg.OPPRETTET,
                opprettetAv = "Juni Leirvik"
            )
        )

        return behandling
    }
}