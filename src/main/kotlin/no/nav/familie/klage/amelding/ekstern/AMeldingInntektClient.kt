package no.nav.familie.klage.amelding.ekstern

import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.restklient.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class AMeldingInntektClient(
    @Value("\${FAMILIE_EF_PROXY_URL}") private val uri: URI,
    @Qualifier("azure") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "inntekt") {
    private val genererUrlUri =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("api/ainntekt/generer-url")
            .build()
            .toUri()

    fun genererAInntektUrl(personIdent: String): String =
        postForEntity(
            genererUrlUri,
            PersonIdent(personIdent),
            HttpHeaders().apply {
                accept = listOf(MediaType.TEXT_PLAIN)
            },
        )
}
