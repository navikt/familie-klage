package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Behandling
import no.nav.familie.klage.behandling.dto.BehandlingDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/behandling"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BehandlingController(
        private val behandlingService: BehandlingService
) {

    @GetMapping("{behandlingId}")
    fun hentBehandling(@PathVariable behandlingId: UUID): Ressurs<BehandlingDto> {
        return Ressurs.success(behandlingService.hentBehandling(behandlingId))
    }

    @PostMapping
    fun opprettBehandling(): Ressurs<Behandling> {
        return Ressurs.success(behandlingService.opprettBehandling())
    }


    @PostMapping("/ferdigstill/{behandlingId}")
    fun ferdigstillBrev(@PathVariable behandlingId: UUID){
        behandlingService.ferdigstillBrev(behandlingId)
    }

}
