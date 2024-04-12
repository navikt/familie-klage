package no.nav.familie.klage.amelding

import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/inntekt")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class InntektController(
    private val tilgangService: TilgangService,
    private val inntektService: InntektService,
) {
    @GetMapping("fagsak/{fagsakId}/generer-url")
    fun genererAInntektUrl(@PathVariable("fagsakId") fagsakId: UUID): Ressurs<String> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForFagsak(fagsakId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForFagsak(fagsakId)
        return success(inntektService.genererAInntektUrlPåFagsak(fagsakId))
    }
}
