package no.nav.familie.klage.vurdering

import no.nav.familie.klage.vurdering.domain.Vurdering
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/vurdering"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VurderingController(
    private val vurderingService: VurderingService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun opprettVurdering(@RequestBody vurdering: Vurdering): Ressurs<Vurdering> {
        logger.info("$vurdering")
        return Ressurs.success(vurderingService.opprettVurdering(vurdering))
    }

}
