package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/intern/migrer-påklaget-vedtak-detaljer"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class MigrerPåklagetVedtakDetaljerController(
    private val behandlingRepository: BehandlingRepository
) {

    @PutMapping("")
    fun hentOgLagreOppdaterteBehandlinger(): List<Behandling> {
        val oppdaterteBehandlinger = hentOppdaterteBehandlinger()
        return behandlingRepository.updateAll(oppdaterteBehandlinger.toList())
    }

    @GetMapping("/dry")
    fun hentOppdaterteBehandlinger(): Iterable<Behandling> {
        return behandlingRepository.findAll()
    }
}
