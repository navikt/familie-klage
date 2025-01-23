package no.nav.familie.klage.brev.brevmottaker.baks

import no.nav.familie.klage.brev.domain.Brevmottakere
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
class BrevmottakerController(
    private val brevmottakerService: BrevmottakerService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("/{behandlingId}")
    fun hentBrevmottakere(
        @PathVariable behandlingId: UUID,
    ): Ressurs<Brevmottakere> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        val brevmottakere = brevmottakerService.hentBrevmottakere(behandlingId)
        return Ressurs.success(brevmottakere)
    }

    @PostMapping("/{behandlingId}")
    fun opprettBrevmottakere(
        @PathVariable behandlingId: UUID,
        @RequestBody nyBrevmottakerPersonUtenIdentDto: NyBrevmottakerPersonUtenIdentDto,
    ): Ressurs<Brevmottakere> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        nyBrevmottakerPersonUtenIdentDto.valider()
        brevmottakerService.opprettBrevmottaker(
            behandlingId,
            NyBrevmottakerPersonUtenIdentMapper.tilDomene(nyBrevmottakerPersonUtenIdentDto),
        )
        val brevmottakere = brevmottakerService.hentBrevmottakere(behandlingId)
        return Ressurs.success(brevmottakere)
    }

    @DeleteMapping("/{behandlingId}/{brevmottakerId}")
    fun slettBrevmottakere(
        @PathVariable behandlingId: UUID,
        @PathVariable brevmottakerId: UUID,
    ): Ressurs<Brevmottakere> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.DELETE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        brevmottakerService.slettBrevmottaker(behandlingId, brevmottakerId)
        val brevmottakere = brevmottakerService.hentBrevmottakere(behandlingId)
        return Ressurs.success(brevmottakere)
    }
}
