package no.nav.familie.klage.vurdering

import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.klage.vurdering.dto.VurderingDto
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
    private val vurderingService: VurderingService,
    private val tilgangService: TilgangService,
) {

    @GetMapping("{behandlingId}")
    fun hentVurdering(@PathVariable behandlingId: UUID): Ressurs<VurderingDto?> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        return Ressurs.success(vurderingService.hentVurderingDto(behandlingId))
    }

    @PostMapping
    fun opprettEllerOppdaterVurdering(@RequestBody vurdering: VurderingDto): Ressurs<VurderingDto> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(vurdering.behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(vurdering.behandlingId)
        return Ressurs.success(vurderingService.opprettEllerOppdaterVurdering(vurdering))
    }
}
