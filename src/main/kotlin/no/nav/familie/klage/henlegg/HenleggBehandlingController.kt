package no.nav.familie.klage.henlegg

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.klage.behandling.FerdigstillBehandlingService
import no.nav.familie.klage.behandling.OpprettRevurderingService
import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.klage.integrasjoner.FagsystemVedtakService
import no.nav.familie.klage.oppgave.OppgaveService
import no.nav.familie.klage.oppgave.TilordnetRessursService
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
@RequestMapping(path = ["/api/behandling"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class HenleggBehandlingController(
    private val behandlingService: BehandlingService,
    private val tilgangService: TilgangService,
    private val ferdigstillBehandlingService: FerdigstillBehandlingService,
    private val fagsystemVedtakService: FagsystemVedtakService,
    private val opprettRevurderingService: OpprettRevurderingService,
    private val tilordnetRessursService: TilordnetRessursService,
    private val oppgaveService: OppgaveService,
    private val henleggBehandlingService: HenleggBehandlingService,
) {

    @PostMapping("{behandlingId}/henlegg")
    fun henleggBehandling(@PathVariable behandlingId: UUID, @RequestBody henlegg: HenlagtDto): Ressurs<Unit> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        if (henlegg.skalSendeHenleggelsesbrev) {
            henleggBehandlingService.opprettJournalførBrevTaskHenlegg(behandlingId)
        }
        return Ressurs.success(henleggBehandlingService.henleggBehandling(behandlingId, henlegg))
    }

    @GetMapping("/{behandlingId}/henlegg/brev/forhandsvisning")
    fun genererHenleggBrev(
        @PathVariable behandlingId: UUID,
    ): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        val saksbehandlerSignatur = SikkerhetContext.hentSaksbehandlerNavn(strict = true)
        return henleggBrevRessurs(behandlingId, saksbehandlerSignatur)
    }

    private fun henleggBrevRessurs(
        behandlingId: UUID,
        saksbehandlerSignatur: String,
    ) = Ressurs.success(henleggBehandlingService.genererHenleggelsesbrev(behandlingId, saksbehandlerSignatur))
}
