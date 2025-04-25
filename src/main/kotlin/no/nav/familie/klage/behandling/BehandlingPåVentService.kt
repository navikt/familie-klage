package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.behandling.dto.SettPåVentRequest
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.behandlingshistorikk.domain.HistorikkHendelse
import no.nav.familie.klage.behandlingsstatistikk.BehandlingsstatistikkTask
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.felles.util.dagensDatoMedNorskFormat
import no.nav.familie.klage.infrastruktur.exception.Feil
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
    private val fagsakService: FagsakService,
) {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun settPåVent(
        behandlingId: UUID,
        settPåVentRequest: SettPåVentRequest,
    ) {
        val behandling = behandlingService.hentBehandling(behandlingId = behandlingId)
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)

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

        taskService.save(
            BehandlingsstatistikkTask.opprettVenterTask(
                behandlingId,
                fagsak.eksternId,
                fagsak.fagsystem,
            ),
        )
    }

    @Transactional
    fun taAvVent(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId = behandlingId)
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)

        validerKanTaAvVent(behandlingStatus = behandling.status)

        behandlingService.oppdaterStatusPåBehandling(behandlingId = behandlingId, status = BehandlingStatus.UTREDES)

        behandlinghistorikkService.opprettBehandlingshistorikk(
            behandlingId = behandling.id,
            steg = behandling.steg,
            historikkHendelse = HistorikkHendelse.TATT_AV_VENT,
        )

        fordelOppgaveTilSaksbehandler(behandlingId = behandling.id)

        taskService.save(
            BehandlingsstatistikkTask.opprettPåbegyntTask(
                behandlingId,
                fagsak.eksternId,
                fagsak.fagsystem,
            ),
        )
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

    private fun utledOppgavebeskrivelse(
        oppgave: Oppgave,
        settPåVentRequest: SettPåVentRequest,
    ): String {
        val tilordnetSaksbehandler =
            utledTilordnetSaksbehandlerBeskrivelse(
                oppgave = oppgave,
                settPåVentRequest = settPåVentRequest,
            )
        val prioritet = utledPrioritetBeskrivelse(oppgave = oppgave, settPåVentRequest = settPåVentRequest)
        val frist = utledFristBeskrivelse(oppgave = oppgave, settPåVentRequest = settPåVentRequest)
        val mappe = utledMappeBeskrivelse(oppgave = oppgave, settPåVentRequest = settPåVentRequest)

        val harEndringer = listOf(tilordnetSaksbehandler, prioritet, frist, mappe).any { it.isNotBlank() }
        val beskrivelse = utledNyBeskrivelse(settPåVentRequest = settPåVentRequest)
        val skalOppdatereBeskrivelse = harEndringer || beskrivelse.isNotBlank()

        val tidligereBeskrivelse =
            if (skalOppdatereBeskrivelse && oppgave.beskrivelse?.isNotBlank() == true) {
                "\n${oppgave.beskrivelse.orEmpty()}"
            } else {
                oppgave.beskrivelse.orEmpty()
            }

        val prefix = utledBeskrivelsePrefix()

        return if (skalOppdatereBeskrivelse) {
            (prefix + beskrivelse + tilordnetSaksbehandler + prioritet + frist + mappe + tidligereBeskrivelse).trimEnd()
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
        val eksisterendeSaksbehandler = oppgave.tilordnetRessurs ?: INGEN_PLACEHOLDER
        val nySaksbehandler =
            if (settPåVentRequest.saksbehandler == "") INGEN_PLACEHOLDER else settPåVentRequest.saksbehandler

        return if (eksisterendeSaksbehandler == nySaksbehandler) {
            ""
        } else {
            "Oppgave flyttet fra saksbehandler $eksisterendeSaksbehandler til ${nySaksbehandler}\n"
        }
    }

    private fun utledPrioritetBeskrivelse(
        oppgave: Oppgave,
        settPåVentRequest: SettPåVentRequest,
    ): String =
        if (oppgave.prioritet != settPåVentRequest.prioritet) {
            "Oppgave endret fra prioritet ${oppgave.prioritet?.name} til ${settPåVentRequest.prioritet}\n"
        } else {
            ""
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

    private fun utledMappeBeskrivelse(
        oppgave: Oppgave,
        settPåVentRequest: SettPåVentRequest,
    ): String {
        val mapper =
            oppgaveService.finnMapper(
                listOf(
                    oppgave.tildeltEnhetsnr ?: throw Feil("Kan ikke finne mapper når oppgave mangler enhet"),
                ),
            )

        val eksisterendeMappe = mapper.find { it.id.toLong() == oppgave.mappeId }?.navn ?: INGEN_PLACEHOLDER
        val nyMappe = mapper.find { it.id.toLong() == settPåVentRequest.mappe }?.navn ?: INGEN_PLACEHOLDER

        return if (eksisterendeMappe == nyMappe) "" else "Oppgave flyttet fra mappe $eksisterendeMappe til $nyMappe\n"
    }

    private fun utledNyBeskrivelse(settPåVentRequest: SettPåVentRequest): String =
        if (settPåVentRequest.beskrivelse.isNotBlank()) {
            "${settPåVentRequest.beskrivelse}\n"
        } else {
            ""
        }

    companion object {
        const val INGEN_PLACEHOLDER = "<ingen>"
    }
}
