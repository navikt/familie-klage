package no.nav.familie.klage.brev.mottaker

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
@RequestMapping(path = ["/api/brevmottaker-med-adresse"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BrevmottakerMedAdresseController(
    private val brevmottakerMedAdresseService: BrevmottakerMedAdresseService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("/{behandlingId}/mottakere")
    fun hentBrevmottakere(
        @PathVariable behandlingId: UUID,
    ): Ressurs<List<BrevmottakerMedAdresse>> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        return Ressurs.success(brevmottakerMedAdresseService.hentBrevmottakere(behandlingId))
    }

    @PostMapping("/{behandlingId}/mottakere")
    fun oppdaterBrevmottakere(
        @PathVariable behandlingId: UUID,
        @RequestBody mottakere: List<BrevmottakerMedAdresse>,
    ): Ressurs<List<BrevmottakerMedAdresse>> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        return Ressurs.success(brevmottakerMedAdresseService.oppdaterBrevmottakere(behandlingId, mottakere))
    }
}
