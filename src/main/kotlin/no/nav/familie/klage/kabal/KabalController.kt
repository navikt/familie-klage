package no.nav.familie.klage.kabal
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/kabal"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class KabalController(

){

}