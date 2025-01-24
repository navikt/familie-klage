package no.nav.familie.klage.brev.brevmottaker.ef

import no.nav.familie.klage.brev.dto.BrevmottakereDto
import no.nav.familie.klage.brev.dto.tilDto
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
@RequestMapping(path = ["/api/brevmottaker/ef"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class EfBrevmottakerController(
    private val efBrevmottakerService: EfBrevmottakerService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("/{behandlingId}")
    fun hentBrevmottakere(
        @PathVariable behandlingId: UUID,
    ): Ressurs<BrevmottakereDto> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        return Ressurs.success(efBrevmottakerService.hentBrevmottakere(behandlingId).tilDto())
    }

    @PostMapping("/{behandlingId}")
    fun oppdaterBrevmottakere(
        @PathVariable behandlingId: UUID,
        @RequestBody mottakere: BrevmottakereDto,
    ): Ressurs<BrevmottakereDto> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        efBrevmottakerService.settBrevmottakere(behandlingId, mottakere)
        return Ressurs.success(mottakere)
    }
}
