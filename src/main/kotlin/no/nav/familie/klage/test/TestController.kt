package no.nav.familie.klage.test

import no.nav.familie.klage.behandling.OpprettBehandlingService
import no.nav.familie.klage.fagsak.FagsakPersonService
import no.nav.familie.klage.fagsak.FagsakRepository
import no.nav.familie.klage.oppgave.BehandleSakOppgave
import no.nav.familie.klage.oppgave.BehandleSakOppgaveRepository
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
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
    private val opprettBehandlingService: OpprettBehandlingService,
    private val behandleSakOppgaveRepository: BehandleSakOppgaveRepository,
) {

    @PostMapping("opprett")
    fun opprettDummybehandling(@RequestBody request: DummybehandlingRequest): Ressurs<UUID> {
        val fagsakPerson = fagsakPersonService.hentEllerOpprettPerson(setOf(request.ident), request.ident)
        // findByEksternIdAndFagsystemAndStønadstype ?
        val eksternFagsakId = fagsakRepository.findAll()
            .find { it.fagsakPersonId == fagsakPerson.id && it.stønadstype == request.stønadstype }
            ?.eksternId ?: UUID.randomUUID().toString()

        val behandlingId = opprettBehandlingService.opprettBehandling(
            OpprettKlagebehandlingRequest(
                ident = request.ident,
                stønadstype = request.stønadstype,
                eksternFagsakId = eksternFagsakId,
                fagsystem = request.fagsystem,
                klageMottatt = request.klageMottatt,
                behandlendeEnhet = request.behandlendeEnhet,
                behandlingsårsak = request.behandlingsårsak,
            ),
        )

        val behandleSakOppgave = BehandleSakOppgave(
            behandlingId,
            123,
        )

        behandleSakOppgaveRepository.insert(behandleSakOppgave)

        return Ressurs.success(
            behandlingId,
        )
    }

    data class DummybehandlingRequest(
        val ident: String,
        val stønadstype: Stønadstype,
        val fagsystem: Fagsystem = Fagsystem.EF,
        val klageMottatt: LocalDate = LocalDate.now(),
        val behandlendeEnhet: String = "4489",
        val behandlingsårsak: Klagebehandlingsårsak = Klagebehandlingsårsak.ORDINÆR,
    )
}
