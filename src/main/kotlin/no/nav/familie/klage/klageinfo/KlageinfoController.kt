package no.nav.familie.klage.klageinfo

import no.nav.familie.klage.klageinfo.domain.Klage
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/klageinfo"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class KlageinfoController(
    val klageinfoService: KlageinfoService
) {
    @GetMapping("{behandlingId}")
    fun hentKlage(@PathVariable behandlingId: UUID): Ressurs<Klage> {
        return Ressurs.success(klageinfoService.hentInfo(behandlingId))
    }
}