package no.nav.familie.klage.behandlingshistorikk

import no.nav.familie.klage.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/behandlingshistorikk"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BehandlingshistorikkController(
    private val behandlingshistorikkService: BehandlingshistorikkService
) {

    @GetMapping("{behandlingId}")
    fun hentBehandlingshistorikker(@PathVariable behandlingId: UUID): Ressurs<List<Behandlingshistorikk>> {
        return Ressurs.success(behandlingshistorikkService.hentBehandlingshistorikker(behandlingId))
    }

    @PostMapping
    fun opprettBehandlingshistorikk(@RequestBody behandlingshistorikk: Behandlingshistorikk): Ressurs<Behandlingshistorikk> {
        return Ressurs.success(behandlingshistorikkService.opprettBehandlingshistorikk(behandlingshistorikk))
    }

}
