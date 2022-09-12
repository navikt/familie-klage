package no.nav.familie.klage.test

import no.nav.familie.klage.behandling.BehandlingService
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ytelsestype
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

@RestController
@RequestMapping(path = ["/api/test"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class TestController(private val behandlingService: BehandlingService) {

    @PostMapping("opprett")
    fun opprettDummybehandling(@RequestBody request: DummybehandlingRequest): Ressurs<UUID> {
        return Ressurs.success(
            behandlingService.opprettBehandling(
                OpprettKlagebehandlingRequest(
                    request.ident,
                    request.ytelsestype,
                    request.eksternBehandlingId,
                    request.eksternFagsakId,
                    request.fagsystem,
                    request.klageMottatt
                )
            )
        )
    }

    @GetMapping
    fun hentSøknadDatoer(): Ressurs<String> {
        return Ressurs.success(
            "Hentet tekst fra backend"
        )
    }

    data class DummybehandlingRequest(
        val ident: String,
        val ytelsestype: Ytelsestype,
        val eksternBehandlingId: String = Random.nextInt().toString(),
        val eksternFagsakId: String = Random.nextInt().toString(),
        val fagsystem: Fagsystem = Fagsystem.EF,
        val klageMottatt: LocalDate = LocalDate.now()
    )
}
