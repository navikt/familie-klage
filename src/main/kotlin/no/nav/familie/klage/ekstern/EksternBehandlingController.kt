package no.nav.familie.klage.ekstern

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.dto.BehandlingDto
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/behandling"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class EksternBehandlingController(val tilgangService: TilgangService, val behandlingService: BehandlingService) {

    @GetMapping("{personIdent}")
    fun hentBehandlinger(
        @PathVariable eksternId: Long,
        @PathVariable fagsystem: Fagsystem
    ): Ressurs<List<BehandlingDto>> {
        /**
         * TODO : Legg til sjekk via tilgangservice
         */
       // val behandlinger = behandlingService.hentBehandlinger(eksternId, fagsystem).map { it }
        return Ressurs.success(behandlinger)
    }
}