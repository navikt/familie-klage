package no.nav.familie.klage.ekstern

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.OpprettBehandlingService
import no.nav.familie.klage.behandling.domain.tilEksternKlagebehandlingDto
import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.klage.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/ekstern/behandling"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class EksternBehandlingController(
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
    private val opprettBehandlingService: OpprettBehandlingService,
    private val oppgaveService: OppgaveService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("{fagsystem}")
    fun finnKlagebehandlingsresultat(
        @PathVariable fagsystem: Fagsystem,
        @RequestParam("eksternFagsakId") eksternFagsakIder: Set<String>,
    ): Ressurs<Map<String, List<KlagebehandlingDto>>> {
        feilHvis(eksternFagsakIder.isEmpty()) {
            "Mangler eksternFagsakId i query param"
        }
        val behandlinger = eksternFagsakIder.associateWith { eksternFagsakId ->
            behandlingService.finnKlagebehandlingsresultat(eksternFagsakId, fagsystem).map {
                it.tilEksternKlagebehandlingDto(behandlingService.hentKlageresultatDto(behandlingId = it.id))
            }
        }
        val antallTreff = behandlinger.entries.associate { it.key to it.value.size }
        logger.info("Henter klagebehandlingsresultat for eksternFagsakIder=$eksternFagsakIder antallTreff=$antallTreff")
        validerTilgang(behandlinger)

        return Ressurs.success(behandlinger)
    }

    private fun validerTilgang(behandlinger: Map<String, List<KlagebehandlingDto>>) {
        behandlinger.entries.flatMap { it.value }.map { it.fagsakId }.distinct().forEach {
            tilgangService.validerTilgangTilPersonMedRelasjonerForFagsak(it, AuditLoggerEvent.ACCESS)
            tilgangService.validerHarVeilederrolleTilStønadForFagsak(it)
        }
    }

    @PostMapping("/opprett")
    fun opprettBehandling(@RequestBody opprettKlageBehandlingDto: OpprettKlagebehandlingRequest): UUID {
        return opprettBehandlingService.opprettBehandling(opprettKlageBehandlingDto)
    }

    @PatchMapping("{behandlingId}/gjelder-tilbakekreving")
    fun oppdaterOppgaveTilÅGjeldeTilbakekreving(@PathVariable behandlingId: UUID) {
        oppgaveService.oppdaterOppgaveTilÅGjeldeTilbakekreving(behandlingId)
    }
}
