package no.nav.familie.klage.brev.brevmottaker

import no.nav.familie.klage.brev.dto.BrevmottakereDto
import no.nav.familie.klage.brev.dto.tilDomene
import no.nav.familie.klage.brev.dto.tilDto
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
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        val brevmottakere = brevmottakerService.hentBrevmottakere(behandlingId)
        return Ressurs.success(brevmottakere.tilDto())
    }

    @PutMapping("/{behandlingId}")
    fun erstattBrevmottakere(
        @PathVariable behandlingId: UUID,
        @RequestBody brevmottakereDto: BrevmottakereDto,
    ): Ressurs<BrevmottakereDto> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        brevmottakereDto.valider()
        val nyeBrevmottakere = brevmottakerService.erstattBrevmottakere(behandlingId, brevmottakereDto.tilDomene())
        return Ressurs.success(nyeBrevmottakere.tilDto())
    }

    @PostMapping("/{behandlingId}")
    fun opprettBrevmottaker(
        @PathVariable behandlingId: UUID,
        @RequestBody nyBrevmottakerPersonUtenIdentDto: NyBrevmottakerPersonUtenIdentDto,
    ): Ressurs<BrevmottakereDto> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        nyBrevmottakerPersonUtenIdentDto.valider()
        brevmottakerService.opprettBrevmottaker(
            behandlingId,
            NyBrevmottakerPersonUtenIdentMapper.tilDomene(nyBrevmottakerPersonUtenIdentDto),
        )
        val brevmottakere = brevmottakerService.hentBrevmottakere(behandlingId)
        return Ressurs.success(brevmottakere.tilDto())
    }

    @DeleteMapping("/{behandlingId}/{brevmottakerId}")
    fun slettBrevmottaker(
        @PathVariable behandlingId: UUID,
        @PathVariable brevmottakerId: UUID,
    ): Ressurs<BrevmottakereDto> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.DELETE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        brevmottakerService.slettBrevmottaker(behandlingId, brevmottakerId)
        val brevmottakere = brevmottakerService.hentBrevmottakere(behandlingId)
        return Ressurs.success(brevmottakere.tilDto())
    }
}
