package no.nav.familie.klage.personopplysninger

import no.nav.familie.klage.personopplysninger.domain.Personopplysninger
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
@RequestMapping(path = ["/api/personopplysninger"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class PersonopplysningerController(private val personopplysningerService: PersonopplysningerService) {

    @GetMapping("{behandlingId}")
    fun hentPersonopplysninger(@PathVariable behandlingId: UUID): Ressurs<Personopplysninger> {
        return Ressurs.success(personopplysningerService.hentPersonopplysninger(behandlingId))
    }

    @PostMapping
    fun opprettPersonopplysninger(@RequestBody personopplysninger: Personopplysninger): Ressurs<Personopplysninger> {
        return Ressurs.success(personopplysningerService.opprettPersonopplysninger(personopplysninger))
    }
}