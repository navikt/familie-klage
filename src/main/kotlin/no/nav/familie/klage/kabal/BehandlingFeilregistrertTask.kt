package no.nav.familie.klage.kabal

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.StegService
import no.nav.familie.klage.behandling.domain.StegType
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.oppgave.OpprettKabalEventOppgaveTask
import no.nav.familie.klage.oppgave.OpprettOppgavePayload
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandlingFeilregistrertTask.TYPE,
    beskrivelse = "Håndter feilregistret klage fra kabal",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
)
class BehandlingFeilregistrertTask(
    private val stegService: StegService,
    private val taskService: TaskService,
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)

        taskService.save(lagOpprettOppgaveTask(behandlingId))

        stegService.oppdaterSteg(
            behandlingId,
            StegType.KABAL_VENTER_SVAR,
            StegType.BEHANDLING_FERDIGSTILT,
        )
    }

    private fun lagOpprettOppgaveTask(behandlingId: UUID): Task {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val årsakFeilregistrert =
            behandlingService
                .hentKlageresultatDto(behandlingId)
                .single()
                .årsakFeilregistrert ?: error("Fant ikke årsak for feilregistrering")

        return OpprettKabalEventOppgaveTask.opprettTask(
            OpprettOppgavePayload(
                klagebehandlingEksternId = behandling.eksternBehandlingId,
                oppgaveTekst = lagOppgavebeskrivelse(årsakFeilregistrert),
                fagsystem = fagsak.fagsystem,
                klageinstansUtfall = null,
                behandlingstype = Behandlingstype.Klage.value,
            ),
            eksternFagsakId = fagsak.eksternId,
            fagsystem = fagsak.fagsystem,
        )
    }

    private fun lagOppgavebeskrivelse(årsakFeilregistrert: String) = "Klagebehandlingen er sendt tilbake fra KA med status feilregistrert.\n\nBegrunnelse fra KA: \"$årsakFeilregistrert\""

    companion object {
        const val TYPE = "BehandlingFeilregistrert"

        fun opprettTask(behandlingId: UUID): Task = Task(TYPE, behandlingId.toString())
    }
}
