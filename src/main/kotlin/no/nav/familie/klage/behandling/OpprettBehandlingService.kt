package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.PåklagetVedtak
import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.formkrav.FormService
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.oppgave.OppgaveTaskService
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class OpprettBehandlingService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val formService: FormService,
    private val oppgaveTaskService: OppgaveTaskService
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
                påklagetVedtak = PåklagetVedtak(
                    eksternFagsystemBehandlingId = opprettKlagebehandlingRequest.eksternBehandlingId,
                    påklagetVedtakstype = if (opprettKlagebehandlingRequest.eksternBehandlingId != null) PåklagetVedtakstype.Vedtak else PåklagetVedtakstype.IkkeValgt
                ),
                klageMottatt = opprettKlagebehandlingRequest.klageMottatt,
                behandlendeEnhet = opprettKlagebehandlingRequest.behandlendeEnhet
            )
        ).id

        formService.opprettInitielleFormkrav(behandlingId)

        oppgaveTaskService.opprettBehandleSakOppgave(behandlingId)

        return behandlingId
    }
}
