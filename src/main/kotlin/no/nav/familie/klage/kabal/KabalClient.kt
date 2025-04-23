package no.nav.familie.klage.kabal

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.klage.kabal.domain.OversendtKlageAnke
import no.nav.familie.klage.kabal.domain.OversendtKlageAnkeV3
import no.nav.familie.klage.kabal.domain.OversendtKlageAnkeV4
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private val oversendelseUrlV3: URI =
        UriComponentsBuilder
            .fromUri(kabalUrl)
            .pathSegment("api/oversendelse/v3/sak")
            .build()
            .toUri()

    private val oversendelseUrlV4: URI =
        UriComponentsBuilder
            .fromUri(kabalUrl)
            .pathSegment("api/oversendelse/v4/sak")
            .build()
            .toUri()

    fun sendTilKabal(oversendtKlage: OversendtKlageAnke) =
        when (oversendtKlage) {
            is OversendtKlageAnkeV3 -> postForEntity(oversendelseUrlV3, oversendtKlage)
            is OversendtKlageAnkeV4 -> postForEntity(oversendelseUrlV4, oversendtKlage)
        }
}
