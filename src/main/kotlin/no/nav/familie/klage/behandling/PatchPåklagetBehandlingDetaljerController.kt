package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.PåklagetVedtakstype
import no.nav.familie.klage.behandling.domain.tilPåklagetVedtakDetaljer
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.integrasjoner.FagsystemVedtakService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/patch-paklaget-behandling"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class PatchPåklagetBehandlingDetaljerController(
    private val behandlingRepository: BehandlingRepository,
    private val fagsystemVedtakService: FagsystemVedtakService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("{dryRun}")
    fun ferdigstillBehandling(@PathVariable dryRun: Boolean) {
        logger.info("Dryrun = $dryRun")
        val behandlingerSomManglerData = behandlingRepository.findAll()
            .filter { (it.påklagetVedtak.eksternFagsystemBehandlingId != null || it.påklagetVedtak.påklagetVedtakstype == PåklagetVedtakstype.VEDTAK) && it.påklagetVedtak.påklagetVedtakDetaljer == null }

        behandlingerSomManglerData.forEach {
            val påklagetVedtak = it.påklagetVedtak
            feilHvis(påklagetVedtak.eksternFagsystemBehandlingId != null && påklagetVedtak.påklagetVedtakstype != PåklagetVedtakstype.VEDTAK) {
                "Behandling=${it.id} har eksternFagsystemBehandlingId men ikke riktig påklagetVedtakstype (${påklagetVedtak.påklagetVedtakstype})"
            }

            feilHvis(påklagetVedtak.påklagetVedtakstype == PåklagetVedtakstype.VEDTAK && påklagetVedtak.eksternFagsystemBehandlingId == null) {
                "Behandling=${it.id} har påklagetVedtakstype men ikke mangler eksternFagsystemBehandlingId"
            }
        }

        behandlingerSomManglerData.forEach {
            logger.info("Behandling=${it.id} mangler påklagetVedtakDetaljer")
        }

        val behandlingerSomSkalOppdateres = behandlingerSomManglerData
            .map { behandling ->
                val påklagetBehandlingId = behandling.påklagetVedtak.eksternFagsystemBehandlingId ?: error("Mangler behandlingId")
                val påklagetVedtakDetaljer = (fagsystemVedtakService.hentFagsystemVedtak(behandling.id)
                    .singleOrNull { it.eksternBehandlingId == påklagetBehandlingId }
                    ?.tilPåklagetVedtakDetaljer()
                    ?: error("Finner ikke vedtak til behandling=${behandling.id}"))
                behandling.id to påklagetVedtakDetaljer
            }

        if (!dryRun) {
            logger.info("Oppdaterer ${behandlingerSomSkalOppdateres.size} behandlinger")
            behandlingerSomSkalOppdateres.forEach {
                behandlingRepository.oppdaterPåklagetVedtakDetaljer(it.second, it.first)
            }
        }
    }

}