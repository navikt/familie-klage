package no.nav.familie.klage.formkrav

import no.nav.familie.klage.formkrav.domain.Form
import no.nav.familie.klage.formkrav.dto.FormDto
import no.nav.familie.klage.formkrav.dto.tilDto
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
    private val formService: FormService
) {

    @GetMapping("{behandlingId}")
    fun hentKlage(@PathVariable behandlingId: UUID): Ressurs<FormDto> {
        //Ikke slett kommentert linje under
        return Ressurs.success(formService.hentForm(behandlingId).tilDto())
        // return Ressurs.success(formService.hentMockFormDto(UUID.fromString(behandlingId)))
    }

    @GetMapping("vilkar/{behandlingId}")
    fun hentVilkår(@PathVariable behandlingId: UUID): Ressurs<Form> {
        return Ressurs.success(formService.hentForm(behandlingId))
    }

    @PostMapping
    fun opprettFormkravVilkår(@RequestBody form: Form): Ressurs<Form> {
        return Ressurs.success(formService.opprettForm(form))
    }
}