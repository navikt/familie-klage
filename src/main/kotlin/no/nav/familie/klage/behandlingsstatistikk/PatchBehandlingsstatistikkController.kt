package no.nav.familie.klage.behandlingsstatistikk

import no.nav.familie.klage.behandling.BehandlingRepository
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/patch-statistikk")
@Unprotected
class PatchBehandlingsstatistikkController(
    private val behandlingRepository: BehandlingRepository,
    private val taskRepository: TaskRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/")
    fun patchTidligereBehandlingResultater(@RequestBody liveRun: LiveRun) {
        val behandlinger = behandlingRepository.findAll()
        behandlinger.forEach { behandling ->
            if (behandling.resultat == BehandlingResultat.IKKE_MEDHOLD) {
                logger.info("Oppretter task for behandlingsstatistikk med hendelse SENDT_TIL_KA for behandlingId:${behandling.id} som har behandlingsresultat ${behandling.resultat.name}. Dryrun : $dryrun")
                if (liveRun.skalOpprette) {
                    val taskSomSkalOpprettes = BehandlingsstatistikkTask.opprettSendtTilKATask(
                        behandlingId = behandling.id,
                        hendelseTidspunkt = behandling.sporbar.endret.endretTid,
                    )
                    taskRepository.save(taskSomSkalOpprettes)
                }
            }
        }
    }
}

data class LiveRun(val skalOpprette: Boolean)
