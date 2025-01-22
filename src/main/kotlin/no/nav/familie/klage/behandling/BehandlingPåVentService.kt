package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.behandling.dto.SettPåVentRequest
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.behandlingshistorikk.domain.HistorikkHendelse
import no.nav.familie.klage.behandlingsstatistikk.BehandlingsstatistikkTask
import no.nav.familie.klage.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.oppgave.OppgaveService
import no.nav.familie.klage.oppgave.TilordnetRessursService
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BehandlingPåVentService(
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val behandlinghistorikkService: BehandlingshistorikkService,
    private val taskService: TaskService,
    private val tilordnetRessursService: TilordnetRessursService,
    private val oppgaveBeskrivelseService: OppgaveBeskrivelseService,
) {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun settPåVent(
        behandlingId: UUID,
        settPåVentRequest: SettPåVentRequest,
    ) {
        val behandling = behandlingService.hentBehandling(behandlingId = behandlingId)

        validerKanSettePåVent(behandlingStatus = behandling.status)

        oppdaterVerdierPåOppgave(settPåVentRequest = settPåVentRequest)

        behandlingService.oppdaterStatusPåBehandling(
            behandlingId = behandlingId,
            status = BehandlingStatus.SATT_PÅ_VENT,
        )

        behandlinghistorikkService.opprettBehandlingshistorikk(
            behandlingId = behandling.id,
            steg = behandling.steg,
            historikkHendelse = HistorikkHendelse.SATT_PÅ_VENT,
        )

        taskService.save(taskService.save(BehandlingsstatistikkTask.opprettVenterTask(behandlingId)))
    }

    @Transactional
    fun taAvVent(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId = behandlingId)

        validerKanTaAvVent(behandlingStatus = behandling.status)

        behandlingService.oppdaterStatusPåBehandling(behandlingId = behandlingId, status = BehandlingStatus.UTREDES)

        behandlinghistorikkService.opprettBehandlingshistorikk(
            behandlingId = behandling.id,
            steg = behandling.steg,
            historikkHendelse = HistorikkHendelse.TATT_AV_VENT,
        )

        fordelOppgaveTilSaksbehandler(behandlingId = behandling.id)

        taskService.save(BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId))
    }

    private fun oppdaterVerdierPåOppgave(settPåVentRequest: SettPåVentRequest) {
        val oppgave = oppgaveService.hentOppgave(gsakOppgaveId = settPåVentRequest.oppgaveId)
        val beskrivelse = oppgaveBeskrivelseService.utledOppgavebeskrivelse(
            oppgave = oppgave,
            saksbehandler = settPåVentRequest.saksbehandler,
            prioritetEndring = settPåVentRequest.prioritet,
            fristEndring = settPåVentRequest.frist,
            mappeEndring = settPåVentRequest.mappe,
            beskrivelse = settPåVentRequest.beskrivelse,
        )

        oppgaveService.oppdaterOppgave(
            Oppgave(
                id = settPåVentRequest.oppgaveId,
                tilordnetRessurs = settPåVentRequest.saksbehandler,
                prioritet = settPåVentRequest.prioritet,
                fristFerdigstillelse = settPåVentRequest.frist,
                mappeId = settPåVentRequest.mappe,
                beskrivelse = beskrivelse,
                versjon = settPåVentRequest.oppgaveVersjon,
            ),
        )
    }

    fun validerKanSettePåVent(behandlingStatus: BehandlingStatus) {
        brukerfeilHvis(behandlingStatus.erLåstForVidereBehandling()) {
            "Kan ikke sette behandling med status $behandlingStatus på vent"
        }
    }

    fun validerKanTaAvVent(behandlingStatus: BehandlingStatus) {
        brukerfeilHvis(behandlingStatus != BehandlingStatus.SATT_PÅ_VENT) {
            "Kan ikke ta behandling med status $behandlingStatus av vent"
        }
    }

    private fun fordelOppgaveTilSaksbehandler(behandlingId: UUID) {
        val oppgave = tilordnetRessursService.hentOppgave(behandlingId)
        val oppgaveId = oppgave?.oppgaveId

        if (oppgaveId != null) {
            oppgaveService.fordelOppgave(
                gsakOppgaveId = oppgaveId,
                saksbehandler = SikkerhetContext.hentSaksbehandler(),
                versjon = oppgave.versjon,
            )
        } else {
            logger.warn("Fant ikke oppgave med id $behandlingId")
        }
    }
}
