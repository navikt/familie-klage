package no.nav.familie.klage.fagsak

import no.nav.familie.klage.fagsak.dto.FagsakDto
import no.nav.familie.klage.fagsak.dto.tilDto
import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/fagsak"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakController(
    private val fagsakService: FagsakService,
    private val tilgangService: TilgangService
) {

    @GetMapping("{fagsakId}")
    fun hentFagsak(@PathVariable fagsakId: UUID): Ressurs<FagsakDto> {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(fagsakService.hentFagsak(fagsakId).tilDto())
    }
}
