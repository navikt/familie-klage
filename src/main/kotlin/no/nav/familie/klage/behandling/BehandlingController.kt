package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.dto.BehandlingDto
import no.nav.familie.klage.behandling.dto.HenlagtDto
import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.klage.integrasjoner.FagsystemVedtakService
import no.nav.familie.klage.oppgave.OppgaveService
import no.nav.familie.klage.oppgave.TilordnetRessursService
import no.nav.familie.klage.oppgave.dto.SaksbehandlerDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.KanOppretteRevurderingResponse
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
    private val fagsystemVedtakService: FagsystemVedtakService,
    private val opprettRevurderingService: OpprettRevurderingService,
    private val tilordnetRessursService: TilordnetRessursService,
    private val oppgaveService: OppgaveService,
) {

    @GetMapping("{behandlingId}")
    fun hentBehandling(@PathVariable behandlingId: UUID): Ressurs<BehandlingDto> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        return Ressurs.success(behandlingService.hentBehandlingDto(behandlingId))
    }

    @PostMapping("{behandlingId}/ferdigstill")
    fun ferdigstillBehandling(@PathVariable behandlingId: UUID): Ressurs<Unit> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        return Ressurs.success(ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId))
    }

    @PostMapping("{behandlingId}/henlegg")
    fun henleggBehandling(@PathVariable behandlingId: UUID, @RequestBody henlegg: HenlagtDto): Ressurs<Unit> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        return Ressurs.success(behandlingService.henleggBehandling(behandlingId, henlegg))
    }

    @GetMapping("{behandlingId}/fagsystem-vedtak")
    fun hentFagsystemVedtak(@PathVariable behandlingId: UUID): Ressurs<List<FagsystemVedtak>> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        return Ressurs.success(fagsystemVedtakService.hentFagsystemVedtak(behandlingId))
    }

    @GetMapping("{behandlingId}/kan-opprette-revurdering")
    fun kanOppretteRevurdering(@PathVariable behandlingId: UUID): Ressurs<KanOppretteRevurderingResponse> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        return Ressurs.success(opprettRevurderingService.kanOppretteRevurdering(behandlingId))
    }

    @GetMapping("{behandlingId}/ansvarlig-saksbehandler")
    fun hentAnsvarligSaksbehandlerForBehandling(@PathVariable behandlingId: UUID): Ressurs<SaksbehandlerDto> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(
            tilordnetRessursService.hentAnsvarligSaksbehandlerForBehandlingsId(behandlingId),
        )
    }
}
