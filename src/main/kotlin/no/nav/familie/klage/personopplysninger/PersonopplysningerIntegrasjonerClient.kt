package no.nav.familie.klage.personopplysninger

import no.nav.familie.klage.felles.dto.EgenAnsattRequest
import no.nav.familie.klage.felles.dto.EgenAnsattResponse
import no.nav.familie.klage.integrasjoner.IntegrasjonerClientProvider
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.body
import java.net.URI

@Component
class PersonopplysningerIntegrasjonerClient(
    private val integrasjonerClientProvider: IntegrasjonerClientProvider,
) {
    fun egenAnsatt(ident: String): Boolean {
        val (integrasjonUri, restClient) = integrasjonerClientProvider.hentKlient()
        return restClient
            .post()
            .uri(URI.create("$integrasjonUri/api/egenansatt"))
            .contentType(MediaType.APPLICATION_JSON)
            .body(EgenAnsattRequest(ident))
            .retrieve()
            .body<Ressurs<EgenAnsattResponse>>()!!
            .data!!
            .erEgenAnsatt
    }
}
