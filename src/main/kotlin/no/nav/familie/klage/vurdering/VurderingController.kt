package no.nav.familie.klage.vurdering

import no.nav.familie.klage.vurdering.domain.Vedtak
import no.nav.familie.klage.vurdering.domain.Vurdering
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
@RequestMapping(path = ["/api/vurdering"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VurderingController(
    private val vurderingService: VurderingService
) {

    @GetMapping("{behandlingId}")
    fun hentVurdering(@PathVariable behandlingId: UUID): Ressurs<Vurdering> {
        return Ressurs.success(vurderingService.hentVurdering(behandlingId))
    }

    @PostMapping
    fun opprettVurdering(@RequestBody vurdering: Vurdering): Ressurs<Vurdering> {
        return Ressurs.success(vurderingService.opprettVurdering(vurdering))
    }

    @GetMapping("{behandlingId}/vedtak")
    fun hentVedtak(@PathVariable behandlingId: UUID): Ressurs<Vedtak?> {
        return Ressurs.success(vurderingService.hentVedtak(behandlingId))
    }

}
