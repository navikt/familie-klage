package no.nav.familie.klage.henlegg

import no.nav.familie.klage.brev.BrevService
import no.nav.familie.klage.felles.domain.AuditLoggerEvent
import no.nav.familie.klage.felles.util.isEqualOrAfter
import no.nav.familie.klage.infrastruktur.exception.feilHvis
import no.nav.familie.klage.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.klage.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.klage.personopplysninger.PersonopplysningerService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/behandling"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class HenleggBehandlingController(
    private val tilgangService: TilgangService,
    private val henleggBehandlingService: HenleggBehandlingService,
    private val brevService: BrevService,
    private val personopplysningerService: PersonopplysningerService,
) {

    @PostMapping("{behandlingId}/henlegg")
    fun henleggBehandling(@PathVariable behandlingId: UUID, @RequestBody henlegg: HenlagtDto): Ressurs<Unit> {
        tilgangService.validerTilgangTilPersonMedRelasjonerForBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolleTilStønadForBehandling(behandlingId)
        if (henlegg.skalSendeHenleggelsesbrev) {
            validerIkkeSendBrevPåFeilType(henlegg)
            validerIkkeSendBrevHvisVergemålEllerFullmakt(behandlingId)
            brevService.opprettJournalførHenleggelsesbrevTask(behandlingId)
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
    ) = Ressurs.success(brevService.genererHenleggelsesbrev(behandlingId, saksbehandlerSignatur))

    private fun validerIkkeSendBrevPåFeilType(henlagt: HenlagtDto) {
        feilHvis(henlagt.skalSendeHenleggelsesbrev && henlagt.årsak == HenlagtÅrsak.FEILREGISTRERT) { "Skal ikke sende brev hvis type er ulik trukket tilbake" }
    }

    private fun validerIkkeSendBrevHvisVergemålEllerFullmakt(
        ident: UUID,
    ) {
        val personopplysninger = personopplysningerService.hentPersonopplysninger(ident)
        val harVerge = personopplysninger.vergemål.isNotEmpty()
        val harFullmakt: Boolean =
            personopplysninger.fullmakt
                .filter {
                    it.gyldigTilOgMed == null ||
                        (
                            it.gyldigTilOgMed.isEqualOrAfter(
                                LocalDate.now(),
                            )
                            )
                }.isNotEmpty()
        feilHvis(harVerge || harFullmakt) {
            "Skal ikke sende brev hvis person er tilknyttet vergemål eller fullmakt"
        }
    }
}
