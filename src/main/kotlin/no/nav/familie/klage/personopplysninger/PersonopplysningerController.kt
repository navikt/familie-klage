package no.nav.familie.klage.personopplysninger

import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.klage.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/personopplysninger"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class PersonopplysningerController(
    private val personopplysningerService: PersonopplysningerService,
    private val tilgangService: TilgangService
) {

    @GetMapping("{behandlingId}")
    fun hentPersonopplysninger(@PathVariable behandlingId: UUID): Ressurs<PersonopplysningerDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(personopplysningerService.hentPersonopplysninger(behandlingId))
    }
}
