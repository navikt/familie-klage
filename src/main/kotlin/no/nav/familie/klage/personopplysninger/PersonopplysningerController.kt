package no.nav.familie.klage.personopplysninger

import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.klage.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.klage.personopplysninger.dto.PersonopplysningerFagsakEierOgSøkerDto
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/personopplysninger"])
@Validated
class PersonopplysningerController(
    private val personopplysningerService: PersonopplysningerService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("{behandlingId}")
    fun hentPersonopplysninger(
        @PathVariable behandlingId: UUID,
    ): Ressurs<PersonopplysningerDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        return Ressurs.success(personopplysningerService.hentPersonopplysningerFagsakEier(behandlingId))
    }

    @GetMapping("{behandlingId}/fagsak-eier-og-soker")
    fun hentPersonopplysningerForFagsakEierOgSøker(
        @PathVariable behandlingId: UUID,
    ): Ressurs<PersonopplysningerFagsakEierOgSøkerDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarVeilederrolleTilStønadForBehandling(behandlingId)
        val dto =
            PersonopplysningerFagsakEierOgSøkerDto(
                fagsakEier = personopplysningerService.hentPersonopplysningerFagsakEier(behandlingId),
                søker = personopplysningerService.hentPersonopplysningerSøker(behandlingId),
            )
        return Ressurs.success(dto)
    }
}
