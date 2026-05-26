package no.nav.familie.klage.søk.ereg

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class EregClient(
    @Value("\${FAMILIE_EF_PROXY_URL}")
    private val familieEfProxyUri: URI,
    @Qualifier("efProxyRestClient")
    private val restClient: RestClient,
) {
    fun hentOrganisasjoner(organisasjonsnumre: List<String>): List<OrganisasjonDto> {
        val uri =
            UriComponentsBuilder
                .fromUri(familieEfProxyUri)
                .pathSegment("api/ereg")
                .queryParam("organisasjonsnumre", organisasjonsnumre)
                .build()
                .toUri()
        return restClient
            .get()
            .uri(uri)
            .retrieve()
            .body<List<OrganisasjonDto>>()!!
    }
}
