package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class OpprettBehandlingService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val formService: FormService
) {

    @Transactional
    fun opprettBehandling(
        opprettKlagebehandlingRequest: OpprettKlagebehandlingRequest
    ): UUID {
        feilHvis(opprettKlagebehandlingRequest.klageMottatt.isAfter(LocalDate.now())) {
            "Kan ikke opprette klage med krav mottatt frem i tid for behandling med eksternBehandlingId=${opprettKlagebehandlingRequest.eksternBehandlingId}"
        }

        val fagsak = fagsakService.hentEllerOpprettFagsak(
            ident = opprettKlagebehandlingRequest.ident,
            eksternId = opprettKlagebehandlingRequest.eksternFagsakId,
            fagsystem = opprettKlagebehandlingRequest.fagsystem,
            stønadstype = opprettKlagebehandlingRequest.stønadstype
        )

        val behandlingId = behandlingService.opprettBehandling(
            Behandling(
                fagsakId = fagsak.id,
                eksternFagsystemBehandlingId = opprettKlagebehandlingRequest.eksternBehandlingId,
                klageMottatt = opprettKlagebehandlingRequest.klageMottatt,
                behandlendeEnhet = opprettKlagebehandlingRequest.behandlendeEnhet
            )
        ).id

        return formService.opprettInitielleFormkrav(behandlingId).behandlingId
    }
}
