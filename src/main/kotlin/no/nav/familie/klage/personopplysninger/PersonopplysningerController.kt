package no.nav.familie.klage.personopplysninger

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.fagsak.FagsakService
import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
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
class PersonopplysningerController(
    private val personopplysningerService: PersonopplysningerService,
    private val tilgangService: TilgangService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
) {

    @GetMapping("{behandlingId}")
    fun hentPersonopplysninger(@PathVariable behandlingId: UUID): Ressurs<Personopplysninger> {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val person = fagsakService.hentFagsak(behandling.fagsakId)
        tilgangService.validerTilgangTilPerson(person.personIdent, AuditLoggerEvent.ACCESS)
        return Ressurs.success(personopplysningerService.hentPersonopplysninger(behandlingId))
    }

    @PostMapping
    fun opprettPersonopplysninger(@RequestBody personopplysninger: Personopplysninger): Ressurs<Personopplysninger> {
        tilgangService.validerTilgangTilPerson(personopplysninger.personIdent, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolle()
        return Ressurs.success(personopplysningerService.opprettPersonopplysninger(personopplysninger))
    }
}
