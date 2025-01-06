package no.nav.familie.klage.brev.ef

import no.nav.familie.klage.brev.ef.dto.BrevmottakereDto
import no.nav.familie.klage.brev.ef.dto.tilDto
import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
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
@RequestMapping(path = ["/api/ef/brev"])
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
        return Ressurs.success(brevService.hentBrevPdf(behandlingId))
    }

    @PostMapping("/{behandlingId}")
    fun lagEllerOppdaterBrev(
        @PathVariable behandlingId: UUID,
    ): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        return Ressurs.success(brevService.lagBrev(behandlingId))
    }

    @GetMapping("/{behandlingId}/mottakere")
    fun hentBrevmottakere(
        @PathVariable behandlingId: UUID,
    ): Ressurs<BrevmottakereDto> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        return Ressurs.success(brevService.hentBrevmottakere(behandlingId).tilDto())
    }

    @PostMapping("/{behandlingId}/mottakere")
    fun oppdaterBrevmottakere(
        @PathVariable behandlingId: UUID,
        @RequestBody mottakere: BrevmottakereDto,
    ): Ressurs<BrevmottakereDto> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        brevService.settBrevmottakere(behandlingId, mottakere)
        return Ressurs.success(mottakere)
    }
}
