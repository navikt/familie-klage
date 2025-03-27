package no.nav.familie.klage.henlegg

import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.domain.StegType.BEHANDLING_FERDIGSTILT
import no.nav.familie.klage.behandling.domain.erLåstForVidereBehandling
import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.klage.behandlingsstatistikk.BehandlingsstatistikkTask
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.felles.domain.SporbarUtils
import no.nav.familie.klage.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.klage.oppgave.OppgaveTaskService
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus.FERDIGSTILT
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class HenleggBehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingService: BehandlingService,
    private val behandlinghistorikkService: BehandlingshistorikkService,
    private val oppgaveTaskService: OppgaveTaskService,
    private val taskService: TaskService,
    private val fagsakService: FagsakService,
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun henleggBehandling(behandlingId: UUID, henlagt: HenlagtDto) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)

        validerKanHenleggeBehandling(behandling)

        val henlagtBehandling = behandling.copy(
            henlagtÅrsak = henlagt.årsak,
            resultat = BehandlingResultat.HENLAGT,
            steg = BEHANDLING_FERDIGSTILT,
            status = FERDIGSTILT,
            vedtakDato = SporbarUtils.now(),
        )

        behandlinghistorikkService.opprettBehandlingshistorikk(
            behandlingId = behandlingId,
            steg = BEHANDLING_FERDIGSTILT,
        )
        oppgaveTaskService.lagFerdigstillOppgaveForBehandlingTask(
            behandlingId = behandling.id,
            eksternFagsakId = fagsak.eksternId,
            fagsystem = fagsak.fagsystem
        )
        behandlingRepository.update(henlagtBehandling)
        taskService.save(
            taskService.save(
                BehandlingsstatistikkTask.opprettFerdigTask(
                    behandlingId = behandlingId,
                    eksternFagsakId = fagsak.eksternId,
                    fagsak.fagsystem
                )
            )
        )
    }

    private fun validerKanHenleggeBehandling(behandling: Behandling) {
        brukerfeilHvis(behandling.status.erLåstForVidereBehandling()) {
            "Kan ikke henlegge behandling med status ${behandling.status}"
        }
    }
}
