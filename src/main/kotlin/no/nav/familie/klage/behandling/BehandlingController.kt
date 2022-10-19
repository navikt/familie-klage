package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.dto.BehandlingDto
import no.nav.familie.klage.behandling.dto.HenlagtDto
import no.nav.familie.klage.behandling.dto.PåklagetVedtakDto
import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.klage.integrasjoner.FagsystemVedtakService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
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
@RequestMapping(path = ["/api/behandling"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BehandlingController(
    private val behandlingService: BehandlingService,
    private val tilgangService: TilgangService,
    private val ferdigstillBehandlingService: FerdigstillBehandlingService,
    private val fagsystemVedtakService: FagsystemVedtakService
) {

    @GetMapping("{behandlingId}")
    fun hentBehandling(@PathVariable behandlingId: UUID): Ressurs<BehandlingDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleForBehandling(behandlingId)
        return Ressurs.success(behandlingService.hentBehandlingDto(behandlingId))
    }

    @PostMapping("{behandlingId}/ferdigstill")
    fun ferdigstillBehandling(@PathVariable behandlingId: UUID): Ressurs<Unit> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolleForBehandling(behandlingId)
        return Ressurs.success(ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId))
    }

    @PostMapping("{behandlingId}/henlegg")
    fun henleggBehandling(@PathVariable behandlingId: UUID, @RequestBody henlegg: HenlagtDto): Ressurs<Unit> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleForBehandling(behandlingId)
        return Ressurs.success(behandlingService.henleggBehandling(behandlingId, henlegg))
    }

    @PostMapping("{behandlingId}/paklaget-vedtak")
    fun settPåklagetVedtak(@PathVariable behandlingId: UUID, @RequestBody påklagetVedtakDto: PåklagetVedtakDto): Ressurs<Unit> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleForBehandling(behandlingId)
        return Ressurs.success(behandlingService.oppdaterPåklagetVedtak(behandlingId, påklagetVedtakDto))
    }

    @GetMapping("{behandlingId}/paklagbare-vedtak")
    fun hentPåklagbareVedtak(@PathVariable behandlingId: UUID): Ressurs<List<FagsystemVedtak>> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleForBehandling(behandlingId)
        return Ressurs.success(fagsystemVedtakService.hentPåklagebareVedtak(behandlingId))
    }
}
