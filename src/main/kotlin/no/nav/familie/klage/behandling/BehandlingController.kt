package no.nav.familie.klage.behandling

import no.nav.familie.klage.behandling.domain.Klagebehandlingsresultat
import no.nav.familie.klage.behandling.dto.BehandlingDto
import no.nav.familie.klage.behandling.dto.OppdaterBehandlendeEnhetDto
import no.nav.familie.klage.behandling.dto.OppgaveDto
import no.nav.familie.klage.behandling.dto.SettPåVentRequest
import no.nav.familie.klage.behandling.enhet.BehandlendeEnhetService
import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.klage.integrasjoner.FagsystemVedtakService
import no.nav.familie.klage.oppgave.OppgaveService
import no.nav.familie.klage.oppgave.OppgaveUtil.ENHET_NR_EGEN_ANSATT
import no.nav.familie.klage.oppgave.OppgaveUtil.ENHET_NR_NAY
import no.nav.familie.klage.oppgave.TilordnetRessursService
import no.nav.familie.klage.oppgave.dto.SaksbehandlerDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.KanOppretteRevurderingResponse
import no.nav.familie.kontrakter.felles.oppgave.MappeDto
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
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
    private val behandlingPåVentService: BehandlingPåVentService,
    private val oppgaveService: OppgaveService,
    private val behandlendeEnhetService: BehandlendeEnhetService,
) {
    @GetMapping("{behandlingId}")
    fun hentBehandling(
        @PathVariable behandlingId: UUID,
    ): Ressurs<BehandlingDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        return Ressurs.success(behandlingService.hentBehandlingDto(behandlingId))
    }

    @PostMapping("{behandlingId}/ferdigstill")
    fun ferdigstillBehandling(
        @PathVariable behandlingId: UUID,
    ): Ressurs<Unit> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        return Ressurs.success(ferdigstillBehandlingService.ferdigstillKlagebehandling(behandlingId))
    }

    @GetMapping("{behandlingId}/fagsystem-vedtak")
    fun hentFagsystemVedtak(
        @PathVariable behandlingId: UUID,
    ): Ressurs<List<FagsystemVedtak>> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        return Ressurs.success(fagsystemVedtakService.hentFagsystemVedtak(behandlingId))
    }

    @GetMapping("{behandlingId}/kan-opprette-revurdering")
    fun kanOppretteRevurdering(
        @PathVariable behandlingId: UUID,
    ): Ressurs<KanOppretteRevurderingResponse> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        return Ressurs.success(opprettRevurderingService.kanOppretteRevurdering(behandlingId))
    }

    @GetMapping("{behandlingId}/ansvarlig-saksbehandler")
    fun hentAnsvarligSaksbehandlerForBehandling(
        @PathVariable behandlingId: UUID,
    ): Ressurs<SaksbehandlerDto> {
        tilgangService.validerTilgangTilBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.ACCESS,
        )
        return Ressurs.success(
            tilordnetRessursService.hentAnsvarligSaksbehandlerForBehandlingsId(behandlingId = behandlingId),
        )
    }

    @GetMapping("{behandlingId}/oppgave")
    fun hentOppgave(
        @PathVariable behandlingId: UUID,
    ): Ressurs<OppgaveDto?> {
        tilgangService.validerTilgangTilBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.ACCESS,
        )

        return Ressurs.success(
            data = tilordnetRessursService.hentOppgave(behandlingId = behandlingId),
        )
    }

    @GetMapping("/mapper")
    fun hentMapper(): Ressurs<List<MappeDto>> {
        val enheter = mutableListOf(ENHET_NR_NAY)
        if (tilgangService.harEgenAnsattRolle()) {
            enheter += ENHET_NR_EGEN_ANSATT
        }
        return Ressurs.success(oppgaveService.finnMapper(enheter = enheter))
    }

    @PostMapping("{behandlingId}/vent")
    fun settPåVent(
        @PathVariable behandlingId: UUID,
        @RequestBody settPåVentRequest: SettPåVentRequest,
    ): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.UPDATE,
        )
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId = behandlingId)
        behandlingPåVentService.settPåVent(behandlingId = behandlingId, settPåVentRequest = settPåVentRequest)

        return Ressurs.success(data = behandlingId)
    }

    @PostMapping("{behandlingId}/ta-av-vent")
    fun taAvVent(
        @PathVariable behandlingId: UUID,
    ): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.UPDATE,
        )
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId = behandlingId)
        behandlingPåVentService.taAvVent(behandlingId = behandlingId)

        return Ressurs.success(data = behandlingId)
    }

    @PutMapping("{behandlingId}/oppdater-behandlende-enhet")
    fun oppdaterBehandlendeEnhet(
        @PathVariable behandlingId: UUID,
        @RequestBody oppdaterBehandlendeEnhetDto: OppdaterBehandlendeEnhetDto,
    ): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(
            behandlingId = behandlingId,
            event = AuditLoggerEvent.UPDATE,
        )
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId = behandlingId)

        behandlendeEnhetService.oppdaterBehandlendeEnhetPåBehandling(
            behandlingId = behandlingId,
            enhetsnummer = oppdaterBehandlendeEnhetDto.enhetsnummer,
            begrunnelse = oppdaterBehandlendeEnhetDto.begrunnelse,
        )

        return Ressurs.success(data = behandlingId)
    }

    @GetMapping("{behandlingId}/hent-klager-ikke-medhold-formkrav-avvist")
    fun hentKlager(
        @PathVariable behandlingId: UUID,
    ): Ressurs<List<Klagebehandlingsresultat>> = Ressurs.success(behandlingService.hentKlagerIkkeMedholdFormkravAvvist(behandlingId))
}
