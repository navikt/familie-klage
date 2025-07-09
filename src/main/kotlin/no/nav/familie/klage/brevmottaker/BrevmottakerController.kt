package no.nav.familie.klage.brevmottaker

import no.nav.familie.klage.brevmottaker.dto.BrevmottakereDto
import no.nav.familie.klage.brevmottaker.dto.NyBrevmottakerDto
import no.nav.familie.klage.brevmottaker.dto.SlettbarBrevmottakerDto
import no.nav.familie.klage.brevmottaker.dto.tilBrevmottakere
import no.nav.familie.klage.brevmottaker.dto.tilBrevmottakereDto
import no.nav.familie.klage.brevmottaker.dto.tilNyBrevmottaker
import no.nav.familie.klage.brevmottaker.dto.tilSlettbarBrevmottaker
import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/brevmottaker"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BrevmottakerController(
    private val brevmottakerService: BrevmottakerService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("/{behandlingId}")
    fun hentBrevmottakere(
        @PathVariable behandlingId: UUID,
    ): Ressurs<BrevmottakereDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        val brevmottakere = brevmottakerService.hentBrevmottakere(behandlingId)
        return Ressurs.success(brevmottakere.tilBrevmottakereDto())
    }

    @PutMapping("/{behandlingId}")
    fun erstattBrevmottakere(
        @PathVariable behandlingId: UUID,
        @RequestBody brevmottakereDto: BrevmottakereDto,
    ): Ressurs<BrevmottakereDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        brevmottakereDto.valider()
        val nyeBrevmottakere = brevmottakerService.erstattBrevmottakere(behandlingId, brevmottakereDto.tilBrevmottakere())
        return Ressurs.success(nyeBrevmottakere.tilBrevmottakereDto())
    }

    @PostMapping("/{behandlingId}")
    fun opprettBrevmottaker(
        @PathVariable behandlingId: UUID,
        @RequestBody nyBrevmottakerDto: NyBrevmottakerDto,
    ): Ressurs<BrevmottakereDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        nyBrevmottakerDto.valider()
        brevmottakerService.opprettBrevmottaker(behandlingId, nyBrevmottakerDto.tilNyBrevmottaker())
        val brevmottakere = brevmottakerService.hentBrevmottakere(behandlingId)
        return Ressurs.success(brevmottakere.tilBrevmottakereDto())
    }

    @DeleteMapping("/{behandlingId}")
    fun slettBrevmottaker(
        @PathVariable behandlingId: UUID,
        @RequestBody slettbarBrevmottakerDto: SlettbarBrevmottakerDto,
    ): Ressurs<BrevmottakereDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.DELETE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        brevmottakerService.slettBrevmottaker(behandlingId, slettbarBrevmottakerDto.tilSlettbarBrevmottaker())
        val brevmottakere = brevmottakerService.hentBrevmottakere(behandlingId)
        return Ressurs.success(brevmottakere.tilBrevmottakereDto())
    }

    @GetMapping("/initielle/{behandlingId}")
    fun utledInitielleBrevmottakere(
        @PathVariable behandlingId: UUID,
    ): Ressurs<BrevmottakereDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        val brevmottakere = brevmottakerService.utledInitielleBrevmottakere(behandlingId)
        return Ressurs.success(brevmottakere.tilBrevmottakereDto())
    }
}
