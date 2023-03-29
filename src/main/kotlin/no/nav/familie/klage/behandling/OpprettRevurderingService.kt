package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.OpprettRevurderingUtil.skalOppretteRevurderingAutomatisk
import no.nav.familie.klage.integrasjoner.FagsystemVedtakService
import no.nav.familie.kontrakter.felles.klage.KanOppretteRevurderingResponse
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OpprettRevurderingService(
    private val behandlingService: BehandlingService,
    private val fagsystemVedtakService: FagsystemVedtakService,
) {

    fun kanOppretteRevurdering(behandlingId: UUID): KanOppretteRevurderingResponse {
        val behandling = behandlingService.hentBehandling(behandlingId)
        return if (skalOppretteRevurderingAutomatisk(behandling.påklagetVedtak)) {
            fagsystemVedtakService.kanOppretteRevurdering(behandlingId)
        } else {
            KanOppretteRevurderingResponse(false, null)
        }
    }
}
