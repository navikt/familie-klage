package no.nav.familie.klage.kabal

import no.nav.familie.klage.kabal.domain.OversendtKlageAnke
import no.nav.familie.klage.kabal.domain.OversendtKlageAnkeV3
import no.nav.familie.klage.kabal.domain.OversendtKlageAnkeV4
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class KabalClient(
    @Value("\${KABAL_URL}")
    private val kabalUrl: URI,
    @Qualifier("kabalRestClient")
    private val restClient: RestClient,
) {
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

    fun sendTilKabal(oversendtKlage: OversendtKlageAnke) {
        when (oversendtKlage) {
            is OversendtKlageAnkeV3 -> {
                restClient
                    .post()
                    .uri(oversendelseUrlV3)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(oversendtKlage)
                    .retrieve()
                    .toBodilessEntity()
            }

            is OversendtKlageAnkeV4 -> {
                restClient
                    .post()
                    .uri(oversendelseUrlV4)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(oversendtKlage)
                    .retrieve()
                    .toBodilessEntity()
            }
        }
    }
}
