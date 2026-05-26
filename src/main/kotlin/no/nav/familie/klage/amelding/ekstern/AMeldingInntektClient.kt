package no.nav.familie.klage.amelding.ekstern

import no.nav.familie.kontrakter.felles.PersonIdent
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class AMeldingInntektClient(
    @Value("\${FAMILIE_EF_PROXY_URL}") private val uri: URI,
    @Qualifier("efProxyRestClient") private val restClient: RestClient,
) {
    private val genererUrlUri =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("api/ainntekt/generer-url")
            .build()
            .toUri()

    fun genererAInntektUrl(personIdent: String): String =
        restClient
            .post()
            .uri(genererUrlUri)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.TEXT_PLAIN)
            .body(PersonIdent(personIdent))
            .retrieve()
            .body<String>()!!
}
