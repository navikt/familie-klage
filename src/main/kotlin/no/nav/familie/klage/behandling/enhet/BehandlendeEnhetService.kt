package no.nav.familie.klage.behandling.enhet

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.dto.OppdaterBehandlendeEnhetRequest
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.behandlingshistorikk.domain.HistorikkHendelse
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.infrastruktur.exception.Feil
import no.nav.familie.klage.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.klage.Fagsystem
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
    fun oppdaterBehandlendeEnhet(behandlingId: UUID, oppdaterBehandlendeEnhetRequest: OppdaterBehandlendeEnhetRequest) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

        EnhetValidator.validerEnhetForFagsystem(
            enhetsnummer = oppdaterBehandlendeEnhetRequest.enhetsnummer,
            fagsystem = fagsak.fagsystem
        )

        val behandlendeEnhet = utledEnhetForFagsystem(fagsak.fagsystem, behandling.behandlendeEnhet)
        val nyBehandlendeEnhet = utledEnhetForFagsystem(fagsak.fagsystem, oppdaterBehandlendeEnhetRequest.enhetsnummer)

        behandlingService.oppdaterBehandlendeEnhet(behandlingId, nyBehandlendeEnhet)

        oppgaveService.oppdaterEnhetPåBehandleSakOppgave(behandling.id, nyBehandlendeEnhet)



        behandlingshistorikkService.opprettBehandlingshistorikk(
            behandlingId = behandlingId,
            steg = behandling.steg,
            historikkHendelse = HistorikkHendelse.BEHANDLENDE_ENHET_ENDRET,
            beskrivelse = "Behandlende enhet endret fra ${behandlendeEnhet.enhetsnummer} (${behandlendeEnhet.enhetsnavn}) til " +
                    "${nyBehandlendeEnhet.enhetsnummer} (${nyBehandlendeEnhet.enhetsnavn})."
                    + "\n\n${oppdaterBehandlendeEnhetRequest.begrunnelse}"
        )
    }

    private fun utledEnhetForFagsystem(fagsystem: Fagsystem, enhetsnummer: String): Enhet =
        when (fagsystem) {
            Fagsystem.BA -> BarnetrygdEnhet.values().single { it.enhetsnummer == enhetsnummer }
            Fagsystem.KS -> KontantstøtteEnhet.values().single { it.enhetsnummer == enhetsnummer }
            else -> throw Feil("Støtter ikke endring av enhet for fagsystem $fagsystem")
        }

}