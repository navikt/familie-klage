package no.nav.familie.klage.søk.ereg

import no.nav.familie.http.client.AbstractPingableRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class EregClient(
    @Value("\${FAMILIE_EF_PROXY_URL}")
    private val familieEfProxyUri: URI,
    @Qualifier("azure")
    private val restOperations: RestOperations,
) : AbstractPingableRestClient(restOperations, "familie.proxy.ereg") {

    fun hentOrganisasjoner(organisasjonsnumre: List<String>): List<OrganisasjonDto> {
        val uriBuilder = UriComponentsBuilder.fromUri(familieEfProxyUri)
            .pathSegment("api/ereg")
            .queryParam("organisasjonsnumre", organisasjonsnumre)

        return getForEntity(uriBuilder.build().toUri())
    }

    override val pingUri = familieEfProxyUri

    override fun ping() {
        operations.optionsForAllow(pingUri)
    }
}
