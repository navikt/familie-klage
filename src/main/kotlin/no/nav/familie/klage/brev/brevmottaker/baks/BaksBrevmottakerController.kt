package no.nav.familie.klage.brev.brevmottaker.baks

import no.nav.familie.klage.brev.dto.BrevmottakereDto
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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/brevmottaker/baks"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BaksBrevmottakerController(
    private val baksBrevmottakerService: BaksBrevmottakerService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("/{behandlingId}")
    fun hentBrevmottakere(
        @PathVariable behandlingId: UUID,
    ): Ressurs<BrevmottakereDto> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        val brevmottakere = baksBrevmottakerService.hentBrevmottakere(behandlingId)
        return Ressurs.success(brevmottakere.tilDto())
    }

    @PostMapping("/{behandlingId}")
    fun opprettBrevmottakere(
        @PathVariable behandlingId: UUID,
        @RequestBody nyBrevmottakerPersonUtenIdentDto: NyBrevmottakerPersonUtenIdentDto,
    ): Ressurs<BrevmottakereDto> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        nyBrevmottakerPersonUtenIdentDto.valider()
        baksBrevmottakerService.opprettBrevmottaker(
            behandlingId,
            NyBrevmottakerPersonUtenIdentMapper.tilDomene(nyBrevmottakerPersonUtenIdentDto),
        )
        val brevmottakere = baksBrevmottakerService.hentBrevmottakere(behandlingId)
        return Ressurs.success(brevmottakere.tilDto())
    }

    @DeleteMapping("/{behandlingId}/{brevmottakerId}")
    fun slettBrevmottakere(
        @PathVariable behandlingId: UUID,
        @PathVariable brevmottakerId: UUID,
    ): Ressurs<BrevmottakereDto> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.DELETE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        baksBrevmottakerService.slettBrevmottaker(behandlingId, brevmottakerId)
        val brevmottakere = baksBrevmottakerService.hentBrevmottakere(behandlingId)
        return Ressurs.success(brevmottakere.tilDto())
    }
}
