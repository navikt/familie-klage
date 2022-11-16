package no.nav.familie.klage.behandlingsstatistikk

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/patch-statistikk")
@Unprotected
class PatchStatistikkController(
    private val behandlingService: BehandlingService,
    private val taskRepository: TaskRepository
) {
    private val triggerTid = LocalDate.of(2022, 11, 11)
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/")
    fun patchTidligereBehandlingResultater() {
        val tasker = hentOgFiltrerTaskerPåTriggertid()
        tasker.forEach { task ->
            val behandlingId = task.getProperty("behandlingId")
            val behandling = behandlingService.hentBehandling(UUID.fromString(behandlingId))
            if (behandling.resultat == BehandlingResultat.IKKE_MEDHOLD) {
                if (sendtTilKATaskErOpprettet(behandlingId, tasker)) {
                    logger.info("Det finnes allerede en task med behandlingId:$behandlingId som har status FERDIG av typen SENDT_TIL_KA")
                    return@forEach
                }
                logger.info("Oppretter task for behandlingsstatistikk med hendelse SENDT_TIL_KA for behandlingId:${behandling.id} som har behandlingsresultat IKKE_MEDHOLD")
                BehandlingsstatistikkTask.opprettSendtTilKATask(behandlingId = behandling.id, hendelseTidspunkt = task.opprettetTid)
            }
        }
    }

    private fun sendtTilKATaskErOpprettet(behandlingId: String, tasker: List<Task>): Boolean {
        tasker.find { task ->
            task.getProperty("behandlingId") == behandlingId && task.getProperty("hendelse") == BehandlingsstatistikkHendelse.SENDT_TIL_KA.name
        }?.let {
            return true
        }
        return false
    }

    private fun hentOgFiltrerTaskerPåTriggertid(): List<Task> {
        return taskRepository.findByStatusInAndType(
            listOf(Status.FERDIG),
            "behandlingsstatistikkKlageTask",
            Pageable.unpaged()
        ).filter { task ->
            task.triggerTid.toLocalDate().isBefore(triggerTid)
        }
    }

    private fun Task.getProperty(property: String): String {
        return this.metadataWrapper.properties.get(property).toString()
    }
}