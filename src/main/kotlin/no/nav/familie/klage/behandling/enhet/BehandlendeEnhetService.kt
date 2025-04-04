package no.nav.familie.klage.behandling.enhet

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.dto.OppdaterBehandlendeEnhetRequest
import no.nav.familie.klage.fagsak.FagsakService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BehandlendeEnhetService(
    val behandlingService: BehandlingService,
    val fagsakService: FagsakService,
) {

    @Transactional
    fun oppdaterBehandlendeEnhet(behandlingId: UUID, oppdaterBehandlendeEnhetRequest: OppdaterBehandlendeEnhetRequest) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

        EnhetValidator.validerEnhetForFagsystem(
            enhetsnummer = oppdaterBehandlendeEnhetRequest.enhetsnummer,
            fagsystem = fagsak.fagsystem
        )

        behandlingService.oppdaterBehandlendeEnhet(behandlingId, oppdaterBehandlendeEnhetRequest.enhetsnummer)
    }
}