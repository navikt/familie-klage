package no.nav.familie.klage.behandling.enhet

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.behandlingshistorikk.domain.HistorikkHendelse
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.oppgave.OppgaveService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BehandlendeEnhetService(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val oppgaveService: OppgaveService,
) {
    @Transactional
    fun oppdaterBehandlendeEnhet(
        behandlingId: UUID,
        enhetsnummer: String,
        begrunnelse: String,
    ) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsystem = fagsakService.hentFagsak(behandling.fagsakId).fagsystem

        val eksisterendeBehandlendeEnhet =
            Enhet.finnEnhet(
                fagsystem = fagsystem,
                enhetsnummer = behandling.behandlendeEnhet,
            )

        val nyBehandlendeEnhet =
            Enhet.finnEnhet(
                fagsystem = fagsystem,
                enhetsnummer = enhetsnummer,
            )

        behandlingService.oppdaterBehandlendeEnhet(
            behandlingId = behandling.id,
            behandlendeEnhet = nyBehandlendeEnhet,
            fagsystem = fagsystem,
        )

        oppgaveService.oppdaterEnhetPåBehandleSakOppgave(
            behandlingId = behandling.id,
            behandlendeEnhet = nyBehandlendeEnhet,
        )

        behandlingshistorikkService.opprettBehandlingshistorikk(
            behandlingId = behandling.id,
            steg = behandling.steg,
            historikkHendelse = HistorikkHendelse.BEHANDLENDE_ENHET_ENDRET,
            beskrivelse =
                "Behandlende enhet endret fra ${eksisterendeBehandlendeEnhet.enhetsnummer} (${eksisterendeBehandlendeEnhet.enhetsnavn}) til " +
                    "${nyBehandlendeEnhet.enhetsnummer} (${nyBehandlendeEnhet.enhetsnavn})." +
                    "\n\n$begrunnelse",
        )
    }
}
