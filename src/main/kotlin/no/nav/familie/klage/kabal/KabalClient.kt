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
import java.time.LocalDate
import java.util.UUID


@Component
class KabalClient(
    @Qualifier("azure")
    private val restOperations: RestOperations,
    @Value("\${FAMILIE_INTEGRASJONER_URL}")
    private val integrasjonUri: URI,
    private val integrasjonerConfig: IntegrasjonerConfig,
) : AbstractRestClient(restOperations, "familie.kabal"){

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    //override val pingUri: URI = URI.create("/api/ping")

    private val dokuarkivUri: URI =
        UriComponentsBuilder.fromUri(integrasjonUri).pathSegment("api/oversendelse/v3/sak").build().toUri()

    fun sendTilKabal(){

        val oversendtKlageAnkeV3 = lagOversendtKlageAnkeV3()

        return postForEntity(
            integrasjonerConfig.sendTilKabalUri,
            oversendtKlageAnkeV3
        )
    }

    fun lagOversendtKlageAnkeV3(): OversendtKlageAnkeV3 {

        val klager = lagKlager()
        val fagsak = lagOversendtSak()

        return OversendtKlageAnkeV3(
            type = Type.KLAGE,
            klager = klager,
            fagsak = fagsak,
            kildeReferanse = "kildereferansen kommer",
            dvhReferanse = "dvhReferanse",
            forrigeBehandlendeEnhet = "forrige behandlende enhet",
            brukersHenvendelseMottattNavDato = LocalDate.now(),
            innsendtTilNav = LocalDate.now(),
            kilde = KildeFagsystem.EF,
            ytelse = Ytelse.ENF,
        )
    }

    fun lagKlager(): OversendtKlager{
        val oversendtPartIdType = OversendtPartIdType.PERSON
        val oversendtPartId = OversendtPartId(oversendtPartIdType, "en verdi")
        return OversendtKlager(oversendtPartId, null)
    }

    fun lagOversendtSak(): OversendtSak{
        return OversendtSak(
            fagsakId = UUID.randomUUID().toString(),
            fagsystem = KildeFagsystem.EF)
    }
}