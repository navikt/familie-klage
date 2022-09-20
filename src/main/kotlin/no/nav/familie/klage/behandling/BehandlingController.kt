package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.dto.BehandlingDto
import no.nav.familie.klage.distribusjon.FerdigstillBehandlingService
import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.klage.kabal.dto.KlageresultatDto
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
@RequestMapping(path = ["/api/behandling"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BehandlingController(
    private val behandlingService: BehandlingService,
    private val tilgangService: TilgangService,
    private val ferdigstillBehandlingService: FerdigstillBehandlingService
) {

    @GetMapping("{behandlingId}")
    fun hentBehandling(@PathVariable behandlingId: UUID): Ressurs<BehandlingDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(behandlingService.hentBehandlingDto(behandlingId))
    }

    @PostMapping("{behandlingId}/ferdigstill")
    fun ferdigstillBehandling(@PathVariable behandlingId: UUID) {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolle()
        ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId)
    }

    @GetMapping("{behandlingId}/resultat")
    fun hentKlageresultat(@PathVariable behandlingId: UUID): Ressurs<List<KlageresultatDto>> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(behandlingService.hentKlageresultatDto(behandlingId))
    }
}
