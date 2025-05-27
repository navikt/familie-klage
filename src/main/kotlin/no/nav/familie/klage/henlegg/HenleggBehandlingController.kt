package no.nav.familie.klage.henlegg

import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.brevmottaker.dto.tilDomene
import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
class HenleggBehandlingController(
    private val tilgangService: TilgangService,
    private val henleggBehandlingService: HenleggBehandlingService,
    private val henleggBehandlingValidator: HenleggBehandlingValidator,
    private val brevService: BrevService,
) {
    private val logger: Logger = LoggerFactory.getLogger(HenleggBehandlingController::class.java)

    @PostMapping("{behandlingId}/henlegg")
    fun henleggBehandling(
        @PathVariable behandlingId: UUID,
        @RequestBody henleggBehandlingDto: HenleggBehandlingDto,
    ): Ressurs<Unit> {
        logger.info("Henlegger behandling=$behandlingId")
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        henleggBehandlingValidator.validerHenleggBehandlingDto(behandlingId, henleggBehandlingDto)
        if (henleggBehandlingDto.skalSendeHenleggelsesbrev) {
            val nyeBrevmottakere = henleggBehandlingDto.nyeBrevmottakere.map { it.tilDomene() }
            brevService.lagHenleggelsesbrevOgOpprettJournalføringstask(behandlingId, nyeBrevmottakere)
        }
        return Ressurs.success(henleggBehandlingService.henleggBehandling(behandlingId, henleggBehandlingDto.årsak))
    }

    @GetMapping("/{behandlingId}/henlegg/brev/forhandsvisning")
    fun genererHenleggBrev(
        @PathVariable behandlingId: UUID,
    ): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        return Ressurs.success(brevService.genererHenleggelsesbrev(behandlingId))
    }
}
