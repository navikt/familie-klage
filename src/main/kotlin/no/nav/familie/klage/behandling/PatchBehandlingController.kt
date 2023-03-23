package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/patch-henlagte-behandlinger"])
@Unprotected
@Validated
class PatchBehandlingController(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingshistorikkService: BehandlingshistorikkService

) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/")
    fun patchHenlagteBehandlinger(@RequestBody liveRun: LiveRun): Ressurs<String> {

        val henlagteBehandlinger = behandlingRepository.findAll().filter { it.resultat == BehandlingResultat.HENLAGT }

        henlagteBehandlinger.forEach { behandling ->
            val tidspunktForHenleggelse = behandlingshistorikkService.hentBehandlingshistorikk(behandling.id).first().endretTid

            if (liveRun.skalPatche) {
                behandlingRepository.update(behandling.copy(vedtakDato = tidspunktForHenleggelse))
                logger.info("Behandling med id ${behandling.id} og resultat ${behandling.resultat} oppdaterer sin vedtaksdato til $tidspunktForHenleggelse")
            }
        }

        return if (liveRun.skalPatche) {
            Ressurs.success("Henlagte behandlinger ble oppdatert med nytt vedtakstidspunkt")
        } else {
            Ressurs.success("Henlagte behandlinger ble ikke oppdatert")
        }
    }
}

data class LiveRun(val skalPatche: Boolean)
