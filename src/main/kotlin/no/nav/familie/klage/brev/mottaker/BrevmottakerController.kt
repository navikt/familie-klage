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
    ): Ressurs<List<Brevmottaker>> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        return Ressurs.success(brevmottakerService.hentBrevmottakere(behandlingId))
    }

    @PostMapping("/{behandlingId}")
    fun oppdaterBrevmottakere(
        @PathVariable behandlingId: UUID,
        @RequestBody brevmottakerDto: BrevmottakerDto,
    ): Ressurs<List<BrevmottakerDto>> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        brevmottakerDto.valider()
        val brevmottaker = brevmottakerDto.mapTilBrevmottaker()
        val oppdaterteBrevmottakereDto = brevmottakerService
            .oppdaterBrevmottakere(behandlingId, brevmottaker)
            .map { it.mapTilBrevMottakerDto() }
        return Ressurs.success(oppdaterteBrevmottakereDto)
    }
}
