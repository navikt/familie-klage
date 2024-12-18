package no.nav.familie.klage.kabal

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class KabalClient(
    @Value("\${KABAL_URL}")
    private val kabalUrl: URI,
    @Qualifier("azure")
    private val restOperations: RestOperations,
) : AbstractRestClient(restOperations, "familie.kabal") {
    private val oversendelseUrl: URI =
        UriComponentsBuilder
            .fromUri(kabalUrl)
            .pathSegment("api/oversendelse/v3/sak")
            .build()
            .toUri()

    fun sendTilKabal(oversendtKlage: OversendtKlageAnkeV3) {
        secureLogger.debug("Sender klage til kabal: ${objectMapper.writeValueAsString(oversendtKlage)}")
        return postForEntity(oversendelseUrl, oversendtKlage)
    }
}
