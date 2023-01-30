package no.nav.familie.klage.test

import no.nav.familie.klage.behandling.OpprettBehandlingService
import no.nav.familie.klage.fagsak.FagsakPersonService
import no.nav.familie.klage.fagsak.FagsakRepository
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/test"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class TestController(
    private val fagsakPersonService: FagsakPersonService,
    private val fagsakRepository: FagsakRepository,
    private val opprettBehandlingService: OpprettBehandlingService
) {

    @PostMapping("opprett")
    fun opprettDummybehandling(@RequestBody request: DummybehandlingRequest): Ressurs<UUID> {
        val fagsakPerson = fagsakPersonService.hentEllerOpprettPerson(setOf(request.ident), request.ident)
        // findByEksternIdAndFagsystemAndStønadstype ?
        val eksternFagsakId = fagsakRepository.findAll()
            .find { it.fagsakPersonId == fagsakPerson.id && it.stønadstype == request.stønadstype }
            ?.eksternId ?: UUID.randomUUID().toString()

        return Ressurs.success(
            opprettBehandlingService.opprettBehandling(
                OpprettKlagebehandlingRequest(
                    request.ident,
                    request.stønadstype,
                    eksternFagsakId,
                    request.fagsystem,
                    request.klageMottatt,
                    request.behandlendeEnhet
                )
            )
        )
    }

    data class DummybehandlingRequest(
        val ident: String,
        val stønadstype: Stønadstype,
        val fagsystem: Fagsystem = Fagsystem.EF,
        val klageMottatt: LocalDate = LocalDate.now(),
        val behandlendeEnhet: String = "4489"
    )
}
