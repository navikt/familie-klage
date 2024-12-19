package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.behandling.dto.SettPåVentRequest
import no.nav.familie.klage.felles.util.dagensDatoMedNorskFormat
import no.nav.familie.klage.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class BehandlingPåVentService(
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
) {

    @Transactional
    fun settPåVent(
        behandlingId: UUID,
        settPåVentRequest: SettPåVentRequest,
    ) {
        val behandling = behandlingService.hentBehandling(behandlingId = behandlingId)

        validerKanSettePåVent(behandling = behandling)

        oppdaterVerdierPåOppgave(settPåVentRequest = settPåVentRequest)

        behandlingService.oppdaterStatusPåBehandling(
            behandlingId = behandlingId,
            status = BehandlingStatus.SATT_PÅ_VENT,
        )
    }

    @Transactional
    fun taAvVent(behandlingId: UUID) {
        kanTaAvVent(behandlingId = behandlingId)
        behandlingService.oppdaterStatusPåBehandling(behandlingId = behandlingId, status = BehandlingStatus.UTREDES)
    }

    private fun oppdaterVerdierPåOppgave(settPåVentRequest: SettPåVentRequest) {
        val oppgave = oppgaveService.hentOppgave(gsakOppgaveId = settPåVentRequest.oppgaveId)
        val beskrivelse = utledOppgavebeskrivelse(oppgave = oppgave, settPåVentRequest = settPåVentRequest)

        oppgaveService.oppdaterOppgave(
            Oppgave(
                id = settPåVentRequest.oppgaveId,
                tilordnetRessurs = settPåVentRequest.saksbehandler,
                prioritet = settPåVentRequest.prioritet,
                fristFerdigstillelse = settPåVentRequest.frist,
                beskrivelse = beskrivelse,
            ),
        )
    }

    private fun validerKanSettePåVent(
        behandling: Behandling,
    ) {
        brukerfeilHvis(boolean = behandling.status.erLåstForVidereBehandling()) {
            "Kan ikke sette behandling med status ${behandling.status} på vent"
        }
    }

    private fun kanTaAvVent(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId = behandlingId)

        brukerfeilHvis(boolean = behandling.status != BehandlingStatus.SATT_PÅ_VENT && behandling.status != BehandlingStatus.FERDIGSTILT) {
            "Kan ikke ta behandling med status ${behandling.status} av vent"
        }
    }

    private fun utledOppgavebeskrivelse(
        oppgave: Oppgave,
        settPåVentRequest: SettPåVentRequest,
    ): String {
        val tilordnetSaksbehandler = utledTilordnetSaksbehandlerBeskrivelse(
            oppgave = oppgave,
            settPåVentRequest = settPåVentRequest,
        )
        val prioritet = utledPrioritetBeskrivelse(oppgave = oppgave, settPåVentRequest = settPåVentRequest)
        val frist = utledFristBeskrivelse(oppgave = oppgave, settPåVentRequest = settPåVentRequest)

        val harEndringer = listOf(tilordnetSaksbehandler, prioritet, frist).any { it.isNotBlank() }
        val beskrivelse = utledNyBeskrivelse(settPåVentRequest = settPåVentRequest)
        val skalOppdatereBeskrivelse = harEndringer || beskrivelse.isNotBlank()

        val tidligereBeskrivelse = if (skalOppdatereBeskrivelse && oppgave.beskrivelse?.isNotBlank() == true) {
            "\n${oppgave.beskrivelse.orEmpty()}"
        } else {
            oppgave.beskrivelse.orEmpty()
        }

        val prefix = utledBeskrivelsePrefix()

        return if (skalOppdatereBeskrivelse) {
            (prefix + beskrivelse + tilordnetSaksbehandler + prioritet + frist + tidligereBeskrivelse).trimEnd()
        } else {
            tidligereBeskrivelse.trimEnd()
        }
    }

    private fun utledBeskrivelsePrefix(): String {
        val innloggetSaksbehandlerIdent = SikkerhetContext.hentSaksbehandler()
        val saksbehandlerNavn = SikkerhetContext.hentSaksbehandlerNavn(strict = false)
        return "--- ${dagensDatoMedNorskFormat()} $saksbehandlerNavn ($innloggetSaksbehandlerIdent) ---\n"
    }

    private fun utledTilordnetSaksbehandlerBeskrivelse(
        oppgave: Oppgave,
        settPåVentRequest: SettPåVentRequest,
    ): String {
        val eksisterendeSaksbehandler = oppgave.tilordnetRessurs ?: "ingen"
        val nySaksbehandler =
            if (settPåVentRequest.saksbehandler == "") "ingen" else settPåVentRequest.saksbehandler

        return if (eksisterendeSaksbehandler == nySaksbehandler) {
            ""
        } else {
            "Oppgave flyttet fra saksbehandler $eksisterendeSaksbehandler til ${nySaksbehandler}\n"
        }
    }

    private fun utledPrioritetBeskrivelse(
        oppgave: Oppgave,
        settPåVentRequest: SettPåVentRequest,
    ): String {
        return if (oppgave.prioritet != settPåVentRequest.prioritet) {
            "Oppgave endret fra prioritet ${oppgave.prioritet?.name} til ${settPåVentRequest.prioritet}\n"
        } else {
            ""
        }
    }

    private fun utledFristBeskrivelse(
        oppgave: Oppgave,
        settPåVentRequest: SettPåVentRequest,
    ): String {
        val eksisterendeFrist = oppgave.fristFerdigstillelse
        val nyFrist = settPåVentRequest.frist
        return if (eksisterendeFrist != nyFrist) {
            "Oppgave endret frist fra $eksisterendeFrist til $nyFrist\n"
        } else {
            ""
        }
    }

    private fun utledNyBeskrivelse(
        settPåVentRequest: SettPåVentRequest,
    ): String {
        return if (settPåVentRequest.beskrivelse.isNotBlank()) {
            "${settPåVentRequest.beskrivelse}\n"
        } else {
            ""
        }
    }
}
