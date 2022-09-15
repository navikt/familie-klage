package no.nav.familie.klage.ekstern

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.domain.tilEksternKlagebehandlingDto
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/ekstern/behandling"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class EksternBehandlingController(val tilgangService: TilgangService, val behandlingService: BehandlingService) {

    @GetMapping("{fagsystem}")
    fun hentBehandlinger(
        @PathVariable fagsystem: Fagsystem,
        @RequestParam("eksternFagsakId") eksternFagsakIder: Set<String>
    ): Ressurs<Map<String, List<KlagebehandlingDto>>> {
        feilHvis(eksternFagsakIder.isEmpty()) {
            "Mangler eksternFagsakId i query param"
        }
        /**
         * TODO : Legg til sjekk via tilgangservice
         */
        val behandlinger = eksternFagsakIder.associateWith { eksternFagsakId ->
            behandlingService.hentBehandlinger(eksternFagsakId, fagsystem).map { it.tilEksternKlagebehandlingDto() }
        }
        // TODO fiks validering av at behandlinger tilhører samme person
        return Ressurs.success(behandlinger)
    }
}
