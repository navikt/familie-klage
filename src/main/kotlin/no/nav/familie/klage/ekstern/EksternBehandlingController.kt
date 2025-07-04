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
        val behandlinger =
            eksternFagsakIder.associateWith { eksternFagsakId ->
                behandlingService.finnKlagebehandlingsresultat(eksternFagsakId, fagsystem).map {
                    it.tilEksternKlagebehandlingDto(behandlingService.hentKlageresultatDto(behandlingId = it.id))
                }
            }
        val antallTreff = behandlinger.entries.associate { it.key to it.value.size }
        logger.info("Henter klagebehandlingsresultat for eksternFagsakIder=$eksternFagsakIder antallTreff=$antallTreff")
        validerTilgang(behandlinger)
        return Ressurs.success(behandlinger)
    }

    @GetMapping("/baks/{fagsystem}")
    fun hentBehandlingerBAKS(
        @PathVariable fagsystem: Fagsystem,
        @RequestParam("eksternFagsakId") eksternFagsakId: String,
    ): Ressurs<List<KlagebehandlingDto>> {
        feilHvis(eksternFagsakId.isBlank()) {
            "Mangler eksternFagsakId i query param"
        }

        feilHvis(fagsystem !in listOf(Fagsystem.BA, Fagsystem.KS)) {
            "Ugyldig fagsystem: $fagsystem. Endepunkt støtter kun BA og KS."
        }

        val behandlinger = hentBehandlingerBAKS(eksternFagsakId, fagsystem)

        return Ressurs.success(behandlinger)
    }

    private fun hentBehandlingerBAKS(
        eksternFagsakId: String,
        fagsystem: Fagsystem,
    ): List<KlagebehandlingDto> {
        tilgangService.validerTilgangTilEksternFagsak(eksternFagsakId = eksternFagsakId, fagsystem = fagsystem, event = AuditLoggerEvent.ACCESS)

        logger.info("Henter klagebehandlinger for eksternFagsakId=$eksternFagsakId")

        return behandlingService
            .finnKlagebehandlingsresultat(
                eksternFagsakId = eksternFagsakId,
                fagsystem = fagsystem,
            ).map { it.tilEksternKlagebehandlingDto(behandlingService.hentKlageresultatDto(it.id)) }
    }

    private fun validerTilgang(behandlinger: Map<String, List<KlagebehandlingDto>>) {
        behandlinger.entries.flatMap { it.value }.map { it.fagsakId }.distinct().forEach { fagsakId ->
            tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.ACCESS)
            tilgangService.validerHarVeilederrolleTilStønadForFagsak(fagsakId)
        }
    }

    @PostMapping("/opprett")
    fun opprettBehandling(
        @RequestBody opprettKlageBehandlingDto: OpprettKlagebehandlingRequest,
    ) {
        opprettBehandlingService.opprettBehandling(opprettKlageBehandlingDto)
    }

    @PostMapping("/v2/opprett")
    fun opprettBehandlingV2(
        @RequestBody opprettKlageBehandlingDto: OpprettKlagebehandlingRequest,
    ): Ressurs<UUID> = Ressurs.success(opprettBehandlingService.opprettBehandling(opprettKlageBehandlingDto))

    @PatchMapping("{behandlingId}/gjelder-tilbakekreving")
    fun oppdaterOppgaveTilÅGjeldeTilbakekreving(
        @PathVariable behandlingId: UUID,
    ) {
        oppgaveService.oppdaterOppgaveTilÅGjeldeTilbakekreving(behandlingId)
    }
}
