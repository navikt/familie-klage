package no.nav.familie.klage.kabal

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.klage.infrastruktur.config.IntegrasjonerConfig
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
    @Qualifier("azure")
    private val restOperations: RestOperations,
    @Value("\${FAMILIE_INTEGRASJONER_URL}")
    private val integrasjonUri: URI,
    private val integrasjonerConfig: IntegrasjonerConfig,
) : AbstractRestClient(restOperations, "familie.kabal"){

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private val dokuarkivUri: URI =
        UriComponentsBuilder.fromUri(integrasjonUri).pathSegment("api/oversendelse/v3/sak").build().toUri()

    fun sendTilKabal(oversendtKlageAnkeV3: OversendtKlageAnkeV3){
        return postForEntity(
            integrasjonerConfig.sendTilKabalUri,
            oversendtKlageAnkeV3
        )
    }

}