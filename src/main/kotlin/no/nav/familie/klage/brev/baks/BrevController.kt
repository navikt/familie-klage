package no.nav.familie.klage.brev.baks

import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
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
@RequestMapping(path = ["/api/baks/brev"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BrevController(
    private val brevService: BrevService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("/{behandlingId}/pdf")
    fun hentBrevPdf(
        @PathVariable behandlingId: UUID,
    ): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        return Ressurs.success(brevService.hentBrev(behandlingId).pdfSomBytes())
    }

    @PostMapping("/{behandlingId}")
    fun lagEllerOppdaterBrev(
        @PathVariable behandlingId: UUID,
    ): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        return Ressurs.success(brevService.lagBrev(behandlingId))
    }
}
