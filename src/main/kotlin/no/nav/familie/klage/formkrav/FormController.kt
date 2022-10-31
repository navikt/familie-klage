package no.nav.familie.klage.formkrav

import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.formkrav.dto.FormkravDto
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
@RequestMapping(path = ["/api/formkrav"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FormController(
    private val formService: FormService,
    private val tilgangService: TilgangService
) {

    @GetMapping("vilkar/{behandlingId}")
    fun hentVilkår(@PathVariable behandlingId: UUID): Ressurs<FormkravDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleForBehandling(behandlingId)
        return Ressurs.success(formService.hentFormDto(behandlingId))
    }

    @PostMapping
    fun oppdaterFormkravVilkår(@RequestBody form: FormkravDto): Ressurs<FormkravDto> {
        tilgangService.validerTilgangTilBehandling(form.behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleForBehandling(form.behandlingId)
        return Ressurs.success(formService.oppdaterFormkrav(form))
    }
}
